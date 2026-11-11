// react-native-maps não suporta web: esta versão usa Leaflet com a mesma API que o app usa
// (MapView, Marker, Circle, Polyline, Polygon, UrlTile e os métodos do ref).
import 'leaflet/dist/leaflet.css';

import * as L from 'leaflet';
import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { createPortal } from 'react-dom';
import { View, type StyleProp, type ViewStyle } from 'react-native';

export const PROVIDER_DEFAULT = null;
export const PROVIDER_GOOGLE = 'google';
export type EdgePadding = { top: number; right: number; bottom: number; left: number };

type LatLng = { latitude: number; longitude: number };
type Region = LatLng & { latitudeDelta: number; longitudeDelta: number };

/** Praça da Sé, quando a tela não informa a região inicial. */
const CENTRO_PADRAO: Region = { latitude: -23.5503, longitude: -46.6339, latitudeDelta: 0.05, longitudeDelta: 0.05 };
const ZOOM_MAXIMO = 19;

const MapaContext = createContext<L.Map | null>(null);

const ll = (c: LatLng): L.LatLngTuple => [c.latitude, c.longitude];

/** Zoom do Leaflet equivalente a um intervalo de longitude visível. */
function zoomDeDelta(longitudeDelta: number): number {
  return Math.min(ZOOM_MAXIMO, Math.max(3, Math.log2(360 / longitudeDelta)));
}

function regiaoDe(map: L.Map): Region {
  const b = map.getBounds();
  const c = map.getCenter();
  return {
    latitude: c.lat,
    longitude: c.lng,
    latitudeDelta: b.getNorth() - b.getSouth(),
    longitudeDelta: b.getEast() - b.getWest(),
  };
}

/** Desloca o centro para compensar o mapPadding (ex.: a bottom sheet cobrindo a parte de baixo). */
function centroComPadding(map: L.Map, centro: LatLng, zoom: number, padding?: Partial<EdgePadding>): L.LatLng {
  const dx = ((padding?.right ?? 0) - (padding?.left ?? 0)) / 2;
  const dy = ((padding?.bottom ?? 0) - (padding?.top ?? 0)) / 2;
  const ponto = map.project(ll(centro), zoom).add([dx, dy]);
  return map.unproject(ponto, zoom);
}

type MapViewProps = {
  style?: StyleProp<ViewStyle>;
  children?: ReactNode;
  initialRegion?: Region;
  mapPadding?: Partial<EdgePadding>;
  onPress?: () => void;
  onMapReady?: () => void;
  onRegionChangeComplete?: (r: Region) => void;
};

