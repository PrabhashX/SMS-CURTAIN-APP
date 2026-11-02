import test from 'node:test';
import assert from 'node:assert/strict';
import { measure, checkMeasurements } from '../math.mjs';

test('window and door feet, fabric, bracket and endcap quantities', () => {
  const m = [
    {kind:'WINDOW', length: 120, unit:'inch', openings:3, frames:2},
    {kind:'DOOR', length: 2, unit:'m', openings:1, frames:1}
  ];
  const result = measure(m, .5);
  assert.ok(Math.abs(result.grandFeet - (20 + 2/.3048)) < 1e-10);
  assert.equal(result.bracketCount, 8); // 3*2 windows + 2*1 door
  assert.equal(result.endcapCount, 6);
  assert.equal(result.eyeletCount, Math.ceil(result.fabricMetres * 6 - 1e-9));
  checkMeasurements({...result, extraMetres:.5}, m);
});
test('tampered bill quantities are rejected', () => {
  const m = [{kind:'WINDOW',length:10,unit:'ft',openings:3,frames:1}];
  assert.throws(() => checkMeasurements({...measure(m,0), extraMetres:0, eyeletCount:1}, m), /eyeletCount/);
});
test('doors do not require doors-amount field', () => {
  const m = [{kind:'DOOR', length:4, unit:'ft', openings:1, frames:3}];
  assert.equal(measure(m,0).bracketCount, 6);
  assert.equal(measure(m,0).endcapCount, 6);
});
