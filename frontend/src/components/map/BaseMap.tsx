import { forwardRef } from 'react';
import { StyleSheet, View } from 'react-native';
import MapView, { PROVIDER_DEFAULT, UrlTile, type MapViewProps } from 'react-native-maps';

import { colors } from '../../theme/tokens';
import { Text } from '../Text';

/**
 * Tiles do OpenStreetMap. A política de uso (operations.osmfoundation.org/policies/tiles) exige o crédito
 * visível e proíbe uso pesado: para produção, troque por um provedor de tiles (MapTiler, Stadia...).
 */
const OSM_TILES = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const OSM_MAX_ZOOM = 19;

/**
 * MapView com o mapa do OpenStreetMap no lugar do Apple Maps / Google Maps, igual no iOS, Android e web.
 * Os filhos (pinos, círculos, rotas) ficam por cima dos tiles. O crédito acompanha o mapPadding.
 */
export const BaseMap = forwardRef<MapView, MapViewProps>(function BaseMap({ children, mapPadding, ...rest }, ref) {
  return (
    <>
      <MapView
        ref={ref}
        provider={PROVIDER_DEFAULT}
        mapType="none"
        mapPadding={mapPadding}
        {...rest}
      >
        <UrlTile
          urlTemplate={OSM_TILES}
          maximumZ={OSM_MAX_ZOOM}
          shouldReplaceMapContent
          tileSize={256}
          zIndex={-1}
        />
        {children}
      </MapView>
      <View style={[styles.credito, { bottom: (mapPadding?.bottom ?? 0) + 4 }]} pointerEvents="none">
        <Text variant="tab" color={colors.secondaryLabel}>
          © OpenStreetMap
        </Text>
      </View>
    </>
  );
});

const styles = StyleSheet.create({
  credito: {
    position: 'absolute',
    right: 6,
    backgroundColor: 'rgba(255,255,255,0.75)',
    borderRadius: 4,
    paddingHorizontal: 4,
    paddingVertical: 1,
  },
});
