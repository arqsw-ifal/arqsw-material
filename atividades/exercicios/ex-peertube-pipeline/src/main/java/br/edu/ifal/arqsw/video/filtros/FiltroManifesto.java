package br.edu.ifal.arqsw.video.filtros;

import br.edu.ifal.arqsw.video.pipeline.Filtro;
import br.edu.ifal.arqsw.video.pipeline.Tarefa;

import java.util.Optional;

/** Transformador: gera o manifesto HLS a partir das versoes ja produzidas (Secao 6.4.8). */
public final class FiltroManifesto implements Filtro {

    @Override
    public Optional<Tarefa> processar(Tarefa t) {
        // TODO (regra 3): sem nenhuma versao transcodificada (artefato .mp4), nao ha o que
        //   listar: a tarefa segue sem manifesto.
        return Optional.of(t.comArtefato(t.videoId() + "-master.m3u8"));
    }
}
