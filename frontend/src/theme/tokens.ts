import { Platform, type TextStyle } from 'react-native';

/** Paleta do SafeMove (Apple HIG, tema claro). */
export const colors = {
  accent: '#E11D48',
  red: '#FF3B30',
  /** Vermelho para TEXTO (contraste AA sobre branco e #F2F2F7). */
  redText: '#D70015',
  /** Laranja para TEXTO (contraste AA). */
  orangeText: '#C93400',
  orange: '#FF9500',
  green: '#34C759',
  greenText: '#248A3D',
  blue: '#007AFF',
  label: '#000000',
  secondaryLabel: 'rgba(60,60,67,0.6)',
  tertiary: '#8E8E93',
  groupedBg: '#F2F2F7',
  separator: 'rgba(60,60,67,0.29)',
  background: '#FFFFFF',
  tabBarBg: 'rgba(249,249,249,0.97)',
  tabInactive: '#999999',
  grabber: 'rgba(60,60,67,0.3)',
  gray: '#8E8E93',
  white: '#FFFFFF',
  /** Fundo do card selecionado na grade de tipos. */
  accentTint: 'rgba(225,29,72,0.08)',
} as const;

export type ColorToken = keyof typeof colors;

const fontFamily = Platform.select({ ios: 'System', default: undefined });

export const type = {
  largeTitle: { fontFamily, fontSize: 34, lineHeight: 41, fontWeight: '700', letterSpacing: 0.37 },
  sheetTitle: { fontFamily, fontSize: 22, lineHeight: 28, fontWeight: '700', letterSpacing: -0.3 },
  body: { fontFamily, fontSize: 17, lineHeight: 22, fontWeight: '400', letterSpacing: -0.41 },
  bodyStrong: { fontFamily, fontSize: 17, lineHeight: 22, fontWeight: '600', letterSpacing: -0.41 },
  secondary: { fontFamily, fontSize: 15, lineHeight: 20, fontWeight: '400', letterSpacing: -0.24 },
  secondaryStrong: { fontFamily, fontSize: 15, lineHeight: 20, fontWeight: '600', letterSpacing: -0.24 },
  caption: { fontFamily, fontSize: 13, lineHeight: 18, fontWeight: '400', letterSpacing: -0.08 },
  captionStrong: { fontFamily, fontSize: 13, lineHeight: 18, fontWeight: '600', letterSpacing: -0.08 },
  tab: { fontFamily, fontSize: 10, lineHeight: 12, fontWeight: '500', letterSpacing: 0.1 },
  stat: { fontFamily, fontSize: 28, lineHeight: 34, fontWeight: '700', letterSpacing: 0.36 },
} satisfies Record<string, TextStyle>;

export const radii = {
  searchBar: 14,
  pill: 99,
  mapButtons: 12,
  sheet: 16,
  grouped: 12,
  tile: 8,
  tileLarge: 12,
  button: 14,
} as const;

export const spacing = {
  gutter: 16,
  rowPadding: 14,
  rowMinHeight: 48,
} as const;

/** Sombras em sintaxe CSS (suportada pelo React Native na nova arquitetura). */
export const shadows = {
  floating: '0px 2px 12px rgba(0,0,0,0.12)',
  sheet: '0px -4px 24px rgba(0,0,0,0.10)',
  pin: '0px 1px 4px rgba(0,0,0,0.30)',
  card: '0px 1px 3px rgba(0,0,0,0.08)',
} as const;

export const hairline = 0.5;

export const ICON_STROKE = 1.8;

/** Altura da tab bar sem a safe area (padrão iOS). */
export const TAB_BAR_BASE_HEIGHT = 49;
