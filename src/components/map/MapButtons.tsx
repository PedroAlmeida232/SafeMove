import { Layers, Navigation } from 'lucide-react-native';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

import { colors, hairline, radii, shadows } from '../../theme/tokens';
import { Touchable } from '../Pressable';

type Props = {
  onCenter: () => void;
  onLayers?: () => void;
  style?: StyleProp<ViewStyle>;
};

/** Coluna branca de 44 px com os botões "centralizar" e "camadas". */
export function MapButtons({ onCenter, onLayers, style }: Props) {
  return (
    <View style={[styles.column, style]}>
      <Touchable
        onPress={onCenter}
        style={styles.button}
        accessibilityRole="button"
        accessibilityLabel="Centralizar na minha localização"
      >
        <Navigation size={20} color={colors.blue} strokeWidth={2} />
      </Touchable>
      {onLayers ? (
        <>
          <View style={styles.separator} />
          <Touchable
            onPress={onLayers}
            style={styles.button}
            accessibilityRole="button"
            accessibilityLabel="Camadas do mapa"
          >
            <Layers size={20} color={colors.blue} strokeWidth={2} />
          </Touchable>
        </>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  column: {
    width: 44,
    backgroundColor: colors.background,
    borderRadius: radii.mapButtons,
    boxShadow: shadows.floating,
  },
  button: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  separator: {
    height: hairline,
    backgroundColor: colors.separator,
    marginHorizontal: 8,
  },
});
