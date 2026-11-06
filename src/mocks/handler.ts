/**
 * Simula a API do SafeMove quando EXPO_PUBLIC_USE_MOCK=true.
 * Responde exatamente com os tipos do contrato, sem endpoints extras.
 */
import { ApiError, type Metodo } from '../api/errors';
import type {
  AvaliarRotaBody,
  Incidente,
  NovaOcorrencia,
  Ocorrencia,
  Ponto,
  Radar,
  RotaAvaliada,
  Usuario,
} from '../api/types';
import { dentroDeSaoPaulo, distanciaMetros } from '../utils/geo';
import { areasRisco, incidentesBase, ocorrenciasBase, radaresBase } from './data';

const LATENCIA_MS = 450;
const esperar = (ms: number) => new Promise((r) => setTimeout(r, ms));
const minutosAtras = (m: number) => new Date(Date.now() - m * 60000).toISOString();

/** Ocorrências criadas durante a sessão de demonstração. */
const criadas: Ocorrencia[] = [];
const confirmacoesExtras = new Map<number, number>();
let proximoId = 1000;

function parse(caminho: string): { path: string; params: URLSearchParams } {
  const [path, qs = ''] = caminho.split('?');
  return { path, params: new URLSearchParams(qs) };
}

function centroDe(params: URLSearchParams): Ponto {
  return { latitude: Number(params.get('lat')), longitude: Number(params.get('lng')) };
}

function ocorrenciasPerto(centro: Ponto, raio: number): Ocorrencia[] {
  const base: Ocorrencia[] = ocorrenciasBase.map((o, i) => ({
    id: i + 1,
    tipo: o.tipo,
    descricao: o.descricao,
    latitude: centro.latitude + o.dLat,
    longitude: centro.longitude + o.dLng,
    criadoEm: minutosAtras(o.minutosAtras),
    confirmacoes: o.confirmacoes,
  }));
  return [...criadas, ...base]
    .filter((o) => distanciaMetros(centro, o) <= raio)
    .map((o) => ({ ...o, confirmacoes: o.confirmacoes + (confirmacoesExtras.get(o.id) ?? 0) }));
}

function incidentesPerto(centro: Ponto): Incidente[] {
  return incidentesBase.map((i) => ({
    id: i.id,
    categoria: i.categoria,
    gravidade: i.gravidade,
    descricao: i.descricao,
    via: i.via,
    atrasoSegundos: i.atrasoSegundos,
    latitude: centro.latitude + i.dLat,
    longitude: centro.longitude + i.dLng,
    atualizadoEm: minutosAtras(i.minutosAtras),
  }));
}

function radaresPerto(centro: Ponto): Radar[] {
  return radaresBase.map((r) => ({
    id: r.id,
    velocidadeMax: r.velocidadeMax,
    latitude: centro.latitude + r.dLat,
    longitude: centro.longitude + r.dLng,
  }));
}

/** Gera uma polilinha curva entre origem e destino. */
function tracado(o: Ponto, d: Ponto, curvatura: number): Ponto[] {
  const n = 48;
  const dLat = d.latitude - o.latitude;
  const dLng = d.longitude - o.longitude;
  const pontos: Ponto[] = [];
  for (let i = 0; i <= n; i++) {
    const t = i / n;
    const bojo = Math.sin(Math.PI * t) * curvatura;
    const ondula = Math.sin(Math.PI * t * 6) * 0.0004;
    pontos.push({
      latitude: o.latitude + dLat * t - dLng * bojo + ondula,
      longitude: o.longitude + dLng * t + dLat * bojo,
    });
  }
  return pontos;
}

