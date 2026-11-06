import { ChevronRight } from 'lucide-react-native';
import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

import { colors, spacing } from '../theme/tokens';
import { Touchable } from './Pressable';
import { Text } from './Text';

type Props = {
  title: string;
  subtitle?: string;
  /** Elemento à esquerda (tile, ponto, ícone). */
  left?: ReactNode;
  /** Elemento à direita (switch, valor, botão). */
  right?: ReactNode;
  /** Valor em cinza à direita. */
  value?: string;
  titleColor?: string;
  titleWeight?: 'regular' | 'strong';
  chevron?: boolean;
  onPress?: () => void;
  accessibilityLabel?: string;
  accessibilityHint?: string;
  children?: ReactNode;
};

/** Linha de lista agrupada: altura mínima 48, padding 14. */
export function Row({
  title,
  subtitle,
  left,
  right,
  value,
  titleColor = colors.label,
  titleWeight = 'regular',
  chevron,
  onPress,
  accessibilityLabel,
  accessibilityHint,
  children,
}: Props) {
  const conteudo = (
    <View style={styles.row}>
      {left ? <View style={styles.left}>{left}</View> : null}
      <View style={styles.texts}>
        <Text variant={titleWeight === 'strong' ? 'bodyStrong' : 'body'} color={titleColor} numberOfLines={2}>
          {title}
        </Text>
        {subtitle ? (
          <Text variant="caption" color={colors.secondaryLabel} numberOfLines={2} style={styles.subtitle}>
            {subtitle}
          </Text>
        ) : null}
        {children}
      </View>
      {value ? (
        <Text variant="body" color={colors.secondaryLabel} style={styles.value}>
          {value}
        </Text>
      ) : null}
      {right}
      {chevron ? <ChevronRight size={18} color={colors.tertiary} strokeWidth={2} style={styles.chevron} /> : null}
    </View>
  );

  if (!onPress) return conteudo;
  return (
    <Touchable
      onPress={onPress}
      activeOpacity={0.6}
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel ?? (subtitle ? `${title}, ${subtitle}` : title)}
      accessibilityHint={accessibilityHint}
    >
      {conteudo}
    </Touchable>
  );
}

const styles = StyleSheet.create({
  row: {
    minHeight: spacing.rowMinHeight,
    paddingHorizontal: spacing.rowPadding,
    paddingVertical: 10,
    flexDirection: 'row',
    alignItems: 'center',
  },
  left: {
    marginRight: 12,
  },
  texts: {
    flex: 1,
    justifyContent: 'center',
  },
  subtitle: {
    marginTop: 1,
  },
  value: {
    marginLeft: 8,
  },
  chevron: {
    marginLeft: 6,
  },
});