const MapView = forwardRef<unknown, MapViewProps>(function MapView(
  { style, children, initialRegion, mapPadding, onPress, onMapReady, onRegionChangeComplete },
  ref,
) {
  const [map, setMap] = useState<L.Map | null>(null);
  // Os callbacks mudam a cada render; os eventos do Leaflet leem sempre o mais recente.
  const cb = useRef({ initialRegion, onPress, onMapReady, onRegionChangeComplete, mapPadding });
  useEffect(() => {
    cb.current = { initialRegion, onPress, onMapReady, onRegionChangeComplete, mapPadding };
  });

  // Callback ref (com limpeza, React 19): cria o mapa quando a div entra na página e o remove ao sair.
  // A região inicial só vale na criação, como no react-native-maps.
  const montar = useCallback((div: HTMLDivElement | null) => {
    if (!div) return;
    const inicial = cb.current.initialRegion ?? CENTRO_PADRAO;
    const m = L.map(div, {
      zoomControl: false,
      attributionControl: false, // o crédito do OpenStreetMap é desenhado pelo BaseMap
      zoomSnap: 0,
      maxZoom: ZOOM_MAXIMO,
    });
    const zoom = zoomDeDelta(inicial.longitudeDelta);
    m.setView(centroComPadding(m, inicial, zoom, cb.current.mapPadding), zoom);
    m.on('click', () => cb.current.onPress?.());
    m.on('moveend', () => cb.current.onRegionChangeComplete?.(regiaoDe(m)));

    // O RN Web mede o layout depois do primeiro desenho: sem isso o mapa fica com tamanho 0.
    const observador = new ResizeObserver(() => m.invalidateSize());
    observador.observe(div);

    setMap(m);
    cb.current.onMapReady?.();
    return () => {
      observador.disconnect();
      m.remove();
      setMap(null);
    };
  }, []);

  useImperativeHandle(
    ref,
    () => ({
      animateToRegion: (r: Region, duracao = 500) => {
        if (!map) return;
        const zoom = zoomDeDelta(r.longitudeDelta);
        map.flyTo(centroComPadding(map, r, zoom, cb.current.mapPadding), zoom, { duration: duracao / 1000 });
      },
      animateCamera: (camera: { center?: LatLng; zoom?: number }, opcoes?: { duration?: number }) => {
        if (!map) return;
        const zoom = camera.zoom ?? map.getZoom();
        const centro = camera.center ? centroComPadding(map, camera.center, zoom, cb.current.mapPadding) : map.getCenter();
        map.flyTo(centro, zoom, { duration: (opcoes?.duration ?? 500) / 1000 });
      },
      fitToCoordinates: (coords: LatLng[], opcoes?: { edgePadding?: EdgePadding; animated?: boolean }) => {
        if (!map || coords.length === 0) return;
        const p = opcoes?.edgePadding;
        map.fitBounds(L.latLngBounds(coords.map(ll)), {
          paddingTopLeft: [p?.left ?? 0, p?.top ?? 0],
          paddingBottomRight: [p?.right ?? 0, p?.bottom ?? 0],
          animate: opcoes?.animated ?? true,
        });
      },
      fitToElements: () => {},
    }),
    [map],
  );

  return (
    <View style={style}>
      {/* zIndex 0 cria um contexto de empilhamento: os panes do Leaflet (z-index 400+) não cobrem a UI. */}
      <div ref={montar} style={{ position: 'absolute', inset: 0, zIndex: 0 }} />
      {map ? <MapaContext.Provider value={map}>{children}</MapaContext.Provider> : null}
    </View>
  );
});

export default MapView;

/** Camada de tiles (o BaseMap usa para o OpenStreetMap). */
export function UrlTile({ urlTemplate, maximumZ, tileSize, zIndex }: {
  urlTemplate: string;
  maximumZ?: number;
  tileSize?: number;
  zIndex?: number;
}) {
  const map = useContext(MapaContext);
  useEffect(() => {
    if (!map) return;
    const camada = L.tileLayer(urlTemplate, { maxZoom: maximumZ ?? ZOOM_MAXIMO, tileSize: tileSize ?? 256, zIndex });
    camada.addTo(map);
    return () => {
      camada.remove();
    };
  }, [map, urlTemplate, maximumZ, tileSize, zIndex]);
  return null;
}

type MarkerProps = {
  coordinate: LatLng;
  onPress?: () => void;
  zIndex?: number;
  accessibilityLabel?: string;
  children?: ReactNode;
};

