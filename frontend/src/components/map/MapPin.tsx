import type { LucideIcon } from 'lucide-react-native';
import { memo, useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Marker } from 'react-native-maps';

import { colors, shadows } from '../../theme/tokens';
import type { Coordenada } from '../../utils/geo';

type Props = {
  coordinate: Coordenada;
  color: string;
  icon: LucideIcon;
  onPress?: () => void;
  accessibilityLabel: string;
  selected?: boolean;
  zIndex?: number;
};

/**
 * Hook para desligar tracksViewChanges depois do primeiro desenho:
 * evita re-renderizar marcadores customizados a cada frame (principalmente no Android).
 */
export function useTracksViewChanges(deps: readonly unknown[]): boolean {
  const [track, setTrack] = useState(true);
  useEffect(() => {
    setTrack(true);
    const t = setTimeout(() => setTrack(false), 600);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  return track;
}

/** Pino circular de 30 px com borda branca de 2.5, sombra e ícone branco. */
export const MapPin = memo(function MapPin({
  coordinate,
  color,
  icon: Icone,
  onPress,
  accessibilityLabel,
  selected,
  zIndex,
}: Props) {
  const track = useTracksViewChanges([color, selected]);
  return (
    <Marker
      coordinate={coordinate}
      onPress={onPress}
      anchor={{ x: 0.5, y: 0.5 }}
      tracksViewChanges={track}
      zIndex={selected ? 999 : zIndex}
      accessibilityLabel={accessibilityLabel}
    >
      <View style={styles.hit}>
        <View style={[styles.pin, { backgroundColor: color }, selected && styles.selected]}>
          <Icone size={selected ? 18 : 15} color={colors.white} strokeWidth={2.2} />
        </View>
      </View>
    </Marker>
  );
});

const styles = StyleSheet.create({
  hit: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pin: {
    width: 30,
    height: 30,
    borderRadius: 15,
    borderWidth: 2.5,
    borderColor: colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    boxShadow: shadows.pin,
  },
  selected: {
    width: 38,
    height: 38,
    borderRadius: 19,
  },
});
