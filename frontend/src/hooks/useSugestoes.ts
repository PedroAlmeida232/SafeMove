import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';

import { MIN_CARACTERES_BUSCA, buscarLugares } from '../api/lugares';

const ESPERA_MS = 350;
const CINCO_MINUTOS = 5 * 60_000;

/** Sugestões de endereço para o texto digitado. Só busca depois de uma pausa na digitação. */
export function useSugestoes(texto: string, ativo: boolean) {
  const termo = texto.trim();
  const [pausado, setPausado] = useState(termo);

  useEffect(() => {
    const t = setTimeout(() => setPausado(termo), ESPERA_MS);
    return () => clearTimeout(t);
  }, [termo]);

  const habilitado = ativo && termo.length >= MIN_CARACTERES_BUSCA;
  const consulta = useQuery({
    queryKey: ['lugares', pausado.toLowerCase()],
    queryFn: ({ signal }) => buscarLugares(pausado, signal),
    enabled: habilitado && pausado.length >= MIN_CARACTERES_BUSCA,
    staleTime: CINCO_MINUTOS,
    retry: false,
    placeholderData: (anterior) => anterior,
  });

  return {
    visivel: habilitado,
    lugares: consulta.data?.lugares ?? [],
    foraDeSaoPaulo: consulta.data?.foraDeSaoPaulo ?? false,
    // Entre a tecla e a resposta ainda vale a lista anterior: só "carregando" sem nada para mostrar.
    carregando: habilitado && (consulta.isPending || (consulta.isFetching && !consulta.data)),
    desatualizado: pausado !== termo || consulta.isPlaceholderData,
    erro: consulta.isError,
  };
}
