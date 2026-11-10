import { PRACA_DA_SE, dentroDeSaoPaulo, type Coordenada } from '../utils/geo';

/**
 * Busca de endereços no Photon (photon.komoot.io), que usa os dados do OpenStreetMap e permite
 * autocompletar sem chave. O servidor público é de uso moderado: para produção, hospede o Photon
 * ou passe a busca para o backend.
 */
const PHOTON_URL = 'https://photon.komoot.io/api/';
/** minLon,minLat,maxLon,maxLat: o mesmo retângulo de dentroDeSaoPaulo. */
const BBOX_SP = '-46.83,-24.01,-46.36,-23.35';
const MAX_SUGESTOES = 6;

export const MIN_CARACTERES_BUSCA = 3;

export type Lugar = Coordenada & {
  id: string;
  /** "Rua Taquari, 670" ou o nome do lugar ("FIAP"). */
  titulo: string;
  /** "Mooca, São Paulo - SP" */
  subtitulo: string;
};

export type BuscaLugares = {
  lugares: Lugar[];
  /** Nada na cidade de São Paulo, mas o endereço existe em outro lugar. */
  foraDeSaoPaulo: boolean;
};

type PhotonFeature = {
  geometry: { coordinates: [number, number] };
  properties: {
    osm_type?: string;
    osm_id?: number;
    name?: string;
    street?: string;
    housenumber?: string;
    district?: string;
    city?: string;
    state?: string;
    countrycode?: string;
  };
};

async function photon(texto: string, params: Record<string, string>, signal?: AbortSignal): Promise<PhotonFeature[]> {
  const query = new URLSearchParams({
    q: texto,
    lat: String(PRACA_DA_SE.latitude),
    lon: String(PRACA_DA_SE.longitude),
    ...params,
  });
  const resposta = await fetch(`${PHOTON_URL}?${query}`, { signal });
  if (!resposta.ok) throw new Error('Não foi possível buscar endereços agora.');
  const corpo = (await resposta.json()) as { features?: PhotonFeature[] };
  return corpo.features ?? [];
}

/** O retângulo também cobre cidades vizinhas (Osasco, Guarulhos...): confere a cidade pelo nome. */
function naCidadeDeSaoPaulo(f: PhotonFeature): boolean {
  const [longitude, latitude] = f.geometry.coordinates;
  const p = f.properties;
  return p.countrycode === 'BR' && p.city === 'São Paulo' && dentroDeSaoPaulo({ latitude, longitude });
}

function paraLugar(f: PhotonFeature): Lugar {
  const [longitude, latitude] = f.geometry.coordinates;
  const p = f.properties;
  const endereco = p.street ? [p.street, p.housenumber].filter(Boolean).join(', ') : null;
  const titulo = p.name ?? endereco ?? 'Local sem nome';
  const regiao = [p.district, 'São Paulo - SP'].filter(Boolean).join(', ');
  // Só repete o endereço quando o título é o nome do lugar ("Renner"), não quando já é a própria rua.
  const subtitulo = p.name && endereco && !p.name.startsWith(p.street ?? '') ? `${endereco} · ${regiao}` : regiao;
  return { id: `${p.osm_type ?? ''}${p.osm_id ?? `${latitude},${longitude}`}`, titulo, subtitulo, latitude, longitude };
}

/** Sugestões de endereço só da cidade de São Paulo, sem repetir a mesma rua (o OSM a divide em trechos). */
export async function buscarLugares(texto: string, signal?: AbortSignal): Promise<BuscaLugares> {
  const termo = texto.trim();
  if (termo.length < MIN_CARACTERES_BUSCA) return { lugares: [], foraDeSaoPaulo: false };

  const naRegiao = await photon(termo, { bbox: BBOX_SP, limit: '15' }, signal);
  const vistos = new Set<string>();
  const lugares = naRegiao
    .filter(naCidadeDeSaoPaulo)
    .map(paraLugar)
    .filter((l) => {
      const chave = `${l.titulo}|${l.subtitulo}`;
      if (vistos.has(chave)) return false;
      vistos.add(chave);
      return true;
    })
    .slice(0, MAX_SUGESTOES);
  if (lugares.length > 0) return { lugares, foraDeSaoPaulo: false };

  // Nada na cidade: só achou em cidades vizinhas, ou existe em outro lugar do mundo?
  const foraDeSaoPaulo = naRegiao.length > 0 || (await photon(termo, { limit: '1' }, signal)).length > 0;
  return { lugares: [], foraDeSaoPaulo };
}
