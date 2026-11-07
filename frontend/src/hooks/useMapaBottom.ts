import { useSafeAreaInsets } from 'react-native-safe-area-context';

/** Topo dos elementos flutuantes do mapa (abaixo do notch). */
export function useTopoFlutuante(): number {
  const insets = useSafeAreaInsets();
  return insets.top + 8;
}
