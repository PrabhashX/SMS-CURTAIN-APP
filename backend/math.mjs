// Shared shop counting policy (used to verify values sent by Android clients).
const round2 = x => Math.round((x + Number.EPSILON) * 100) / 100;
const feet = (value, unit) => {
  switch (unit) {
    case 'cm': return value / 30.48;
    case 'm': return value / 0.3048;
    case 'inch': return value / 12;
    case 'ft': return value;
    default: throw Error('Unsupported measurement unit');
  }
};
export function measure(measurements, extra) {
  if (!Number.isFinite(extra) || extra < 0 || extra > 10000) throw Error('Invalid extra fabric');
  let windowFeet = 0, doorFeet = 0, bracketCount = 0, endcapCount = 0;
  for (const m of measurements) {
    if (!Number.isFinite(m.length) || m.length <= 0 || !Number.isInteger(m.frames) || m.frames < 1 || m.frames > 10000 ||
        !Number.isInteger(m.openings) || m.openings < 1 || !['DOOR','WINDOW'].includes(m.kind)) throw Error('Invalid measurement');
    const count = feet(m.length, m.unit) * m.frames;
    if (m.kind === 'WINDOW') {
      windowFeet += count;
      bracketCount += m.frames * (m.openings >= 3 ? 3 : 2);
    } else { doorFeet += count; bracketCount += m.frames * 2; }
    endcapCount += m.frames * 2;
  }
  const grandFeet = windowFeet + doorFeet;
  const fabricMetres = grandFeet + extra;
  return { windowFeet, doorFeet, grandFeet, fabricMetres,
    eyeletCount: Math.ceil(fabricMetres * 6 - 1e-9), bracketCount, endcapCount };
}
export function checkMeasurements(invoice, measurements) {
  const expected = measure(measurements, invoice.extraMetres);
  for (const [field, value] of Object.entries(expected)) {
    const tolerance = ['bracketCount','endcapCount','eyeletCount'].includes(field) ? 0 : 0.0001;
    if (!Number.isFinite(invoice[field]) || Math.abs(invoice[field] - value) > tolerance)
      throw Error(`Bill mismatch: ${field}`);
  }
}
export { round2 };
