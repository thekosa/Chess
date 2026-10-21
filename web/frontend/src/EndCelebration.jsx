import React, { useEffect, useRef, useState } from 'react';
import { resultLabel } from './gameStatus';

/** Visual-only, bounded animation; reset/unmount cancels every scheduled frame. */
export function EndCelebration({ game, flipped }) {
  const canvas = useRef(null);
  const [dismissed, setDismissed] = useState(false);
  useEffect(() => {
    if (dismissed) return;
    let frame, start, previous, cancelled = false;
    const element = canvas.current;
    const context = element.getContext('2d');
    const motion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const images = game.board.map(piece => {
      const image = new Image();
      image.src = `/pieces/${piece.side.toLowerCase()}_${piece.type.toLowerCase()}.png`;
      return image;
    });
    // Coordinates stay in a square 800-unit space on every viewport size.
    const sprites = Array.from({ length: game.board.length ? Math.max(16, game.board.length) : 0 }, (_, index) => {
      const piece = game.board[index % game.board.length];
      const file = piece.square.charCodeAt(0) - 97, rank = Number(piece.square[1]) - 1;
      return { image: images[index % images.length], x: (flipped ? 7 - file : file) * 100,
        y: (flipped ? rank : 7 - rank) * 100, vx: (index % 2 ? -1 : 1) * (110 + index % 5 * 35),
        vy: -190 - index % 4 * 60, delay: index * .07, hue: index * 37 % 360 };
    });
    const deadline = setTimeout(() => setDismissed(true), 6500);
    function draw(now) {
      if (cancelled) return;
      start ??= now;
      previous ??= now;
      const elapsed = (now - start) / 1000;
      const dt = Math.min(.035, (now - previous) / 1000);
      previous = now;
      if (!motion.matches && context) {
        context.fillStyle = 'rgba(18, 37, 30, .10)';
        context.fillRect(0, 0, 800, 800);
        for (const sprite of sprites) {
          if (elapsed < sprite.delay) continue;
          sprite.vy += 760 * dt;
          sprite.x += sprite.vx * dt;
          sprite.y += sprite.vy * dt;
          if (sprite.y > 720) { sprite.y = 720; sprite.vy = -Math.max(260, Math.abs(sprite.vy) * .82); }
          if (sprite.x < 0) { sprite.x = 0; sprite.vx = Math.abs(sprite.vx); }
          if (sprite.x > 720) { sprite.x = 720; sprite.vx = -Math.abs(sprite.vx); }
          context.strokeStyle = `hsla(${(sprite.hue + elapsed * 25) % 360}, 85%, 72%, .8)`;
          context.lineWidth = 4;
          context.beginPath();
          context.moveTo(sprite.x + 40, sprite.y + 40);
          context.lineTo(sprite.x + 40 - sprite.vx * .09, sprite.y + 40 - sprite.vy * .09);
          context.stroke();
          if (sprite.image.complete && sprite.image.naturalWidth) context.drawImage(sprite.image, sprite.x, sprite.y, 80, 80);
        }
      }
      if (elapsed < 6.5) frame = requestAnimationFrame(draw);
    }
    if (!motion.matches) frame = requestAnimationFrame(draw);
    return () => { cancelled = true; cancelAnimationFrame(frame); clearTimeout(deadline); };
  }, [game.id, game.revision, flipped, dismissed]);
  if (dismissed) return null;
  return <div className="end-celebration" data-testid="end-celebration">
    <canvas ref={canvas} width="800" height="800" aria-hidden="true" />
    <div className="result-banner" role="status"><span>KONIEC PARTII</span><h2>{resultLabel(game)}</h2>
      <p>Dobra partia. Czas na rewanż?</p><button onClick={() => setDismissed(true)}>Pomiń efekt</button>
    </div>
  </div>;
}
