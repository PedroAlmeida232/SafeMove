export type Coordenada = { latitude: number; longitude: number };

/** Praça da Sé — centro usado quando a localização é negada. */
export const PRACA_DA_SE: Coordenada = { latitude: -23.5503, longitude: -46.6339 };

/** Retângulo aproximado do município de São Paulo. */
const SP_BOUNDS = { minLat: -24.01, maxLat: -23.35, minLng: -46.83, maxLng: -46.36 };

export function dentroDeSaoPaulo(c: Coordenada): boolean {
  return (
    c.latitude >= SP_BOUNDS.minLat &&
    c.latitude <= SP_BOUNDS.maxLat &&
    c.longitude >= SP_BOUNDS.minLng &&
    c.longitude <= SP_BOUNDS.maxLng
  );
}

const R = 6371000;
const rad = (d: number) => (d * Math.PI) / 180;

/** Distância em metros entre dois pontos (haversine). */
export function distanciaMetros(a: Coordenada, b: Coordenada): number {
  const dLat = rad(b.latitude - a.latitude);
  const dLng = rad(b.longitude - a.longitude);
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(rad(a.latitude)) * Math.cos(rad(b.latitude)) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

/** Distância (m) de um ponto a um segmento, numa projeção plana local. */
function distanciaAoSegmento(p: Coordenada, a: Coordenada, b: Coordenada): number {
  const k = Math.cos(rad(p.latitude));
  const ax = a.longitude * k, ay = a.latitude;
  const bx = b.longitude * k, by = b.latitude;
  const px = p.longitude * k, py = p.latitude;
  const dx = bx - ax, dy = by - ay;
  const len2 = dx * dx + dy * dy;
  const t = len2 === 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
  return distanciaMetros(p, { latitude: ay + t * dy, longitude: (ax + t * dx) / k });
}

/** Menor distância (m) de um ponto até uma polilinha. */
export function distanciaAteRota(p: Coordenada, rota: Coordenada[]): number {
  if (rota.length === 0) return Infinity;
  if (rota.length === 1) return distanciaMetros(p, rota[0]);
  let min = Infinity;
  for (let i = 0; i < rota.length - 1; i++) {
    const d = distanciaAoSegmento(p, rota[i], rota[i + 1]);
    if (d < min) min = d;
  }
  return min;
}

export function pontoMedio(rota: Coordenada[]): Coordenada {
  return rota[Math.floor(rota.length / 2)] ?? PRACA_DA_SE;
}

/** Arredonda coordenadas para servir de chave de cache (~110 m). */
export function chaveCoord(c: Coordenada): [number, number] {
  return [Math.round(c.latitude * 1000) / 1000, Math.round(c.longitude * 1000) / 1000];
}
