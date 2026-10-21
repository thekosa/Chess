export const finished = game => !!game && !['ACTIVE', 'CHECK'].includes(game.status);

export function resultLabel(game) {
  if (!game) return 'Chwila na rozstawienie figur';
  switch (game.status) {
    case 'CHECKMATE': return `Wygrywają ${game.sideToMove === 'BLACK' ? 'białe' : 'czarne'}`;
    case 'STALEMATE': return 'Pat. Remis.';
    case 'DRAW_REPETITION': return 'Remis przez powtórzenie pozycji';
    case 'DRAW_FIFTY_MOVES': return 'Remis — reguła 50 posunięć';
    case 'DRAW_INSUFFICIENT_MATERIAL': return 'Remis — niewystarczający materiał';
    case 'DRAW_FIVEFOLD_REPETITION': return 'Remis — pięciokrotne powtórzenie';
    case 'DRAW_SEVENTY_FIVE_MOVES': return 'Remis — reguła 75 posunięć';
    default: return `Ruch ${game.sideToMove === 'WHITE' ? 'białych' : 'czarnych'}`;
  }
}
