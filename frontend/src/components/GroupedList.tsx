import { Children, Fragment, isValidElement, type ReactNode } from 'react';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

import { colors, hairline, radii, spacing } from '../theme/tokens';
import { Text } from './Text';

type Props = {
  children: ReactNode;
  /** Título da seção (em cima, cinza). */
  header?: string;
  /** Texto de rodapé da seção (embaixo, cinza). */
  footer?: string;
  style?: StyleProp<ViewStyle>;
  /** Recuo do separador a partir da esquerda (alinha com o texto após o tile). */
  separatorInset?: number;
};

/** Lista agrupada no estilo Ajustes do iOS. */
export function GroupedList({ children, header, footer, style, separatorInset = spacing.rowPadding }: Props) {
  const itens = Children.toArray(children).filter(isValidElement);
  return (
    <View style={style}>
      {header ? (
        <Text variant="caption" color={colors.secondaryLabel} style={styles.header}>
          {header.toUpperCase()}
        </Text>
      ) : null}
      <View style={styles.group}>
        {itens.map((item, i) => (
          <Fragment key={item.key ?? i}>
            {i > 0 ? <View style={[styles.separator, { marginLeft: separatorInset }]} /> : null}
            {item}
          </Fragment>
        ))}
      </View>
      {footer ? (
        <Text variant="caption" color={colors.secondaryLabel} style={styles.footer}>
          {footer}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  group: {
    backgroundColor: colors.groupedBg,
    borderRadius: radii.grouped,
    overflow: 'hidden',
  },
  separator: {
    height: hairline,
    backgroundColor: colors.separator,
  },
  header: {
    paddingHorizontal: spacing.rowPadding,
    marginBottom: 6,
  },
  footer: {
    paddingHorizontal: spacing.rowPadding,
    marginTop: 6,
  },
});
