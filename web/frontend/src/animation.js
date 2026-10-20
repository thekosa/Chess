export function initialPieces(game) {
  return game.board.map(piece => ({ ...piece, id: `${game.id}:${game.revision}:${piece.square}` }));
}

function same(a, b) {
  return a && b && a.side === b.side && a.type === b.type;
}

// Only presentation: the server has already validated and committed the move.
// Comparing snapshots also discovers the rook in castling and en-passant victims.
export function movingPieces(tokens, before, after, move) {
  const oldBoard = new Map(before.board.map(piece => [piece.square, piece]));
  const newBoard = new Map(after.board.map(piece => [piece.square, piece]));
  const journeys = new Map([[move.from, move.to]]);
  before.board.forEach(piece => {
    if (piece.square === move.from || piece.side !== before.sideToMove || same(piece, newBoard.get(piece.square))) return;
    const arrivals = after.board.filter(next => next.square !== move.to && same(piece, next) && !same(next, oldBoard.get(next.square)));
    if (arrivals.length === 1) journeys.set(piece.square, arrivals[0].square);
  });
  return tokens.filter(piece => !piece.exiting).flatMap(piece => {
    if (journeys.has(piece.square)) {
      const destination = journeys.get(piece.square);
      return [{ ...piece, square: destination, finalType: newBoard.get(destination).type, moving: true }];
    }
    if (same(piece, newBoard.get(piece.square))) return [{ ...piece, moving: false }];
    if (piece.side !== before.sideToMove) return [{ ...piece, moving: false, exiting: true }];
    return [];
  });
}

export function finishPieces(tokens) {
  return tokens.filter(piece => !piece.exiting).map(piece => ({ ...piece, type: piece.finalType || piece.type, moving: false }));
}
