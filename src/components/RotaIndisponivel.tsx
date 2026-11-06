import { router } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors } from '../theme/tokens';
import { ErrorState } from './StateViews';
import { TextButton } from './Buttons';

/** Mostrado quando a tela de rota é aberta sem um planejamento em memória. */
export function RotaIndisponivel() {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <ErrorState message="Essa rota não está mais disponível. Planeje o trajeto de novo." />
      <TextButton title="Planejar rota" onPress={() => router.replace('/rotas')} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.background,
  },
});
