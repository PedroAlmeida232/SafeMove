import type { LucideIcon } from 'lucide-react-native';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

import { colors, radii, shadows } from '../theme/tokens';
import { Touchable } from './Pressable';
import { Text } from './Text';

type Props = {
  label: string;
  icon: LucideIcon;
  /** Cor de fundo (vermelha para alertas, verde para rota segura). */
  color: string;
  onPress?: () => void;
  style?: StyleProp<ViewStyle>;
};

/** Pílula flutuante: raio 99, padding 7×12, texto 13/600 com ícone. */
export function Pill({ label, icon: Icone, color, onPress, style }: Props) {
  const conteudo = (
    <View style={[styles.pill, { backgroundColor: color }, style]}>
      <Icone size={14} color={colors.white} strokeWidth={2.2} />
      <Text variant="captionStrong" color={colors.white}>
        {label}
      </Text>
    </View>
  );
  if (!onPress) {
    return (
      <View accessible accessibilityRole="text" accessibilityLabel={label}>
        {conteudo}
      </View>
    );
  }
  return (
    <Touchable onPress={onPress} accessibilityRole="button" accessibilityLabel={label} hitSlop={8}>
      {conteudo}
    </Touchable>
  );
}

const styles = StyleSheet.create({
  pill: {
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingVertical: 7,
    paddingHorizontal: 12,
    borderRadius: radii.pill,
    boxShadow: shadows.floating,
  },
});