/** Marcador com conteúdo React (o mesmo View do app), centralizado no ponto como anchor 0.5/0.5. */
export function Marker({ coordinate, onPress, zIndex, accessibilityLabel, children }: MarkerProps) {
  const map = useContext(MapaContext);
  // O conteúdo é desenhado (via portal) dentro deste elemento, que vira o ícone do marcador.
  const [conteudo] = useState(() => document.createElement('div'));
  const marcador = useRef<L.Marker | null>(null);
  const onPressRef = useRef(onPress);
  useEffect(() => {
    onPressRef.current = onPress;
  });

  const { latitude, longitude } = coordinate;
  useEffect(() => {
    if (!map) return;
    const m = L.marker([latitude, longitude], {
      icon: L.divIcon({ className: 'safemove-marker', html: conteudo, iconSize: [0, 0] }),
      keyboard: false,
    });
    m.on('click', () => onPressRef.current?.());
    m.addTo(map);
    marcador.current = m;
    return () => {
      m.remove();
      marcador.current = null;
    };
    // A posição é atualizada no efeito abaixo, sem recriar o marcador.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, conteudo]);

  useEffect(() => {
    marcador.current?.setLatLng([latitude, longitude]);
  }, [latitude, longitude]);

  useEffect(() => {
    marcador.current?.setZIndexOffset((zIndex ?? 0) * 100);
  }, [zIndex, map]);

  useEffect(() => {
    if (accessibilityLabel) conteudo.setAttribute('aria-label', accessibilityLabel);
    conteudo.setAttribute('role', onPress ? 'button' : 'img');
  }, [conteudo, accessibilityLabel, onPress]);

  return createPortal(
    <div style={{ position: 'absolute', transform: 'translate(-50%, -50%)', cursor: onPress ? 'pointer' : 'default' }}>
      {children}
    </div>,
    conteudo,
  );
}

type LinhaProps = {
  strokeColor?: string;
  strokeWidth?: number;
  fillColor?: string;
  lineCap?: 'butt' | 'round' | 'square';
  lineJoin?: 'miter' | 'round' | 'bevel';
  lineDashPattern?: number[];
  zIndex?: number;
};

const Z_LINHA = Symbol('zIndex');
type Camada = L.Path & { [Z_LINHA]?: number };

/** O Leaflet desenha na ordem de inserção; reordena pelas zIndex, como no react-native-maps. */
function reordenar(map: L.Map) {
  const camadas: Camada[] = [];
  map.eachLayer((c) => {
    if (c instanceof L.Path) camadas.push(c as Camada);
  });
  camadas.sort((a, b) => (a[Z_LINHA] ?? 0) - (b[Z_LINHA] ?? 0)).forEach((c) => c.bringToFront());
}

function opcoesDeLinha(p: LinhaProps): L.PathOptions {
  return {
    color: p.strokeColor ?? '#000',
    weight: p.strokeWidth ?? 1,
    opacity: 1,
    fill: p.fillColor !== undefined,
    fillColor: p.fillColor,
    fillOpacity: 1, // a transparência já vem no rgba de fillColor
    lineCap: p.lineCap,
    lineJoin: p.lineJoin,
    dashArray: p.lineDashPattern?.join(' '),
    interactive: false,
  };
}

function useCamada(criar: () => Camada, deps: unknown[], zIndex?: number) {
  const map = useContext(MapaContext);
  useEffect(() => {
    if (!map) return;
    const camada = criar();
    camada[Z_LINHA] = zIndex;
    camada.addTo(map);
    reordenar(map);
    return () => {
      camada.remove();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, zIndex, ...deps]);
}

export function Circle({ center, radius, ...linha }: LinhaProps & { center: LatLng; radius: number }) {
  useCamada(
    () => L.circle(ll(center), { radius, ...opcoesDeLinha(linha) }),
    [center.latitude, center.longitude, radius, JSON.stringify(linha)],
    linha.zIndex,
  );
  return null;
}

export function Polyline({ coordinates, ...linha }: LinhaProps & { coordinates: LatLng[] }) {
  useCamada(
    () => L.polyline(coordinates.map(ll), { ...opcoesDeLinha(linha), fill: false }),
    [coordinates, JSON.stringify(linha)],
    linha.zIndex,
  );
  return null;
}

export function Polygon({ coordinates, ...linha }: LinhaProps & { coordinates: LatLng[] }) {
  useCamada(
    () => L.polygon(coordinates.map(ll), opcoesDeLinha(linha)),
    [coordinates, JSON.stringify(linha)],
    linha.zIndex,
  );
  return null;
}
