import type { ReactNode } from 'react';
import { ActivityIndicator, StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

import { colors, radii, spacing } from '../theme/tokens';
import { Touchable } from './Pressable';
import { Text } from './Text';

type PrimaryProps = {
  title: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  color?: string;
  style?: StyleProp<ViewStyle>;
  accessibilityHint?: string;
};

/** Botão principal: altura 50, raio 14, fundo accent, texto branco 17/600. */
export function PrimaryButton({
  title,
  onPress,
  loading,
  disabled,
  color = colors.accent,
  style,
  accessibilityHint,
}: PrimaryProps) {
  const inativo = disabled || loading;
  return (
    <Touchable
      onPress={onPress}
      disabled={inativo}
      accessibilityRole="button"
      accessibilityLabel={title}
      accessibilityHint={accessibilityHint}
      accessibilityState={{ disabled: inativo, busy: loading }}
      style={[styles.primary, { backgroundColor: color, opacity: disabled ? 0.4 : 1 }, style]}
    >
      {loading ? (
        <ActivityIndicator color={colors.white} />
      ) : (
        <Text variant="bodyStrong" color={colors.white}>
          {title}
        </Text>
      )}
    </Touchable>
  );
}

type TextButtonProps = {
  title: string;
  onPress: () => void;
  disabled?: boolean;
  color?: string;
  weight?: 'regular' | 'strong';
  style?: StyleProp<ViewStyle>;
  accessibilityLabel?: string;
};

/** Botão de texto: só o texto em accent, 17/600, área de toque ≥ 44. */
export function TextButton({
  title,
  onPress,
  disabled,
  color = colors.accent,
  weight = 'strong',
  style,
  accessibilityLabel,
}: TextButtonProps) {
  return (
    <Touchable
      onPress={onPress}
      disabled={disabled}
      hitSlop={6}
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel ?? title}
      accessibilityState={{ disabled }}
      style={[styles.text, style]}
    >
      <Text variant={weight === 'strong' ? 'bodyStrong' : 'body'} color={disabled ? colors.tertiary : color}>
        {title}
      </Text>
    </Touchable>
  );
}

/** Margem lateral padrão do botão principal (largura total menos 16). */
export function ButtonContainer({ children }: { children: ReactNode }) {
  return <View style={styles.container}>{children}</View>;
}

const styles = StyleSheet.create({
  primary: {
    height: 50,
    borderRadius: radii.button,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  text: {
    minHeight: 44,
    minWidth: 44,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 4,
  },
  container: {
    paddingHorizontal: spacing.gutter,
  },
});
