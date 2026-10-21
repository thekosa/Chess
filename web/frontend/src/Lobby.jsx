import React, { useEffect, useState } from 'react';
import { api } from './api';

export function newClientId() {
  if (crypto.randomUUID) return crypto.randomUUID();
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 15) | 64; bytes[8] = (bytes[8] & 63) | 128;
  const hex = Array.from(bytes, value => value.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export function Lobby({ clientId, onEnter, onLocal, initialError }) {
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(initialError || null);
  const [playerName, setPlayerName] = useState(() => {
    try { return localStorage.getItem('szachy.nickname') || ''; } catch { return ''; }
  });
  const [name, setName] = useState('Przy szachownicy');
  const [side, setSide] = useState('WHITE');
  const invited = new URLSearchParams(window.location.search).get('room');
  useEffect(() => {
    let cancelled = false, polling = false;
    async function load() {
      if (polling) return;
      polling = true;
      try {
        const result = await api('/rooms');
        if (!cancelled) { setRooms(result.rooms); setLoading(false); }
      } catch (error) { if (!cancelled) { setError(error.message); setLoading(false); } }
      finally { polling = false; }
    }
    load(); const interval = setInterval(load, 3000);
    return () => { cancelled = true; clearInterval(interval); };
  }, []);
  async function enter(roomId) {
    if (pending) return;
    if (!playerName.trim()) { setError('Najpierw wpisz swój nick.'); return; }
    setPending(true); setError(null);
    try {
      const result = await api(roomId ? `/rooms/${roomId}/join` : '/rooms', roomId
        ? { playerName: playerName.trim(), clientId }
        : { name: name.trim(), playerName: playerName.trim(), side, clientId });
      try { localStorage.setItem('szachy.nickname', playerName.trim()); } catch { /* Optional storage. */ }
      onEnter(result);
    } catch (error) { setError(error.message); }
    finally { setPending(false); }
  }
  const sorted = [...rooms].sort((a, b) => (b.id === invited) - (a.id === invited));
  return <div className="page lobby-page">
    <header className="page-header"><a className="brand" href="/">♟ <span>szachy<span className="brand-period">.</span></span></a>
      <span className="local-label"><i /> Online · dwa miejsca przy planszy</span></header>
    <section className="intro"><div><p className="eyebrow">SPOTKAJMY SIĘ PRZY PLANSZY</p><h1>Wybierz swój pokój.</h1></div>
      <p>Utwórz partię i zaproś znajomego.<br />Możecie grać w przeglądarce lub konsoli.</p></section>
    {error && <div className="error-banner" role="alert"><span>{error}</span><button onClick={() => setError(null)}>Zamknij</button></div>}
    <main className="lobby-layout">
      <section className="room-list panel"><div className="panel-heading"><h2>Pokoje do gry</h2><span>Odświeżane automatycznie</span></div>
        {loading ? <div className="lobby-empty"><span className="spinner" /> Wczytywanie pokoi…</div>
          : !sorted.length ? <div className="lobby-empty"><span className="empty-piece">♞</span><h3>Jeszcze tu spokojnie.</h3><p>Utwórz pierwszy pokój i zaproś kogoś do gry.</p></div>
            : sorted.map(room => <article key={room.id} className={`room-row ${room.id === invited ? 'invited-room' : ''}`}>
              <div className="room-heading"><h3>{room.name}</h3><span className={`room-badge ${room.playerCount === 2 ? 'full' : ''}`}>{room.playerCount}/2 graczy</span></div>
              <div className="room-players">{room.players.map(player => <span key={player.side} className={player.connected ? '' : 'muted'}>
                {player.side === 'WHITE' ? '♔' : '♚'} {player.name}{!player.connected && ' · rozłączony'}</span>)}</div>
              <div className="room-bottom"><span>{room.id === invited ? 'Pokój z Twojego zaproszenia' : room.status === 'FINISHED' ? 'Partia zakończona' : room.playerCount === 2 ? 'Gracze już przy planszy' : 'Wolne miejsce czeka na Ciebie'}</span>
                <button className={room.playerCount === 2 ? 'secondary-button' : 'primary-button'} disabled={pending || room.playerCount >= 2} onClick={() => enter(room.id)}>
                  {room.playerCount >= 2 ? 'Pokój pełny' : 'Dołącz do gry →'}</button></div>
            </article>)}
        {invited && !loading && !rooms.some(room => room.id === invited) && <p className="room-missing">Pokój z zaproszenia został zamknięty lub wygasł. Możesz wybrać inny.</p>}
      </section>
      <aside className="lobby-sidebar">
        <form className="create-room panel" onSubmit={event => { event.preventDefault(); enter(); }}>
          <p className="eyebrow">TWÓJ RUCH</p><h2>Stwórz własny pokój</h2>
          <div className="game-settings"><label>Twój nick<input value={playerName} onChange={event => setPlayerName(event.target.value)} maxLength={24} placeholder="Jak Cię nazywać?" autoComplete="nickname" required /></label>
            <label>Nazwa pokoju<input value={name} onChange={event => setName(event.target.value)} maxLength={40} required /></label>
            <label>Grasz<select value={side} onChange={event => setSide(event.target.value)}><option value="WHITE">Białymi — zaczynasz</option><option value="BLACK">Czarnymi</option></select></label></div>
          <button className="primary-button create-room-button" disabled={pending}>{pending ? 'Łączenie…' : 'Utwórz pokój →'}</button>
          <p>Dokładnie dwóch graczy. Wyślij znajomemu link lub wybierzcie ten sam pokój z listy.</p>
        </form>
        <div className="offline-card"><h3>Masz ochotę na trening?</h3><p>Zagraj ze Stockfishem lub we dwoje na jednym komputerze.</p>
          <button className="new-game" onClick={onLocal} disabled={pending}>Gra lokalna / z komputerem</button></div>
      </aside>
    </main><footer className="page-footer"><span>64 pola. Dwa miejsca. Wasza partia.</span><span>Stan partii jest wspólny dla obu graczy.</span></footer>
  </div>;
}
