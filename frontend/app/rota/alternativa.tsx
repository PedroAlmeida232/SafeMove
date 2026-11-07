import * as Haptics from 'expo-haptics';
import { router, useLocalSearchParams } from 'expo-router';
import { Flag, Navigation, ShieldCheck } from 'lucide-react-native';
import { useEffect, useMemo, useRef } from 'react';
import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { RotaAvaliada } from '../../src/api/types';
import { PrimaryButton, TextButton } from '../../src/components/Buttons';
import { CompactRouteBar } from '../../src/components/CompactRouteBar';
import { GroupedList } from '../../src/components/GroupedList';
import { IconTile } from '../../src/components/IconTile';
import { MapPin } from '../../src/components/map/MapPin';
import { RouteLine } from '../../src/components/map/RouteLine';
import { RouteMap, type RouteMapHandle } from '../../src/components/map/RouteMap';
import { UserDot } from '../../src/components/map/UserDot';
import { Pill } from '../../src/components/Pill';
import { Row } from '../../src/components/Row';
import { RotaIndisponivel } from '../../src/components/RotaIndisponivel';
import { Sheet } from '../../src/components/Sheet';
import { SheetHeader } from '../../src/components/SheetHeader';
import { Text } from '../../src/components/Text';
import { useRotaPorIndice } from '../../src/hooks/useRotaSelecionada';
import { useTopoFlutuante } from '../../src/hooks/useMapaBottom';
import { pedirPermissaoNotificacoes } from '../../src/state/AlertasStore';
import { useLocation } from '../../src/state/LocationProvider';
import { useRotas } from '../../src/state/RotaStore';
import { colors } from '../../src/theme/tokens';
import { diferencaComSinal, metrosParaKm, segundosParaMinutos, segundosParaTexto } from '../../src/utils/format';

/** Laranja e verde com contraste AA sobre fundo cinza-claro. */
const COR_PIOR = colors.orangeText;
const COR_MELHOR = colors.greenText;

function Diferenca({ valor, menorEhMelhor }: { valor: number; menorEhMelhor: boolean }) {
  if (valor === 0) return null;
  const melhor = menorEhMelhor ? valor < 0 : valor > 0;
  return (
    <Text variant="bodyStrong" color={melhor ? COR_MELHOR : COR_PIOR} style={styles.diff}>
      {diferencaComSinal(valor)}
    </Text>
  );
}

/** Quando a rota segura foi escolhida direto, comparamos com a alternativa mais rápida. */
function escolherDescartada(rotas: RotaAvaliada[], segura: RotaAvaliada): RotaAvaliada | undefined {
  return rotas
    .filter((r) => r.indice !== segura.indice)
    .reduce<RotaAvaliada | undefined>((m, r) => (!m || r.duracaoSegundos < m.duracaoSegundos ? r : m), undefined);
}

