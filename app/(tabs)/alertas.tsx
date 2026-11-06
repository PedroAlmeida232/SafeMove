import { router } from 'expo-router';
import { BellOff } from 'lucide-react-native';
import { useEffect, useState } from 'react';
import { FlatList, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { AppLogo } from '../../src/components/AppLogo';
import { TextButton } from '../../src/components/Buttons';
import { Touchable } from '../../src/components/Pressable';
import { Loading } from '../../src/components/StateViews';
import { Text } from '../../src/components/Text';
import { useAlertas, type Alerta } from '../../src/state/AlertasStore';
import { useRotas } from '../../src/state/RotaStore';
import { colors, hairline, radii, spacing } from '../../src/theme/tokens';
import { haQuantoTempo } from '../../src/utils/format';

/** Re-renderiza a cada minuto para atualizar o "há X min". */
function useRelogio(): number {
  const [agora, setAgora] = useState(Date.now());
  useEffect(() => {
    const t = setInterval(() => setAgora(Date.now()), 60_000);
    return () => clearInterval(t);
  }, []);
  return agora;
}

function CardAlerta({ alerta, agora }: { alerta: Alerta; agora: number }) {
  const tempo = haQuantoTempo(alerta.criadoEm, agora);
  return (
    <View style={styles.card} accessible accessibilityLabel={`${alerta.titulo}. ${alerta.texto}. ${tempo}`}>
      <View style={styles.cardTopo}>
        <AppLogo size={20} />
        <Text variant="caption" color={colors.secondaryLabel} style={styles.app}>
          SAFEMOVE
        </Text>
        <Text variant="caption" color={colors.secondaryLabel}>
          {tempo}
        </Text>
      </View>
      <Text variant="secondaryStrong" style={styles.cardTitulo}>
        {alerta.titulo}
      </Text>
      <Text variant="secondary" color={colors.label}>
        {alerta.texto}
      </Text>
    </View>
  );
}

export default function AlertasScreen() {
  const insets = useSafeAreaInsets();
  const { alertas, carregado, limpar } = useAlertas();
  const { ativo } = useRotas();
  const agora = useRelogio();

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <View style={styles.cabecalho}>
        <Text variant="largeTitle" accessibilityRole="header">
          Alertas
        </Text>
        {alertas.length > 0 ? <TextButton title="Limpar" weight="regular" onPress={limpar} /> : null}
      </View>

      {ativo ? (
        <Touchable
          style={styles.ativo}
          onPress={() => router.push({ pathname: '/rota/alternativa', params: { segura: String(ativo.rota.indice) } })}
          accessibilityRole="button"
          accessibilityLabel={`Monitorando o trajeto até ${ativo.destinoLabel}`}
        >
          <View style={styles.ativoPonto} />
          <Text variant="secondary" color={colors.secondaryLabel} style={styles.flex} numberOfLines={1}>
            Monitorando o trajeto até <Text variant="secondaryStrong">{ativo.destinoLabel}</Text>
          </Text>
        </Touchable>
      ) : null}

      {!carregado ? (
        <Loading />
      ) : (
        <FlatList
          data={alertas}
          keyExtractor={(a) => a.id}
          renderItem={({ item }) => <CardAlerta alerta={item} agora={agora} />}
          contentContainerStyle={[styles.lista, alertas.length === 0 && styles.listaVazia]}
          ItemSeparatorComponent={() => <View style={styles.espaco} />}
          ListEmptyComponent={
            <View style={styles.vazio}>
              <BellOff size={32} color={colors.tertiary} strokeWidth={1.7} />
              <Text variant="bodyStrong" style={styles.vazioTitulo}>
                Nenhum alerta por enquanto
              </Text>
              <Text variant="secondary" color={colors.secondaryLabel} style={styles.vazioTexto}>
                Inicie um trajeto e o SafeMove avisa se surgir um incidente grave perto da rota ou se ela ficar
                arriscada.
              </Text>
              <TextButton title="Planejar rota" onPress={() => router.navigate('/rotas')} />
            </View>
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  flex: {
    flex: 1,
  },
  cabecalho: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.gutter,
    paddingTop: 12,
    paddingBottom: 12,
  },
  ativo: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginHorizontal: spacing.gutter,
    marginBottom: 12,
    paddingHorizontal: 14,
    minHeight: 44,
    borderRadius: radii.grouped,
    backgroundColor: colors.groupedBg,
  },
  ativoPonto: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: colors.green,
  },
  lista: {
    paddingHorizontal: spacing.gutter,
    paddingBottom: 24,
  },
  listaVazia: {
    flexGrow: 1,
    justifyContent: 'center',
  },
  espaco: {
    height: 8,
  },
  card: {
    backgroundColor: colors.groupedBg,
    borderRadius: 16,
    padding: 14,
    borderWidth: hairline,
    borderColor: 'rgba(60,60,67,0.12)',
  },
  cardTopo: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
  },
  app: {
    flex: 1,
    marginLeft: 8,
    letterSpacing: 0.3,
  },
  cardTitulo: {
    marginBottom: 1,
  },
  vazio: {
    alignItems: 'center',
    paddingHorizontal: 24,
  },
  vazioTitulo: {
    marginTop: 12,
  },
  vazioTexto: {
    textAlign: 'center',
    marginTop: 6,
    marginBottom: 8,
  },
});
