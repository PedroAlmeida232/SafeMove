import { router, useLocalSearchParams } from 'expo-router';
import { Flag, Info, TriangleAlert } from 'lucide-react-native';
import { useMemo, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PrimaryButton, TextButton } from '../../src/components/Buttons';
import { CompactRouteBar } from '../../src/components/CompactRouteBar';
import { GroupedList } from '../../src/components/GroupedList';
import { IconTile } from '../../src/components/IconTile';
import { MapPin } from '../../src/components/map/MapPin';
import { RiskCircle } from '../../src/components/map/RiskCircle';
import { RouteLine } from '../../src/components/map/RouteLine';
import { RouteMap } from '../../src/components/map/RouteMap';
import { UserDot } from '../../src/components/map/UserDot';
import { Row } from '../../src/components/Row';
import { RotaIndisponivel } from '../../src/components/RotaIndisponivel';
import { Sheet } from '../../src/components/Sheet';
import { SheetHeader } from '../../src/components/SheetHeader';
import { StatBlock, StatRow } from '../../src/components/StatBlock';
import { Text } from '../../src/components/Text';
import { usePontosDeRisco, useRotaPorIndice } from '../../src/hooks/useRotaSelecionada';
import { useTopoFlutuante } from '../../src/hooks/useMapaBottom';
import { melhorRota, useRotas } from '../../src/state/RotaStore';
import { colors, radii } from '../../src/theme/tokens';
import { segundosParaTexto } from '../../src/utils/format';
import { corTextoClassificacao } from '../../src/utils/incidente';

export default function AlertaRotaScreen() {
  const { indice } = useLocalSearchParams<{ indice: string }>();
  const insets = useSafeAreaInsets();
  const topo = useTopoFlutuante();
  const { planejamento } = useRotas();
  const rota = useRotaPorIndice(indice);
  const riscos = usePontosDeRisco(rota);
  const [detalhes, setDetalhes] = useState(false);

  const padding = useMemo(
    () => ({ top: topo + 72, right: 48, bottom: 420, left: 48 }),
    [topo],
  );

  if (!planejamento || !rota) return <RotaIndisponivel />;

  const melhor = melhorRota(planejamento.rotas);
  const temAlternativa = melhor !== undefined && melhor.indice !== rota.indice && melhor.nota > rota.nota;
  const titulo = rota.resumo.assaltosRecentes > 0 ? 'Rota com risco de assalto' : 'Rota com risco';
  const subtitulo = rota.alertas[0] ?? 'Há ocorrências recentes no trajeto.';
  const inicio = rota.pontos[0];
  const fim = rota.pontos[rota.pontos.length - 1];

  return (
    <View style={styles.container}>
      <RouteMap pontos={rota.pontos} padding={padding}>
        {riscos.map((r) => (
          <RiskCircle key={r.id} center={r.centro} radius={r.raio} color={r.cor} />
        ))}
        <RouteLine coordinates={rota.pontos} variant="atual" />
        {inicio ? <UserDot coordinate={inicio} /> : null}
        {fim ? (
          <MapPin
            coordinate={fim}
            color={colors.accent}
            icon={Flag}
            accessibilityLabel={`Destino: ${planejamento.destinoLabel}`}
          />
        ) : null}
      </RouteMap>

      <View style={[styles.topo, { top: topo }]}>
        <CompactRouteBar
          origem={planejamento.origemLabel}
          destino={planejamento.destinoLabel}
          tempo={segundosParaTexto(rota.duracaoSegundos)}
          onBack={() => router.back()}
        />
      </View>

      <Sheet bottomPadding={insets.bottom + 12}>
        <SheetHeader
          tile={<IconTile icon={TriangleAlert} color={colors.red} size="large" />}
          title={titulo}
          subtitle={subtitulo}
        />
        <StatRow>
          <StatBlock value={String(rota.resumo.ocorrenciasRecentes)} label="ocorrências recentes" />
          <StatBlock
            value={String(rota.nota)}
            suffix="/100"
            label="nota da rota"
            color={corTextoClassificacao(rota.classificacao)}
          />
          <StatBlock
            value={String(rota.resumo.incidentesGraves + rota.resumo.incidentesLeves)}
            label="incidentes no trânsito"
          />
        </StatRow>

        <View style={styles.acoes}>
          {temAlternativa ? (
            <PrimaryButton
              title="Traçar rota alternativa"
              onPress={() =>
                router.push({
                  pathname: '/rota/alternativa',
                  params: { segura: String(melhor.indice), descartada: String(rota.indice) },
                })
              }
            />
          ) : (
            <View style={styles.aviso} accessible accessibilityLiveRegion="polite">
              <Info size={18} color={colors.secondaryLabel} strokeWidth={2} />
              <Text variant="secondary" color={colors.secondaryLabel} style={styles.avisoTexto}>
                Esta já é a opção mais segura no momento
              </Text>
            </View>
          )}
          <TextButton
            title={detalhes ? 'Ocultar detalhes' : 'Ver detalhes'}
            onPress={() => setDetalhes((d) => !d)}
            style={styles.detalhesBotao}
          />
        </View>

        {detalhes ? (
          <GroupedList style={styles.lista} separatorInset={56}>
            {rota.alertas.length > 0 ? (
              rota.alertas.map((a, i) => (
                <Row key={i} title={a} left={<IconTile icon={TriangleAlert} color={colors.orange} />} />
              ))
            ) : (
              <Row title="Nenhum alerta específico para este trajeto." />
            )}
          </GroupedList>
        ) : null}
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
  acoes: {
    paddingHorizontal: 16,
    marginTop: 16,
  },
  aviso: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: colors.groupedBg,
    borderRadius: radii.button,
    minHeight: 50,
    paddingHorizontal: 14,
  },
  avisoTexto: {
    flex: 1,
  },
  detalhesBotao: {
    alignSelf: 'center',
    marginTop: 4,
  },
  lista: {
    marginHorizontal: 16,
    marginTop: 4,
  },
});