function rotasMock(body: AvaliarRotaBody): RotaAvaliada[] {
  const metros = Math.max(400, distanciaMetros(body.origem, body.destino));
  const velocidade = body.modo === 'A_PE' ? 1.3 : body.modo === 'BICICLETA' ? 4.2 : 6.5;
  const base = Math.round((metros * 1.3) / velocidade);
  return [
    {
      indice: 1,
      nota: 86,
      classificacao: 'SEGURA',
      distanciaMetros: Math.round(metros * 1.42),
      duracaoSegundos: base + 360,
      atrasoTransitoSegundos: 60,
      resumo: { incidentesGraves: 0, incidentesLeves: 1, ocorrenciasRecentes: 1, assaltosRecentes: 0, riscoMedioRegiao: 2.1, radares: 1 },
      alertas: ['Trânsito leve em um trecho curto'],
      pontos: tracado(body.origem, body.destino, 0.22),
    },
    {
      indice: 0,
      nota: 58,
      classificacao: 'ATENCAO',
      distanciaMetros: Math.round(metros * 1.3),
      duracaoSegundos: base,
      atrasoTransitoSegundos: 240,
      resumo: { incidentesGraves: 1, incidentesLeves: 1, ocorrenciasRecentes: 5, assaltosRecentes: 2, riscoMedioRegiao: 3.8, radares: 2 },
      alertas: [
        '2 assaltos registrados no trajeto nas últimas 3 h',
        'Acidente com lentidão na Av. Brigadeiro Luís Antônio',
        'Trecho em área de risco alto (Sé)',
      ],
      pontos: tracado(body.origem, body.destino, 0.04),
    },
    {
      indice: 2,
      nota: 34,
      classificacao: 'EVITE',
      distanciaMetros: Math.round(metros * 1.25),
      duracaoSegundos: Math.max(60, base - 120),
      atrasoTransitoSegundos: 420,
      resumo: { incidentesGraves: 2, incidentesLeves: 2, ocorrenciasRecentes: 7, assaltosRecentes: 3, riscoMedioRegiao: 4.5, radares: 3 },
      alertas: [
        '3 assaltos registrados no trajeto nas últimas 24 h',
        'Via interditada na R. Augusta',
        'Trecho sem iluminação pública',
      ],
      pontos: tracado(body.origem, body.destino, -0.16),
    },
  ];
}

const usuarioMock: Usuario = {
  uid: 'demo',
  nome: 'Ana Ribeiro',
  email: 'ana@safemove.app',
  criadoEm: new Date().toISOString(),
};

export async function mockRequest<T>(method: Metodo, caminho: string, body: unknown): Promise<T> {
  await esperar(LATENCIA_MS);
  const { path, params } = parse(caminho);
  const responder = (v: unknown) => v as T;

  if (method === 'GET' && path === '/ocorrencias') {
    return responder(ocorrenciasPerto(centroDe(params), Number(params.get('raio') ?? 2000)));
  }
  if (method === 'GET' && path === '/incidentes') return responder(incidentesPerto(centroDe(params)));
  if (method === 'GET' && path === '/radares') return responder(radaresPerto(centroDe(params)));
  if (method === 'GET' && path === '/areas-risco') return responder(areasRisco);
  if (method === 'GET' && path === '/usuarios/me') return responder(usuarioMock);

  if (method === 'POST' && path === '/rotas/avaliar') {
    return responder(rotasMock(body as AvaliarRotaBody));
  }

  if (method === 'POST' && path === '/ocorrencias') {
    const nova = body as NovaOcorrencia;
    if (!dentroDeSaoPaulo(nova)) {
      throw new ApiError(422, 'O local informado fica fora da cidade de São Paulo.');
    }
    const criada: Ocorrencia = {
      id: proximoId++,
      tipo: nova.tipo,
      descricao: nova.descricao ?? null,
      latitude: nova.latitude,
      longitude: nova.longitude,
      criadoEm: new Date().toISOString(),
      confirmacoes: 0,
    };
    criadas.unshift(criada);
    return responder(criada);
  }

  const confirmar = path.match(/^\/ocorrencias\/(\d+)\/confirmar$/);
  if (method === 'POST' && confirmar) {
    const id = Number(confirmar[1]);
    if (criadas.some((o) => o.id === id)) {
      throw new ApiError(422, 'Você não pode confirmar uma ocorrência que você mesmo registrou.');
    }
    confirmacoesExtras.set(id, (confirmacoesExtras.get(id) ?? 0) + 1);
    const base = ocorrenciasBase[id - 1];
    if (!base) throw new ApiError(404, 'Ocorrência não encontrada.');
    return responder({
      id,
      tipo: base.tipo,
      descricao: base.descricao,
      latitude: 0,
      longitude: 0,
      criadoEm: minutosAtras(base.minutosAtras),
      confirmacoes: base.confirmacoes + (confirmacoesExtras.get(id) ?? 0),
    } satisfies Ocorrencia);
  }

  throw new ApiError(404, `Rota de exemplo não encontrada: ${method} ${path}`);
}
