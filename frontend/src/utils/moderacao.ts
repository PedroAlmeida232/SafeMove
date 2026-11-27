/**
 * Filtro simples de texto ofensivo para descrições de ocorrências (Diretriz 1.2 da App Store).
 * Compara palavras inteiras, sem acento e sem diferença de maiúsculas.
 * O backend deve repetir essa checagem; aqui ela só evita o envio.
 */
const PROIBIDAS = [
  'arrombado', 'babaca', 'bosta', 'buceta', 'caralho', 'corno', 'cu', 'cuzao', 'desgraçado', 'escroto',
  'fdp', 'filhodaputa', 'foda', 'foder', 'fodase', 'idiota', 'imbecil', 'macaco', 'merda', 'otario',
  'piranha', 'porra', 'puta', 'puto', 'retardado', 'vadia', 'viado', 'vagabunda', 'vsf', 'pqp',
];

function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase();
}

const CONJUNTO = new Set(PROIBIDAS.map(normalizar));

/** true quando o texto tem alguma palavra da lista. */
export function temConteudoImproprio(texto: string): boolean {
  const palavras = normalizar(texto).split(/[^a-z0-9]+/).filter(Boolean);
  if (palavras.some((p) => CONJUNTO.has(p))) return true;
  // pega "filho da puta", "foda se" etc. escritos com espaço
  const junto = palavras.join('');
  return ['filhodaputa', 'fodase', 'vaisefoder'].some((p) => junto.includes(p));
}
