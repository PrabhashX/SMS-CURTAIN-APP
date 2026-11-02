import 'dotenv/config';
import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import rateLimit from 'express-rate-limit';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { MongoClient, ServerApiVersion } from 'mongodb';
import { checkMeasurements } from './math.mjs';

const required = ['MONGODB_URI', 'JWT_SECRET', 'ADMIN_PASSWORD', 'EMPLOYEE_PASSWORD'];
for (const name of required) if (!process.env[name]) throw Error(`Missing ${name} in backend/.env`);
if (process.env.JWT_SECRET.length < 32) throw Error('JWT_SECRET must be >= 32 chars');
if (process.env.NODE_ENV === 'production' &&
    (process.env.ADMIN_PASSWORD === 'admin' || process.env.EMPLOYEE_PASSWORD === 'employee')) {
  throw Error('Demo passwords forbidden in production. Change ADMIN_PASSWORD and EMPLOYEE_PASSWORD.');
}
const client = new MongoClient(process.env.MONGODB_URI, {
  serverApi: { version: ServerApiVersion.v1, strict: false, deprecationErrors: true }
});
await client.connect();
const db = client.db(process.env.MONGODB_DB || 'sms_curtains');
const users = db.collection('users');
const bills = db.collection('bills');
const requests = db.collection('requests');
const events = db.collection('processedEvents');
await Promise.all([
  bills.createIndex({ uid: 1 }, { unique: true }),
  requests.createIndex({ uid: 1 }, { unique: true }),
  events.createIndex({ eventId: 1 }, { unique: true })
]);
for (const [username, role, password] of [
  ['admin', 'admin', process.env.ADMIN_PASSWORD],
  ['employee', 'employee', process.env.EMPLOYEE_PASSWORD]
]) {
  const existing = await users.findOne({ username });
  if (!existing) await users.insertOne({ username, role, passwordHash: await bcrypt.hash(password, 12) });
  // To rotate an existing account password, use an administrator-approved reset;
  // changing env variables alone doesn't silently replace stored passwords.
}
const app = express();
app.disable('x-powered-by');
app.use(helmet());
app.use(cors({ origin: false }));
app.use(express.json({ limit: '250kb' }));
app.use('/auth/login', rateLimit({ windowMs: 15 * 60_000, limit: 15 }));
const bad = (message, status = 400) => Object.assign(new Error(message), { status });
const number = (v) => typeof v === 'number' && Number.isFinite(v) && v >= 0;
const key = (s) => typeof s === 'string' && s.length >= 3 && s.length <= 100 && /^[a-zA-Z0-9_-]+$/.test(s);
const string = (s, max = 300) => typeof s === 'string' && s.length <= max;
function must(ok, message) { if (!ok) throw bad(message); }
function round2(x) { return Math.round((x + Number.EPSILON) * 100) / 100; }
function derived(invoice) {
  const b = structuredClone(invoice);
  for (const field of ['materialRate', 'stripRate', 'eyeletRate', 'woodRate', 'bracketRate', 'endcapRate', 'sewingRate'])
    must(number(b[field]) && b[field] <= 10_000_000, `Invalid ${field}`);
  for (const field of ['fabricMetres', 'grandFeet']) must(number(b[field]) && b[field] <= 100000, `Invalid ${field}`);
  for (const field of ['eyeletCount', 'bracketCount', 'endcapCount']) must(Number.isSafeInteger(b[field]) && b[field] >= 0 && b[field] < 1000000, `Invalid ${field}`);
  b.materialTotal = round2(b.fabricMetres * b.materialRate);
  b.stripTotal = round2(b.fabricMetres * b.stripRate);
  b.eyeletTotal = round2(b.eyeletCount * b.eyeletRate);
  b.woodTotal = round2(b.grandFeet * b.woodRate);
  b.bracketTotal = round2(b.bracketCount * b.bracketRate);
  b.endcapTotal = round2(b.endcapCount * b.endcapRate);
  b.sewingTotal = round2(b.fabricMetres * b.sewingRate);
  b.grandTotal = round2(b.materialTotal + b.stripTotal + b.eyeletTotal + b.woodTotal + b.bracketTotal + b.endcapTotal + b.sewingTotal);
  return b;
}
function validateBill(b) {
  must(b && typeof b === 'object', 'Invalid bill');
  for (const field of ['customer', 'phone', 'address', 'dueDate', 'materialName', 'paymentPreference', 'workStatus'])
    must(string(b[field], 500), `Invalid ${field}`);
  must(b.customer.trim().length > 0, 'Customer name is required');
  must(['TO_START', 'ONGOING', 'DONE'].includes(b.workStatus), 'Invalid work status');
  must(['CASH', 'CARD', 'CREDIT'].includes(b.paymentPreference), 'Invalid payment preference');
  return derived(b);
}
function auth(req, res, next) {
  try {
    const header = req.get('authorization') || '';
    const token = /^Bearer (.+)$/.exec(header)?.[1];
    if (!token) throw bad('Unauthorized', 401);
    req.user = jwt.verify(token, process.env.JWT_SECRET, { algorithms: ['HS256'] });
    next();
  } catch { res.status(401).json({ error: 'Session expired. Sign in again.' }); }
}
app.get('/health', async (_req, res) => {
  await db.command({ ping: 1 }); res.json({ status: 'ok' });
});
app.post('/auth/login', async (req, res, next) => {
  try {
    const { username, password } = req.body || {};
    const user = typeof username === 'string' ? await users.findOne({ username }) : null;
    if (!user || typeof password !== 'string' || !await bcrypt.compare(password, user.passwordHash))
      throw bad('Incorrect username or password', 401);
    const token = jwt.sign({ sub: user.username, role: user.role }, process.env.JWT_SECRET, { expiresIn: '7d' });
    res.json({ token, username: user.username, role: user.role });
  } catch (e) { next(e); }
});
const approvedRoles = { CREATE_BILL: ['admin', 'employee'], WORK_STATUS: ['admin', 'employee'], PAYMENT: ['admin'], MODIFY: ['admin'], REQUEST_PAYMENT: ['employee'], REQUEST_MODIFY: ['employee'], DECIDE_REQUEST: ['admin'] };
async function applyEvent(event, user) {
  must(event && key(event.eventId) && approvedRoles[event.type]?.includes(user.role), 'Forbidden event');
  must(event.actor === user.sub, 'Event creator differs from signed-in user');
  const { type, payload, billUid } = event;
  must(key(billUid), 'Invalid bill UID');
  const existing = await events.findOne({ eventId: event.eventId });
  if (existing) return;
  const current = await bills.findOne({ uid: billUid });
  const now = Date.now();
  if (type === 'CREATE_BILL') {
    must(!current || current.createdBy === user.sub, 'Bill UID already belongs to another user');
    must(Array.isArray(payload?.measurements) && payload.measurements.length <= 200, 'Invalid measurements');
    must(payload.measurements.every(m => m && ['WINDOW','DOOR'].includes(m.kind) &&
      number(m.length) && m.length > 0 && Number.isSafeInteger(m.frames) && m.frames > 0 && m.frames <= 10000 &&
      Number.isSafeInteger(m.openings) && m.openings > 0), 'Invalid measurement entries');
    const bill = validateBill(payload.invoice);
    checkMeasurements(bill, payload.measurements);
    if (!current) await bills.insertOne({ uid: billUid, invoice: bill, measurements: payload.measurements,
      payments: [], workStatus: 'TO_START', createdBy: user.sub, updatedAt: now, revision: 0 });
  } else {
    must(!!current, 'Bill not found');
    if (type === 'WORK_STATUS') {
      must(['TO_START', 'ONGOING', 'DONE'].includes(payload?.status), 'Invalid work status');
      // Don't let employees rewind a completed order.
      if (user.role === 'employee') must(({ TO_START:0, ONGOING:1, DONE:2 })[payload.status] >=
        ({ TO_START:0, ONGOING:1, DONE:2 })[current.workStatus], 'Cannot rewind work progress');
      await bills.updateOne({ uid: billUid }, { $set: { workStatus: payload.status, 'invoice.workStatus': payload.status, updatedAt: now }, $inc: { revision: 1 } });
    } else if (type === 'PAYMENT') {
      await recordPayment(current, payload, user.sub);
    } else if (type === 'MODIFY') {
      await changeBill(current, payload?.changes);
    } else if (type === 'REQUEST_PAYMENT' || type === 'REQUEST_MODIFY') {
      must(key(payload?.uid), 'Invalid request UID');
      if (type === 'REQUEST_PAYMENT') validPayment(current, payload.payment);
      else validateChanges(current, payload?.changes);
      await requests.updateOne({ uid: payload.uid }, { $setOnInsert: {
        uid: payload.uid, billUid, type: type === 'REQUEST_PAYMENT' ? 'PAYMENT' : 'MODIFY',
        payment: type === 'REQUEST_PAYMENT' ? payload.payment : null,
        changes: type === 'REQUEST_MODIFY' ? payload.changes : null,
        note: string(payload?.note, 500) ? payload.note : '',
        status: 'PENDING', createdBy: user.sub, createdAt: now, decidedBy: '', decidedAt: 0
      } }, { upsert: true });
    } else if (type === 'DECIDE_REQUEST') {
      must(key(payload?.uid) && ['APPROVED', 'REJECTED'].includes(payload?.decision), 'Invalid decision');
      const review = await requests.findOne({ uid: payload.uid, billUid });
      must(!!review, 'Request not found');
      if (review.status === 'PENDING') {
        if (payload.decision === 'APPROVED') {
          if (review.type === 'PAYMENT') await recordPayment(current, { ...review.payment, uid: review.uid }, user.sub);
          if (review.type === 'MODIFY') await changeBill(current, review.changes);
        }
        await requests.updateOne({ uid: review.uid, status: 'PENDING' },
          { $set: { status: payload.decision, decidedBy: user.sub, decidedAt: now } });
      }
    }
  }
  await events.updateOne({ eventId: event.eventId }, { $setOnInsert: {
    eventId: event.eventId, type, actor: user.sub, at: now
  } }, { upsert: true });
}
function validPayment(current, p) {
  must(p && key(p.uid) && number(p.amount) && p.amount > 0 && p.amount <= 1e8 &&
    ['CASH','CARD','CREDIT'].includes(p.method) && ['ADVANCE','PAYMENT'].includes(p.kind) &&
    Number.isSafeInteger(p.paidAt), 'Invalid payment');
  const paid = (current.payments || []).reduce((a, v) => a + v.amount, 0);
  must(paid + p.amount <= current.invoice.grandTotal + 0.01, 'Payment exceeds balance');
}
async function recordPayment(current, p, actor) {
  if ((current.payments || []).some(x => x.uid === p?.uid)) return;
  const latest = await bills.findOne({ uid: current.uid });
  if ((latest.payments || []).some(x => x.uid === p?.uid)) return;
  validPayment(latest, p);
  const outcome = await bills.updateOne({ uid: latest.uid, revision: latest.revision, 'payments.uid': { $ne: p.uid } },
    { $push: { payments: { uid:p.uid, amount:p.amount, method:p.method, kind:p.kind, paidAt:p.paidAt, actor } },
      $set: { updatedAt: Date.now() }, $inc: { revision: 1 } });
  if (outcome.modifiedCount !== 1) throw bad('Bill changed concurrently; retry sync');
}
const editableText = ['customer', 'phone', 'address', 'dueDate'];
const editableRates = ['materialRate','stripRate','eyeletRate','woodRate','bracketRate','endcapRate','sewingRate'];
function validateChanges(current, changes) {
  must(changes && typeof changes === 'object' && !Array.isArray(changes), 'Invalid changes');
  const entries = Object.entries(changes);
  must(entries.length > 0 && entries.length <= 11, 'No valid changes');
  for (const [name, value] of entries) {
    must(editableText.includes(name) || editableRates.includes(name), `Cannot edit ${name}`);
    if (editableText.includes(name)) must(string(value, 500), `Invalid ${name}`);
    else must(number(value) && value <= 10000000, `Invalid ${name}`);
  }
  const next = validateBill({ ...current.invoice, ...changes });
  const paid = (current.payments || []).reduce((a, p) => a + p.amount, 0);
  must(next.grandTotal + .005 >= paid, 'New total cannot be less than paid amount');
  return next;
}
async function changeBill(current, changes) {
  const latest = await bills.findOne({ uid: current.uid });
  const invoice = validateChanges(latest, changes);
  const result = await bills.updateOne({ uid: latest.uid, revision: latest.revision },
    { $set: { invoice, updatedAt: Date.now() }, $inc: { revision: 1 } });
  if (result.modifiedCount !== 1) throw bad('Bill changed concurrently; retry sync');
}
app.post('/api/sync', auth, async (req, res, next) => {
  try {
    const toPush = req.body?.events || [];
    must(Array.isArray(toPush) && toPush.length <= 30, 'Too many events');
    const accepted = [];
    let rejected = null;
    // Ordered, stop-on-first-failure: older event must not be skipped.
    for (const e of toPush) {
      try { await applyEvent(e, req.user); accepted.push(e.eventId); }
      catch (err) { rejected = { eventId: e?.eventId || '', error: err.message }; break; }
    }
    const [allBills, allRequests] = await Promise.all([
      bills.find({}, { projection: { _id: 0 } }).sort({ updatedAt: -1 }).limit(3000).toArray(),
      requests.find(req.user.role === 'admin' ? {} : { createdBy: req.user.sub }, { projection: { _id: 0 } }).sort({ createdAt: -1 }).limit(3000).toArray()
    ]);
    res.json({ accepted, rejected, bills: allBills, requests: allRequests });
  } catch (e) { next(e); }
});
app.use((err, _req, res, _next) => {
  const code = err.status || 500;
  if (code === 500) console.error(err);
  res.status(code).json({ error: code === 500 ? 'Server error' : err.message });
});
const port = Number(process.env.PORT || 8080);
app.listen(port, '0.0.0.0', () => console.log(`SMS Curtains API listening on ${port}`));
