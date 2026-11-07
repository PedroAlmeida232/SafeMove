import { StyleSheet, View } from 'react-native';
import { Marker } from 'react-native-maps';

import { colors } from '../../theme/tokens';
import type { Coordenada } from '../../utils/geo';
import { useTracksViewChanges } from './MapPin';

const DOT = 22;
const HALO = 8;

/** Posição do usuário: círculo azul de 22 px, borda branca 3.5 e halo azul de 8 px a 18%. */
export function UserDot({ coordinate }: { coordinate: Coordenada }) {
  const track = useTracksViewChanges([]);
  return (
    <Marker
      coordinate={coordinate}
      anchor={{ x: 0.5, y: 0.5 }}
      tracksViewChanges={track}
      zIndex={1000}
      accessibilityLabel="Sua localização"
    >
      <View style={styles.halo}>
        <View style={styles.dot} />
      </View>
    </Marker>
  );
}

const styles = StyleSheet.create({
  halo: {
    width: DOT + HALO * 2,
    height: DOT + HALO * 2,
    borderRadius: (DOT + HALO * 2) / 2,
    backgroundColor: 'rgba(0,122,255,0.18)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  dot: {
    width: DOT,
    height: DOT,
    borderRadius: DOT / 2,
    backgroundColor: colors.blue,
    borderWidth: 3.5,
    borderColor: colors.white,
    boxShadow: '0px 1px 4px rgba(0,0,0,0.25)',
  },
});
