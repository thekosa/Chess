import React, { useEffect, useId, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { api } from './api';
import { initialPieces, movingPieces, finishPieces } from './animation';
import { finished, resultLabel } from './gameStatus';
import { EndCelebration } from './EndCelebration';
import './style.css';

const STORAGE = 'szachy.web.game.v1';
const NAMES = { PAWN: 'pion', KNIGHT: 'skoczek', BISHOP: 'goniec', ROOK: 'wieża', QUEEN: 'hetman', KING: 'król' };
const SIDE = { WHITE: 'białe', BLACK: 'czarne' };
const LEVEL = { EASY: 'Łatwy', MEDIUM: 'Średni', HARD: 'Trudny' };
const DEFAULT_OPTIONS = { opponent: 'TWO_PLAYERS', difficulty: 'MEDIUM', humanSide: 'WHITE' };
const file = square => square.charCodeAt(0) - 97;
const rank = square => Number(square[1]) - 1;
const image = piece => `/pieces/${piece.side.toLowerCase()}_${piece.type.toLowerCase()}.png`;
const computerTurn = game => game?.opponent === 'COMPUTER' && game.sideToMove !== game.humanSide && !finished(game);

function Icon({ name, ...props }) {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
    {name === 'flip' ? <><path d="M4 8h14l-4-4M20 16H6l4 4" /><path d="M20 8v3M4 16v-3" /></> :
      name === 'new' ? <><path d="M20 7v5h-5" /><path d="M19 12a7 7 0 1 0-2 5M20 12l-3-4" /></> :
      name === 'arrow' ? <path d="M5 12h14m-5-5 5 5-5 5" /> :
        name === 'king' ? <><path d="M12 3v6M9 5h6M6 10h12l-2 8H8zM7 21h10" /></> : <path d="m6 6 12 12M18 6 6 18" />}
  </svg>;
}

function Modal({ title, onClose, children }) {
  const ref = useRef(null);
  const id = useId();
  useEffect(() => {
    ref.current.showModal();
    const element = ref.current;
    return () => element.close();
  }, []);
  return <dialog className="modal" ref={ref} aria-labelledby={id} onCancel={event => { event.preventDefault(); onClose(); }}>
    <button className="modal-close icon-button" aria-label="Zamknij" onClick={onClose}><Icon name="close" /></button>
    <h2 id={id}>{title}</h2>{children}
  </dialog>;
}

function Board({ game, tokens, selected, moves, onSquare, flipped, blocked, showResult }) {
  const buttons = useRef([]);
  const destinations = new Set(moves.map(move => move.to));
  const occupants = new Map((game?.board || []).map(piece => [piece.square, piece]));
  const last = game?.history.at(-1);
  const squares = Array.from({ length: 64 }, (_, index) => {
    const row = Math.floor(index / 8), column = index % 8;
    const x = flipped ? 7 - column : column, y = flipped ? row : 7 - row;
    const square = `${String.fromCharCode(97 + x)}${y + 1}`;
    const piece = occupants.get(square);
    const target = destinations.has(square);
    const checked = game?.status === 'CHECK' && piece?.type === 'KING' && piece.side === game.sideToMove;
    const description = `${square}${piece ? `, ${piece.side === 'WHITE' ? 'biały' : 'czarny'} ${NAMES[piece.type]}` : ', puste pole'}${target ? ', możliwy ruch' : ''}`;
    return <button key={square} ref={element => { buttons.current[index] = element; }}
      className={`square ${(x + y) % 2 === 0 ? 'dark' : 'light'} ${selected === square ? 'selected' : ''} ${target ? (piece ? 'capture-target' : 'target') : ''} ${checked ? 'checked' : ''} ${last && [last.from, last.to].includes(square) ? 'last-move' : ''}`}
      aria-label={description} aria-pressed={selected === square} aria-disabled={blocked}
      tabIndex={selected ? (selected === square ? 0 : -1) : (square === (flipped ? 'e7' : 'e2') ? 0 : -1)}
      onClick={() => { if (!blocked) onSquare(square); }}
      onKeyDown={event => {
        const direction = { ArrowUp: [-1, 0], ArrowDown: [1, 0], ArrowLeft: [0, -1], ArrowRight: [0, 1] }[event.key];
        if (!direction) return;
        event.preventDefault();
        const nextRow = Math.max(0, Math.min(7, row + direction[0]));
        const nextCol = Math.max(0, Math.min(7, column + direction[1]));
        buttons.current[nextRow * 8 + nextCol]?.focus();
      }}>
      {column === 0 && <span className="coordinate rank">{y + 1}</span>}
      {row === 7 && <span className="coordinate file">{String.fromCharCode(97 + x)}</span>}
    </button>;
  });
  return <div className={`board ${!game ? 'board-loading' : ''}`} role="group" aria-label="Szachownica">
    <div className="square-grid">{squares}</div>
    <div className="pieces" aria-hidden="true">
      {tokens.map(piece => {
        const x = flipped ? 7 - file(piece.square) : file(piece.square);
        const y = flipped ? rank(piece.square) : 7 - rank(piece.square);
        return <div key={piece.id} className={`piece ${piece.moving ? 'moving' : ''} ${piece.exiting ? 'exiting' : ''}`}
          style={{ transform: `translate(${x * 100}%, ${y * 100}%)` }}>
          <img src={image(piece)} alt="" draggable="false" />
        </div>;
      })}
    </div>
    {!game && <div className="loading-overlay"><span className="spinner" /> Przygotowujemy planszę…</div>}
    {showResult && <EndCelebration key={`${game.id}-${game.revision}`} game={game} flipped={flipped} />}
  </div>;
}

function Player({ side, game, position }) {
  const active = game?.sideToMove === side && !finished(game);
  const name = game?.opponent === 'COMPUTER' ? (side === game.humanSide ? 'Ty' : `Stockfish · ${LEVEL[game.difficulty]}`) : position;
  return <div className={`player ${active ? 'active' : ''}`}>
    <div className={`player-avatar ${side.toLowerCase()}`}><img src={image({ side, type: 'KING' })} alt="" /></div>
    <div><strong>{side === 'WHITE' ? 'Białe' : 'Czarne'}</strong><span>{name}</span></div>
    {active && <span className="player-turn"><i /> Na ruchu</span>}
  </div>;
}

function GameSetup({ initial, onClose, onPlay }) {
  const [options, setOptions] = useState({ ...DEFAULT_OPTIONS, ...initial });
  const change = (key, value) => setOptions(current => ({ ...current, [key]: value }));
  return <Modal title="Jak chcesz zagrać?" onClose={onClose}>
    <p>Rozpocznij nową partię. Obecna pozycja i historia zostaną wyczyszczone.</p>
    <form onSubmit={event => { event.preventDefault(); onPlay(options); }}>
      <div className="game-settings">
        <label>Przeciwnik<select value={options.opponent} onChange={event => change('opponent', event.target.value)}>
          <option value="TWO_PLAYERS">Dwie osoby na jednym komputerze</option><option value="COMPUTER">Komputer — Stockfish</option>
        </select></label>
        {options.opponent === 'COMPUTER' && <>
          <label>Poziom trudności<select value={options.difficulty} onChange={event => change('difficulty', event.target.value)}>
            {Object.entries(LEVEL).map(([key, name]) => <option key={key} value={key}>{name}</option>)}
          </select></label>
          <label>Grasz<select value={options.humanSide} onChange={event => change('humanSide', event.target.value)}>
            <option value="WHITE">Białymi — zaczynasz</option><option value="BLACK">Czarnymi — zaczyna komputer</option>
          </select></label>
          <p className="engine-note">Przy pierwszym użyciu aplikacja pobierze silnik (ok. 80 MB). Potem możesz grać bez internetu.</p>
        </>}
      </div>
      <div className="modal-actions"><button type="button" className="secondary-button" onClick={onClose}>Anuluj</button><button type="submit" className="primary-button">Rozpocznij grę</button></div>
    </form>
  </Modal>;
}

function App() {
  const [game, setGame] = useState(null);
  const [tokens, setTokens] = useState([]);
  const [selected, setSelected] = useState(null);
  const [moves, setMoves] = useState([]);
  const [phase, setPhase] = useState('loading');
  const [notice, setNotice] = useState('Wybierz figurę, aby zobaczyć jej możliwe ruchy.');
  const [fault, setFault] = useState(null);
  const [promotion, setPromotion] = useState(null);
  const [confirmReset, setConfirmReset] = useState(false);
  const [flipped, setFlipped] = useState(false);
  const gameRef = useRef(null);
  const busy = useRef(true);
  const operation = useRef(0);
  const timer = useRef(null);
  const history = useRef(null);

  function install(snapshot) {
    clearTimeout(timer.current);
    gameRef.current = snapshot;
    setGame(snapshot);
    setTokens(initialPieces(snapshot));
    setSelected(null);
    setMoves([]);
    setPromotion(null);
    try { sessionStorage.setItem(STORAGE, snapshot.id); } catch { /* Storage can be disabled by the browser. */ }
  }

  async function load() {
    const sequence = ++operation.current;
    clearTimeout(timer.current);
    busy.current = true;
    setPhase('loading');
    setFault(null);
    try {
      let id = gameRef.current?.id;
      try { id ||= sessionStorage.getItem(STORAGE); } catch { /* A session still works without storage. */ }
      const params = new URLSearchParams(window.location.search);
      const launch = params.get('launch');
      let previousLaunch;
      try { previousLaunch = sessionStorage.getItem(`${STORAGE}.launch`); } catch { /* Optional storage. */ }
      if (!gameRef.current && launch && previousLaunch !== launch) id = null;
      let snapshot;
      if (id) {
        try { snapshot = await api(`/games/${id}`); }
        catch (error) { if (error.status !== 404) throw error; }
      }
      const fresh = !snapshot;
      if (!snapshot) {
        const options = { ...DEFAULT_OPTIONS };
        if (launch) {
          for (const key of Object.keys(options)) if (params.has(key)) options[key] = params.get(key);
        }
        snapshot = await api('/games', options);
      }
      if (sequence !== operation.current) return;
      install(snapshot);
      if (launch) { try { sessionStorage.setItem(`${STORAGE}.launch`, launch); } catch { /* Optional storage. */ } }
      if (fresh && !launch) setConfirmReset(true);
      if (snapshot.opponent === 'COMPUTER') setFlipped(snapshot.humanSide === 'BLACK');
      else if (fresh) setFlipped(false);
      setNotice('Wybierz figurę, aby zobaczyć jej możliwe ruchy.');
    } catch (error) {
      if (sequence === operation.current) setFault(error.message);
    } finally {
      if (sequence === operation.current) { busy.current = false; setPhase('idle'); }
    }
  }

  useEffect(() => {
    load();
    return () => { operation.current++; clearTimeout(timer.current); };
  }, []);

  useEffect(() => {
    if (history.current) history.current.scrollTop = history.current.scrollHeight;
  }, [game?.history.length]);

  useEffect(() => {
    if (game?.opponent !== 'COMPUTER') return;
    let cancelled = false, polling = false;
    const interval = setInterval(async () => {
      if (cancelled || polling || busy.current || fault) return;
      const before = gameRef.current;
      const sequence = operation.current;
      polling = true;
      try {
        const next = await api(`/games/${before.id}`);
        if (cancelled || busy.current || sequence !== operation.current
            || before !== gameRef.current) return;
        if (next.revision !== before.revision) {
          if (next.revision === before.revision + 1 && next.history.length === before.history.length + 1) {
            const move = next.history.at(-1);
            setNotice(`Stockfish: ${move.from} → ${move.to}.`);
            presentMove(before, next, move, ++operation.current);
          } else install(next);
        } else if (next.computerThinking !== before.computerThinking || next.computerError !== before.computerError) {
          gameRef.current = next;
          setGame(next);
        }
      } catch (error) {
        if (!cancelled && !busy.current && sequence === operation.current) setFault(error.message);
      } finally { polling = false; }
    }, 400);
    return () => { cancelled = true; clearInterval(interval); };
  }, [game?.id, game?.opponent, fault]);

  function failure(error) {
    if (error.data?.state) {
      install(error.data.state);
      setNotice(error.message);
    } else setFault(error.message);
  }

  async function choose(square) {
    if (busy.current || fault || !gameRef.current || finished(gameRef.current) || computerTurn(gameRef.current)) return;
    if (square === selected) { setSelected(null); setMoves([]); return; }
    const candidates = moves.filter(move => move.to === square);
    if (selected && candidates.length) {
      if (candidates[0].promotion) setPromotion(candidates);
      else execute(candidates[0]);
      return;
    }
    const current = gameRef.current;
    const piece = current.board.find(piece => piece.square === square);
    if (!piece || piece.side !== current.sideToMove) {
      setNotice(`Teraz grają ${SIDE[current.sideToMove]}. Wybierz ich figurę.`);
      return;
    }
    const sequence = ++operation.current;
    busy.current = true;
    setPhase('requesting');
    setSelected(square);
    setMoves([]);
    try {
      const result = await api(`/games/${current.id}/moves?from=${square}`);
      if (sequence !== operation.current) return;
      if (result.revision !== current.revision) {
        const currentPosition = await api(`/games/${current.id}`);
        if (sequence !== operation.current) return;
        install(currentPosition);
        setNotice('Pozycja została odświeżona. Wybierz figurę ponownie.');
      } else {
        setMoves(result.moves);
        setNotice(result.moves.length ? 'Wybierz podświetlone pole docelowe.' : 'Ta figura nie ma teraz legalnego ruchu.');
      }
    } catch (error) { if (sequence === operation.current) failure(error); }
    finally { if (sequence === operation.current) { busy.current = false; setPhase('idle'); } }
  }

  async function execute(move) {
    if (busy.current) return;
    const sequence = ++operation.current;
    const before = gameRef.current;
    busy.current = true;
    setPhase('requesting');
    setPromotion(null);
    setSelected(null);
    setMoves([]);
    try {
      const next = await api(`/games/${before.id}/moves`, { ...move, revision: before.revision });
      if (sequence !== operation.current) return;
      setNotice(`Ruch ${move.from} → ${move.to}${move.promotion ? `, promocja na: ${NAMES[move.promotion]}` : ''}.`);
      presentMove(before, next, move, sequence);
    } catch (error) {
      if (sequence !== operation.current) return;
      failure(error);
      busy.current = false;
      setPhase('idle');
    }
  }

  function presentMove(before, next, move, sequence) {
    busy.current = true;
    gameRef.current = next;
    setGame(next);
    setSelected(null);
    setMoves([]);
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      install(next);
      busy.current = false;
      setPhase('idle');
    } else {
      setTokens(current => movingPieces(current, before, next, move));
      setPhase('animating');
      timer.current = setTimeout(() => {
        if (sequence !== operation.current) return;
        setTokens(current => finishPieces(current));
        busy.current = false;
        setPhase('idle');
      }, 340);
    }
  }

  async function reset(options) {
    setConfirmReset(false);
    const sequence = ++operation.current;
    clearTimeout(timer.current);
    busy.current = true;
    setPhase('requesting');
    setPromotion(null);
    try {
      const current = gameRef.current;
      let next;
      try { next = await api(`/games/${current.id}/reset`, { ...options, revision: current.revision }); }
      catch (error) {
        // A computer move can finish while the settings dialog is open.
        if (error.status !== 409 || !error.data?.state) throw error;
        next = await api(`/games/${current.id}/reset`, { ...options, revision: error.data.state.revision });
      }
      if (sequence !== operation.current) return;
      install(next);
      setFlipped(next.opponent === 'COMPUTER' && next.humanSide === 'BLACK');
      setFault(null);
      setNotice('Nowa partia. Pierwszy ruch należy do białych.');
    } catch (error) { if (sequence === operation.current) failure(error); }
    finally { if (sequence === operation.current) { busy.current = false; setPhase('idle'); } }
  }

  async function retryComputer() {
    if (busy.current) return;
    const sequence = ++operation.current;
    busy.current = true;
    setPhase('requesting');
    try {
      const current = gameRef.current;
      const next = await api(`/games/${current.id}/retry`, { revision: current.revision });
      if (sequence === operation.current) install(next);
    } catch (error) { if (sequence === operation.current) failure(error); }
    finally { if (sequence === operation.current) { busy.current = false; setPhase('idle'); } }
  }

  async function claimDraw() {
    if (busy.current || computerTurn(gameRef.current)) return;
    const sequence = ++operation.current;
    busy.current = true;
    setPhase('requesting');
    try {
      const current = gameRef.current;
      const next = await api(`/games/${current.id}/claim-draw`, { revision: current.revision });
      if (sequence === operation.current) { install(next); setNotice('Remis został zgłoszony.'); }
    } catch (error) { if (sequence === operation.current) failure(error); }
    finally { if (sequence === operation.current) { busy.current = false; setPhase('idle'); } }
  }

  const status = resultLabel(game);
  const captures = side => (game?.capturedPieces || []).filter(piece => piece.side === side);
  const rows = Array.from({ length: Math.ceil((game?.history.length || 0) / 2) }, (_, index) => game.history.slice(index * 2, index * 2 + 2));
  const notation = move => move ? <>{move.from}<span className="move-arrow">→</span>{move.to}{move.promotion && <small>={NAMES[move.promotion]}</small>}</> : <span className="muted">—</span>;

  return <div className="page">
    <header className="page-header">
      <a className="brand" href="/" aria-label="Szachy, strona główna"><span className="brand-mark"><Icon name="king" /></span><span>szachy<span className="brand-period">.</span></span></a>
      <span className="local-label"><i /> {game?.opponent === 'COMPUTER' ? `Stockfish · ${LEVEL[game.difficulty]}` : 'Dwoje graczy · jedna plansza'}</span>
    </header>
    <section className="intro"><div><p className="eyebrow">MAŁA PLANSZA, WIELKIE PLANY</p><h1>Twoja kolej.</h1></div><p>Zagraj we dwoje lub z komputerem.<br />Dobry ruch zaczyna się tutaj.</p></section>

    {fault && <div className="error-banner" role="alert"><span>{fault}</span><button onClick={load} disabled={phase === 'loading'}>Połącz ponownie <Icon name="arrow" /></button></div>}

    <main className="game-layout">
      <section className="board-section" aria-label="Plansza i gracze">
        <Player side={flipped ? 'WHITE' : 'BLACK'} game={game} position="Po drugiej stronie planszy" />
        <div className="board-frame"><Board game={game} tokens={tokens} selected={selected} moves={moves} onSquare={choose} flipped={flipped} blocked={phase !== 'idle' || !!fault || !!promotion || finished(game) || computerTurn(game)} showResult={finished(game) && phase === 'idle'} /></div>
        <Player side={flipped ? 'BLACK' : 'WHITE'} game={game} position="Po tej stronie planszy" />
        <div className="board-footer"><span>Kliknij figurę, potem pole docelowe.</span><button className="text-button" onClick={() => setFlipped(!flipped)} disabled={phase !== 'idle'}><Icon name="flip" /> Obróć planszę</button></div>
      </section>

      <aside className="sidebar">
        <section className={`turn-card ${game?.status === 'CHECK' ? 'is-check' : ''}`} aria-live="polite">
          <div className="turn-caption"><span>{finished(game) ? 'KONIEC PARTII' : 'TERAZ NA PLANSZY'}</span><Icon name="king" /></div>
          <h2>{status}</h2>
          {game?.status === 'CHECK' && <span className="check-badge">Szach — osłoń swojego króla</span>}
          {game?.status === 'CHECKMATE' && <span className="check-badge">Mat. Dobra partia!</span>}
          <p>{game?.computerThinking ? 'Stockfish myśli… Przy pierwszej grze aplikacja pobiera silnik (ok. 80 MB).' : game?.computerError || (finished(game) ? 'Rewanż? Rozpocznij nową partię.' : notice)}</p>
          {game?.computerError && <button className="retry-computer" onClick={retryComputer} disabled={phase !== 'idle'}>Ponów ruch komputera</button>}
        </section>

        <section className="history-card panel"><div className="panel-heading"><h2>Przebieg partii</h2><span>{game?.history.length || 0} ruchów</span></div>
          <div className="history-columns"><span>Nr</span><span>Białe</span><span>Czarne</span></div>
          <div className="history-scroll" ref={history} tabIndex={0} aria-label="Historia ruchów">
            {!rows.length ? <div className="empty-history"><Icon name="arrow" /><p>Każda partia ma swój początek.</p><span>Zacznij od pierwszego ruchu.</span></div> : rows.map((pair, index) => <div className="history-row" key={index}><span className="move-number">{index + 1}.</span><span>{notation(pair[0])}</span><span>{notation(pair[1])}</span></div>)}
          </div>
        </section>

        <section className="captures-card panel"><h2>Poza planszą</h2>{['WHITE', 'BLACK'].map(side => <div className="captured-group" key={side}><span>Zbite {SIDE[side]}</span><div>{captures(side).length ? captures(side).map((piece, index) => <img key={index} src={image(piece)} alt={`${NAMES[piece.type]}, ${SIDE[piece.side]}`} />) : <span className="muted">Jeszcze żadna figura</span>}</div></div>)}</section>

        <button className="new-game" disabled={!game || phase === 'requesting' || phase === 'loading'} onClick={() => setConfirmReset(true)}><Icon name="new" /> Nowa partia</button>
        {game?.canClaimDraw && !computerTurn(game) && <button className="new-game" disabled={phase !== 'idle'} onClick={claimDraw}>Zgłoś remis</button>}
        <p className="session-note">Partia zostanie po odświeżeniu strony.<br />Możecie grać, dopóki aplikacja jest otwarta.</p>
      </aside>
    </main>

    <footer className="page-footer"><span>64 pola. Nieskończenie wiele pomysłów.</span><span>Gramy na zmianę, zaczynają białe.</span></footer>

    {promotion && <Modal title="Czas na awans." onClose={() => setPromotion(null)}><p>Na jaką figurę zamienić pionka?</p><div className="promotion-options">{promotion.map(move => <button key={move.promotion} onClick={() => execute(move)}><img src={image({ side: game.sideToMove, type: move.promotion })} alt="" /><span>{NAMES[move.promotion]}</span></button>)}</div></Modal>}
    {confirmReset && <GameSetup initial={game && { opponent: game.opponent, difficulty: game.difficulty, humanSide: game.humanSide }} onClose={() => setConfirmReset(false)} onPlay={reset} />}
  </div>;
}

createRoot(document.getElementById('root')).render(<App />);
