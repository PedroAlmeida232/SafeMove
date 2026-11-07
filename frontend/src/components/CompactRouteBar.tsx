import { ChevronLeft } from 'lucide-react-native';
import { StyleSheet, View } from 'react-native';

import { colors, radii, shadows } from '../theme/tokens';
import { Touchable } from './Pressable';
import { Text } from './Text';

type Props = {
  origem: string;
  destino: string;
  tempo: string;
  tempoColor?: string;
  onBack: () => void;
};

/** Barra flutuante compacta: voltar, ponto azul, "Origem → Destino" e tempo. */
export function CompactRouteBar({ origem, destino, tempo, tempoColor = colors.label, onBack }: Props) {
  return (
    <View style={styles.bar}>
      <Touchable
        onPress={onBack}
        hitSlop={4}
        style={styles.back}
        accessibilityRole="button"
        accessibilityLabel="Voltar"
      >
        <ChevronLeft size={22} color={colors.accent} strokeWidth={2.2} />
      </Touchable>
      <View style={styles.dot} />
      <Text variant="secondaryStrong" numberOfLines={1} style={styles.trajeto}>
        {origem} <Text variant="secondary" color={colors.tertiary}>→</Text> {destino}
      </Text>
      <Text variant="secondaryStrong" color={tempoColor} style={styles.tempo}>
        {tempo}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    height: 48,
    borderRadius: radii.searchBar,
    backgroundColor: colors.background,
    flexDirection: 'row',
    alignItems: 'center',
    paddingRight: 14,
    boxShadow: shadows.floating,
  },
  back: {
    width: 40,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: colors.blue,
    marginRight: 8,
  },
  trajeto: {
    flex: 1,
  },
  tempo: {
    marginLeft: 8,
  },
});
