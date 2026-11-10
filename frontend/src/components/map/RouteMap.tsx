import { forwardRef, useCallback, useImperativeHandle, useRef, type ReactNode } from 'react';
import { StyleSheet } from 'react-native';
import type MapView from 'react-native-maps';
import type { EdgePadding } from 'react-native-maps';

import { colors } from '../../theme/tokens';
import { BaseMap } from './BaseMap';
import type { Coordenada } from '../../utils/geo';

export type RouteMapHandle = {
  enquadrar: () => void;
  seguir: (c: Coordenada) => void;
};

type Props = {
  /** Pontos usados para enquadrar o mapa. */
  pontos: Coordenada[];
  padding: EdgePadding;
  children: ReactNode;
};

/** Mapa em tela cheia que enquadra a rota ao abrir. */
export const RouteMap = forwardRef<RouteMapHandle, Props>(function RouteMap({ pontos, padding, children }, ref) {
  const mapRef = useRef<MapView>(null);

  const enquadrar = useCallback(() => {
    if (pontos.length < 2) return;
    mapRef.current?.fitToCoordinates(pontos, { edgePadding: padding, animated: true });
  }, [pontos, padding]);

  useImperativeHandle(
    ref,
    () => ({
      enquadrar,
      seguir: (c) => mapRef.current?.animateCamera({ center: c, zoom: 17, pitch: 0 }, { duration: 600 }),
    }),
    [enquadrar],
  );

  const inicial = pontos[0];

  return (
    <BaseMap
      ref={mapRef}
      style={[StyleSheet.absoluteFill, styles.map]}
      initialRegion={
        inicial ? { ...inicial, latitudeDelta: 0.05, longitudeDelta: 0.05 } : undefined
      }
      onMapReady={() => setTimeout(enquadrar, 50)}
      showsUserLocation={false}
      showsMyLocationButton={false}
      showsCompass={false}
      toolbarEnabled={false}
      pitchEnabled={false}
    >
      {children}
    </BaseMap>
  );
});

const styles = StyleSheet.create({
  map: {
    backgroundColor: colors.groupedBg,
  },
});