export default function RotaAlternativaScreen() {
  const params = useLocalSearchParams<{ segura: string; descartada?: string }>();
  const insets = useSafeAreaInsets();
  const topo = useTopoFlutuante();
  const mapRef = useRef<RouteMapHandle>(null);
  const { planejamento, ativo, iniciarTrajeto, encerrarTrajeto } = useRotas();
  const { posicao } = useLocation();

  const segura = useRotaPorIndice(params.segura);
  const descartadaParam = useRotaPorIndice(params.descartada);
  const descartada = useMemo(
    () => descartadaParam ?? (planejamento && segura ? escolherDescartada(planejamento.rotas, segura) : undefined),
    [descartadaParam, planejamento, segura],
  );

  const navegando =
    ativo !== null && segura !== undefined && ativo.rota.indice === segura.indice && ativo.destinoLabel === planejamento?.destinoLabel;

  const padding = useMemo(
    () => ({ top: topo + 110, right: 48, bottom: navegando ? 200 : 440, left: 48 }),
    [topo, navegando],
  );

  // Durante o trajeto, a câmera acompanha a posição do usuário.
  useEffect(() => {
    if (navegando && posicao) mapRef.current?.seguir(posicao);
  }, [navegando, posicao]);

  if (!planejamento || !segura) return <RotaIndisponivel />;

  const minSegura = segundosParaMinutos(segura.duracaoSegundos);
  const fim = segura.pontos[segura.pontos.length - 1];

  const iniciar = async () => {
    iniciarTrajeto(segura);
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => undefined);
    pedirPermissaoNotificacoes().catch(() => undefined);
  };

  const encerrar = () => {
    encerrarTrajeto();
    router.navigate('/');
  };

  return (
    <View style={styles.container}>
      <RouteMap ref={mapRef} pontos={segura.pontos} padding={padding}>
        {descartada && !navegando ? <RouteLine coordinates={descartada.pontos} variant="descartada" /> : null}
        <RouteLine coordinates={segura.pontos} variant="segura" />
        {fim ? (
          <MapPin
            coordinate={fim}
            color={colors.accent}
            icon={Flag}
            accessibilityLabel={`Destino: ${planejamento.destinoLabel}`}
          />
        ) : null}
        {posicao ? <UserDot coordinate={posicao} /> : segura.pontos[0] ? <UserDot coordinate={segura.pontos[0]} /> : null}
      </RouteMap>

      <View style={[styles.topo, { top: topo }]} pointerEvents="box-none">
        <CompactRouteBar
          origem={planejamento.origemLabel}
          destino={planejamento.destinoLabel}
          tempo={segundosParaTexto(segura.duracaoSegundos)}
          tempoColor={colors.greenText}
          onBack={() => router.back()}
        />
        <Pill label="Rota segura" icon={ShieldCheck} color={colors.green} style={styles.pill} />
      </View>

      <Sheet bottomPadding={insets.bottom + 12}>
        {navegando ? (
          <>
            <SheetHeader
              tile={<IconTile icon={Navigation} color={colors.green} size="large" />}
              title="Em trajeto"
              subtitle={`${segundosParaTexto(segura.duracaoSegundos)} · ${metrosParaKm(segura.distanciaMetros)} até ${planejamento.destinoLabel}`}
            />
            <Text variant="caption" color={colors.secondaryLabel} style={styles.nota}>
              Vamos avisar se surgir um incidente grave perto da rota ou se ela ficar arriscada.
            </Text>
            <TextButton title="Encerrar trajeto" color={colors.redText} onPress={encerrar} style={styles.encerrar} />
          </>
        ) : (
          <>
            <SheetHeader
              tile={<IconTile icon={ShieldCheck} color={colors.green} size="large" />}
              title="Rota mais segura encontrada"
              subtitle="Evita o trecho com ocorrências recentes."
            />
            <GroupedList style={styles.lista}>
              <Row
                title="Tempo"
                right={
                  <View style={styles.valor}>
                    <Text variant="body" color={colors.secondaryLabel}>
                      {minSegura} min
                    </Text>
                    {descartada ? (
                      <Diferenca
                        valor={minSegura - segundosParaMinutos(descartada.duracaoSegundos)}
                        menorEhMelhor
                      />
                    ) : null}
                  </View>
                }
              />
              <Row
                title="Ocorrências no trajeto"
                right={
                  <View style={styles.valor}>
                    <Text variant="body" color={colors.secondaryLabel}>
                      {segura.resumo.ocorrenciasRecentes}
                    </Text>
                    {descartada ? (
                      <Diferenca
                        valor={segura.resumo.ocorrenciasRecentes - descartada.resumo.ocorrenciasRecentes}
                        menorEhMelhor
                      />
                    ) : null}
                  </View>
                }
              />
              <Row
                title="Nota"
                right={
                  <View style={styles.valor}>
                    {descartada ? (
                      <Text variant="body" color={colors.secondaryLabel}>
                        {descartada.nota} →{' '}
                      </Text>
                    ) : null}
                    <Text variant="bodyStrong" color={colors.greenText}>
                      {segura.nota}
                    </Text>
                  </View>
                }
              />
            </GroupedList>
            <PrimaryButton title="Iniciar trajeto" onPress={iniciar} style={styles.botao} />
          </>
        )}
      </Sheet>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.groupedBg,
  },
  topo: {
    position: 'absolute',
    left: 16,
    right: 16,
  },
  pill: {
    marginTop: 12,
  },
  lista: {
    marginHorizontal: 16,
  },
  valor: {
    flexDirection: 'row',
    alignItems: 'baseline',
  },
  diff: {
    marginLeft: 8,
    minWidth: 28,
    textAlign: 'right',
  },
  botao: {
    marginHorizontal: 16,
    marginTop: 16,
  },
  nota: {
    paddingHorizontal: 16,
  },
  encerrar: {
    alignSelf: 'center',
    marginTop: 8,
  },
});
