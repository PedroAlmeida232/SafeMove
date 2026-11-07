import { Pressable as RNPressable, type PressableProps, type StyleProp, type ViewStyle } from 'react-native';

type Props = Omit<PressableProps, 'style'> & {
  style?: StyleProp<ViewStyle>;
  /** Opacidade aplicada enquanto pressionado (feedback no estilo iOS). */
  activeOpacity?: number;
};

/** Pressable com feedback de toque por opacidade. */
export function Touchable({ style, activeOpacity = 0.55, disabled, ...rest }: Props) {
  return (
    <RNPressable
      {...rest}
      disabled={disabled}
      style={({ pressed }) => [style, pressed && !disabled ? { opacity: activeOpacity } : null]}
    />
  );
}
