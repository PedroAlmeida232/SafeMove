import { Text as RNText, type TextProps } from 'react-native';

import { colors, type as typography } from '../theme/tokens';

type Variante = keyof typeof typography;

type Props = TextProps & {
  variant?: Variante;
  color?: string;
};

export function Text({ variant = 'body', color = colors.label, style, ...rest }: Props) {
  return <RNText maxFontSizeMultiplier={1.4} {...rest} style={[typography[variant], { color }, style]} />;
}
