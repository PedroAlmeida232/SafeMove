import { router } from 'expo-router';
import { Camera, MapPin, X } from 'lucide-react-native';
import { useState, type ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

import { mensagemDeErro } from '../../api/client';
import { useConfirmarOcorrencia } from '../../api/hooks';
import type { Incidente, Ocorrencia, Radar } from '../../api/types';
import { useAuth } from '../../auth/AuthProvider';
import { colors } from '../../theme/tokens';
import { haQuantoTempo, segundosParaMinutos } from '../../utils/format';
import type { Coordenada } from '../../utils/geo';
import { corIncidente, rotuloCategoria } from '../../utils/incidente';
import { ICONE_INCIDENTE, META_OCORRENCIA } from '../../utils/ocorrencia';
import { PrimaryButton } from '../Buttons';
import { IconTile } from '../IconTile';
import { Touchable } from '../Pressable';
import { Text } from '../Text';

export type Selecao =
  | { tipo: 'ocorrencia'; item: Ocorrencia }
  | { tipo: 'incidente'; item: Incidente }
  | { tipo: 'radar'; item: Radar }
  | { tipo: 'lugar'; titulo: string; subtitulo: string; coordenada: Coordenada };

type Props = {
  selecao: Selecao;
  onClose: () => void;
};

function Cabecalho({
  tile,
  titulo,
  subtitulo,
  onClose,
}: {
  tile: ReactNode;
  titulo: string;
  subtitulo: string;
  onClose: () => void;
}) {
  return (
    <View style={styles.header}>
      {tile}
      <View style={styles.headerTexts}>
        <Text variant="bodyStrong" numberOfLines={2}>
          {titulo}
        </Text>
        <Text variant="caption" color={colors.secondaryLabel} numberOfLines={2}>
          {subtitulo}
        </Text>
      </View>
      <Touchable onPress={onClose} style={styles.close} accessibilityRole="button" accessibilityLabel="Fechar">
        <View style={styles.closeCircle}>
          <X size={15} color={colors.secondaryLabel} strokeWidth={2.4} />
        </View>
      </Touchable>
    </View>
  );
}

function DetalheOcorrencia({ item, onClose }: { item: Ocorrencia; onClose: () => void }) {
  const meta = META_OCORRENCIA[item.tipo];
  const { usuario } = useAuth();
  const confirmar = useConfirmarOcorrencia();
  const [confirmadas, setConfirmadas] = useState(item.confirmacoes);
  const [feito, setFeito] = useState(false);

  const euTambemVi = () => {
    if (!usuario) {
      router.push({ pathname: '/login', params: { next: '/' } });
      return;
    }
    confirmar.mutate(item.id, {
      onSuccess: (o) => {
        setConfirmadas(o.confirmacoes);
        setFeito(true);
      },
    });
  };

  const confirmacoesTexto =
    confirmadas === 0 ? 'Nenhuma confirmação' : confirmadas === 1 ? '1 confirmação' : `${confirmadas} confirmações`;

  return (
    <View>
      <Cabecalho
        tile={<IconTile icon={meta.icone} color={meta.cor} size="large" />}
        titulo={meta.rotulo}
        subtitulo={`${haQuantoTempo(item.criadoEm)} · ${confirmacoesTexto}`}
        onClose={onClose}
      />
      {item.descricao ? (
        <Text variant="body" style={styles.descricao}>
          {item.descricao}
        </Text>
      ) : null}
      {confirmar.error ? (
        <Text variant="caption" color={colors.redText} style={styles.feedback} accessibilityLiveRegion="polite">
          {mensagemDeErro(confirmar.error)}
        </Text>
      ) : null}
      {feito ? (
        <Text variant="captionStrong" color={colors.greenText} style={styles.feedback} accessibilityLiveRegion="polite">
          Obrigado! Sua confirmação ajuda quem passa por aqui.
        </Text>
      ) : (
        <PrimaryButton
          title="Eu também vi"
          onPress={euTambemVi}
          loading={confirmar.isPending}
          style={styles.button}
          accessibilityHint={usuario ? undefined : 'Você precisará entrar na sua conta'}
        />
      )}
    </View>
  );
}

function DetalheIncidente({ item, onClose }: { item: Incidente; onClose: () => void }) {
  const partes = [haQuantoTempo(item.atualizadoEm)];
  if (item.atrasoSegundos && item.atrasoSegundos > 0) {
    partes.push(`+${segundosParaMinutos(item.atrasoSegundos)} min de atraso`);
  }
  return (
    <View>
      <Cabecalho
        tile={<IconTile icon={ICONE_INCIDENTE[item.categoria]} color={corIncidente(item)} size="large" />}
        titulo={item.via ? `${rotuloCategoria(item.categoria)} · ${item.via}` : rotuloCategoria(item.categoria)}
        subtitulo={partes.join(' · ')}
        onClose={onClose}
      />
      {item.descricao ? (
        <Text variant="body" style={styles.descricao}>
          {item.descricao}
        </Text>
      ) : null}
    </View>
  );
}

function DetalheRadar({ item, onClose }: { item: Radar; onClose: () => void }) {
  return (
    <Cabecalho
      tile={<IconTile icon={Camera} color={colors.blue} size="large" />}
      titulo="Radar"
      subtitulo={item.velocidadeMax ? `Velocidade máxima ${item.velocidadeMax} km/h` : 'Velocidade máxima não informada'}
      onClose={onClose}
    />
  );
}

function DetalheLugar({
  titulo,
  subtitulo,
  coordenada,
  onClose,
}: {
  titulo: string;
  subtitulo: string;
  coordenada: Coordenada;
  onClose: () => void;
}) {
  return (
    <View>
      <Cabecalho
        tile={<IconTile icon={MapPin} color={colors.accent} size="large" />}
        titulo={titulo}
        subtitulo={subtitulo}
        onClose={onClose}
      />
      <PrimaryButton
        title="Planejar rota até aqui"
        style={styles.button}
        onPress={() =>
          router.navigate({
            pathname: '/rotas',
            params: {
              destLat: String(coordenada.latitude),
              destLng: String(coordenada.longitude),
              destLabel: titulo,
            },
          })
        }
      />
    </View>
  );
}

/** Card exibido na sheet ao tocar num pino. */
export function PinDetail({ selecao, onClose }: Props) {
  switch (selecao.tipo) {
    case 'ocorrencia':
      return <DetalheOcorrencia key={selecao.item.id} item={selecao.item} onClose={onClose} />;
    case 'incidente':
      return <DetalheIncidente item={selecao.item} onClose={onClose} />;
    case 'radar':
      return <DetalheRadar item={selecao.item} onClose={onClose} />;
    case 'lugar':
      return (
        <DetalheLugar
          titulo={selecao.titulo}
          subtitulo={selecao.subtitulo}
          coordenada={selecao.coordenada}
          onClose={onClose}
        />
      );
  }
}

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: 16,
    paddingRight: 6,
    gap: 12,
  },
  headerTexts: {
    flex: 1,
  },
  close: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeCircle: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: colors.groupedBg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  descricao: {
    paddingHorizontal: 16,
    marginTop: 12,
  },
  feedback: {
    paddingHorizontal: 16,
    marginTop: 12,
  },
  button: {
    marginHorizontal: 16,
    marginTop: 16,
  },
});
