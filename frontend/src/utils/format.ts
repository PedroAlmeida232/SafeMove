/** "agora", "há 5 min", "há 2 h", "há 3 d". */
export function haQuantoTempo(iso: string, agora: number = Date.now()): string {
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return '';
  const diffMin = Math.max(0, Math.floor((agora - t) / 60000));
  if (diffMin < 1) return 'agora';
  if (diffMin < 60) return `há ${diffMin} min`;
  const h = Math.floor(diffMin / 60);
  if (h < 24) return `há ${h} h`;
  return `há ${Math.floor(h / 24)} d`;
}

/** 850 → "850 m"; 4230 → "4,2 km". */
export function metrosParaKm(m: number): string {
  if (m < 1000) return `${Math.round(m)} m`;
  const km = m / 1000;
  return `${km.toLocaleString('pt-BR', { maximumFractionDigits: km < 10 ? 1 : 0 })} km`;
}

/** Arredonda segundos para minutos inteiros (mínimo 1). */
export function segundosParaMinutos(s: number): number {
  return Math.max(1, Math.round(s / 60));
}

/** 720 → "12 min"; 4500 → "1 h 15 min". */
export function segundosParaTexto(s: number): string {
  const min = segundosParaMinutos(s);
  if (min < 60) return `${min} min`;
  const h = Math.floor(min / 60);
  const r = min % 60;
  return r === 0 ? `${h} h` : `${h} h ${String(r).padStart(2, '0')} min`;
}

/** Diferença com sinal tipográfico: +6, −4, 0. */
export function diferencaComSinal(n: number): string {
  if (n > 0) return `+${n}`;
  if (n < 0) return `−${Math.abs(n)}`;
  return '0';
}

/** Iniciais para o avatar: "Maria Souza" → "MS"; "joao@x.com" → "J". */
export function iniciais(nomeOuEmail: string | null | undefined): string {
  if (!nomeOuEmail) return '';
  const base = nomeOuEmail.includes('@') ? nomeOuEmail.split('@')[0] : nomeOuEmail;
  const partes = base.trim().split(/[\s._-]+/).filter(Boolean);
  if (partes.length === 0) return '';
  const primeira = partes[0][0] ?? '';
  const ultima = partes.length > 1 ? (partes[partes.length - 1][0] ?? '') : '';
  return (primeira + ultima).toUpperCase();
}

export function primeiroNome(nome: string | null | undefined): string {
  if (!nome) return '';
  const base = nome.includes('@') ? nome.split('@')[0] : nome;
  const p = base.trim().split(/\s+/)[0] ?? '';
  return p.charAt(0).toUpperCase() + p.slice(1);
}
