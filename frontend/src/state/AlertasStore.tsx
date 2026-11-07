import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Notifications from 'expo-notifications';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { Platform } from 'react-native';

export type Alerta = {
  id: string;
  titulo: string;
  texto: string;
  criadoEm: string;
  nivel: 'risco' | 'atencao';
};

type AlertasContextValue = {
  alertas: Alerta[];
  carregado: boolean;
  notificar: (a: Omit<Alerta, 'id' | 'criadoEm'>) => Promise<void>;
  limpar: () => Promise<void>;
};

const CHAVE = 'safemove:alertas';
const MAX_ALERTAS = 50;

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

let permissaoPedida = false;

async function garantirPermissao(): Promise<boolean> {
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('alertas', {
      name: 'Alertas de rota',
      importance: Notifications.AndroidImportance.HIGH,
      lightColor: '#E11D48',
    });
  }
  const atual = await Notifications.getPermissionsAsync();
  if (atual.granted) return true;
  if (permissaoPedida && !atual.canAskAgain) return false;
  permissaoPedida = true;
  const nova = await Notifications.requestPermissionsAsync();
  return nova.granted;
}

const AlertasContext = createContext<AlertasContextValue | null>(null);

export function AlertasProvider({ children }: { children: ReactNode }) {
  const [alertas, setAlertas] = useState<Alerta[]>([]);
  const [carregado, setCarregado] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(CHAVE)
      .then((json) => json && setAlertas(JSON.parse(json) as Alerta[]))
      .catch(() => undefined)
      .finally(() => setCarregado(true));
  }, []);

  const salvar = useCallback((lista: Alerta[]) => {
    AsyncStorage.setItem(CHAVE, JSON.stringify(lista)).catch(() => undefined);
  }, []);

  const notificar = useCallback(
    async (a: Omit<Alerta, 'id' | 'criadoEm'>) => {
      const novo: Alerta = {
        ...a,
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        criadoEm: new Date().toISOString(),
      };
      setAlertas((lista) => {
        const proxima = [novo, ...lista].slice(0, MAX_ALERTAS);
        salvar(proxima);
        return proxima;
      });
      try {
        if (await garantirPermissao()) {
          await Notifications.scheduleNotificationAsync({
            content: { title: a.titulo, body: a.texto, sound: true },
            trigger: Platform.OS === 'android' ? { channelId: 'alertas' } : null,
          });
        }
      } catch {
        // Sem permissão: o alerta continua visível na aba Alertas.
      }
    },
    [salvar],
  );

  const limpar = useCallback(async () => {
    setAlertas([]);
    await AsyncStorage.removeItem(CHAVE);
  }, []);

  const value = useMemo(() => ({ alertas, carregado, notificar, limpar }), [alertas, carregado, notificar, limpar]);
  return <AlertasContext.Provider value={value}>{children}</AlertasContext.Provider>;
}

export function useAlertas(): AlertasContextValue {
  const ctx = useContext(AlertasContext);
  if (!ctx) throw new Error('useAlertas precisa estar dentro de <AlertasProvider>.');
  return ctx;
}

export { garantirPermissao as pedirPermissaoNotificacoes };
