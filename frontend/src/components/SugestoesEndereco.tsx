import { MapPin } from 'lucide-react-native';
import { StyleSheet, View } from 'react-native';

import type { Lugar } from '../api/lugares';
import { useSugestoes } from '../hooks/useSugestoes';
import { colors, spacing } from '../theme/tokens';
import { GroupedList } from './GroupedList';
import { Row } from './Row';
import { Text } from './Text';

export const MENSAGEM_FORA_DE_SP = 'Só aceitamos endereços na cidade de São Paulo.';

type Props = {
  /** Texto digitado no campo em edição. */
  texto: string;
  ativo: boolean;
  onEscolher: (lugar: Lugar) => void;
};

/** Lista de endereços sugeridos enquanto o usuário digita (só cidade de São Paulo). */
export function SugestoesEndereco({ texto, ativo, onEscolher }: Props) {
  const { visivel, lugares, foraDeSaoPaulo, carregando, desatualizado, erro } = useSugestoes(texto, ativo);
  if (!visivel) return null;

  if (lugares.length > 0) {
    return (
      <GroupedList style={styles.lista} separatorInset={48}>
        {lugares.map((l) => (
          <Row
            key={l.id}
            title={l.titulo}
            subtitle={l.subtitulo}
            left={<MapPin size={20} color={colors.tertiary} strokeWidth={2} />}
            onPress={() => onEscolher(l)}
          />
        ))}
      </GroupedList>
    );
  }

  // Sem lista: espera a resposta do texto atual antes de dizer que não achou.
  const mensagem = carregando || desatualizado
    ? 'Buscando endereços…'
    : erro
      ? 'Não foi possível buscar endereços agora.'
      : foraDeSaoPaulo
        ? MENSAGEM_FORA_DE_SP
        : 'Nenhum endereço encontrado em São Paulo.';
  const aviso = !carregando && !desatualizado && foraDeSaoPaulo;

  return (
    <View style={styles.mensagem} accessibilityLiveRegion="polite">
      <Text variant="secondary" color={aviso ? colors.redText : colors.secondaryLabel}>
        {mensagem}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  lista: {
    marginTop: 8,
  },
  mensagem: {
    marginTop: 8,
    paddingHorizontal: spacing.rowPadding,
    paddingVertical: 10,
  },
});
