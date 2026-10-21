import { test } from 'node:test';
import assert from 'node:assert/strict';
import { finished, resultLabel } from './gameStatus.js';

test('every draw blocks further play and describes its cause', () => {
  for (const status of ['STALEMATE', 'DRAW_REPETITION', 'DRAW_FIFTY_MOVES', 'DRAW_INSUFFICIENT_MATERIAL',
    'DRAW_FIVEFOLD_REPETITION', 'DRAW_SEVENTY_FIVE_MOVES']) {
    assert.equal(finished({ status }), true);
    assert.match(resultLabel({ status }), /remis/i);
  }
});
test('check is playable and checkmate announces the opposite side', () => {
  assert.equal(finished(null), false);
  assert.equal(finished({ status: 'ACTIVE' }), false);
  assert.equal(finished({ status: 'CHECK' }), false);
  assert.equal(finished({ status: 'CHECKMATE' }), true);
  assert.equal(resultLabel({ status: 'CHECKMATE', sideToMove: 'WHITE' }), 'Wygrywają czarne');
  assert.equal(resultLabel({ status: 'CHECKMATE', sideToMove: 'BLACK' }), 'Wygrywają białe');
});
