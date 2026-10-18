package br.edu.ifal.arqsw.video.filtros;

import br.edu.ifal.arqsw.video.pipeline.Filtro;
import br.edu.ifal.arqsw.video.pipeline.Tarefa;

import java.util.Optional;

/** Testador: so deixa passar videos que podem ser transcodificados. */
public final class FiltroValidacao implements Filtro {

    @Override
    public Optional<Tarefa> processar(Tarefa t) {
        // TODO (regra 2): aceitar so os formatos suportados: .mp4, .mov, .mkv e .webm
        //   (sem diferenciar maiusculas de minusculas).
        return t.resolucao() > 0 ? Optional.of(t) : Optional.empty();
    }
}
