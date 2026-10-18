package br.edu.ifal.arqsw.video.pipeline;

import java.util.Optional;

/** Contrato unico entre os filtros (o "requisito derivado" da Secao 4.4.3). */
public interface Filtro {

    /** {@code Optional.empty()} significa que um filtro testador descartou a tarefa. */
    Optional<Tarefa> processar(Tarefa entrada);
}
