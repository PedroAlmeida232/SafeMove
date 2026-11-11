import { BottomSheetModalProvider } from '@gorhom/bottom-sheet';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { ApiError } from '../src/api/client';
import { AuthProvider } from '../src/auth/AuthProvider';
import { useAlertaAreaRisco } from '../src/hooks/useAlertaAreaRisco';
import { useMonitorRota } from '../src/hooks/useMonitorRota';
import { AlertasProvider } from '../src/state/AlertasStore';
import { LocationProvider } from '../src/state/LocationProvider';
import { RotaProvider } from '../src/state/RotaStore';
import { colors } from '../src/theme/tokens';

function criarQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        // Não insiste em erros do cliente (4xx); tenta 2× em falhas de rede/servidor.
        retry: (falhas, erro) =>
          !(erro instanceof ApiError && erro.status >= 400 && erro.status < 500) && falhas < 2,
      },
      mutations: { retry: false },
    },
  });
}

/** Observa o trajeto ativo e dispara as notificações locais. */
function MonitorDeRota() {
  useMonitorRota();
  return null;
}

/** Avisa quando o usuário está numa região que não é segura (assaltos e crimes do último mês). */
function MonitorDeArea() {
  useAlertaAreaRisco();
  return null;
}

export default function RootLayout() {
  const [queryClient] = useState(criarQueryClient);

  return (
    <GestureHandlerRootView style={styles.root}>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <LocationProvider>
              <AlertasProvider>
                <RotaProvider>
                  <BottomSheetModalProvider>
                    <MonitorDeRota />
                    <MonitorDeArea />
                    <StatusBar style="dark" />
                    <Stack
                      screenOptions={{
                        headerShown: false,
                        contentStyle: { backgroundColor: colors.background },
                      }}
                    >
                      <Stack.Screen name="(tabs)" />
                      <Stack.Screen name="rota/alerta" />
                      <Stack.Screen name="rota/alternativa" />
                      <Stack.Screen name="login" options={{ presentation: 'modal' }} />
                      <Stack.Screen name="reportar/index" options={{ presentation: 'modal' }} />
                      <Stack.Screen
                        name="reportar/enviada"
                        options={{ presentation: 'modal', gestureEnabled: false }}
                      />
                    </Stack>
                  </BottomSheetModalProvider>
                </RotaProvider>
              </AlertasProvider>
            </LocationProvider>
          </AuthProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.background },
});
