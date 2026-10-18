package br.edu.ifal.arqsw.video.filtros;

import br.edu.ifal.arqsw.video.pipeline.Filtro;
import br.edu.ifal.arqsw.video.pipeline.Tarefa;

import java.util.Optional;

/**
 * Transformador: gera a versao do video na resolucao-alvo (a chamada ao ffmpeg e' omitida).
 * Um filtro por resolucao, como os perfis de codificacao da Secao 6.4.7.
 */
public final class FiltroTranscodificacao implements Filtro {

    private final int resolucaoAlvo;

    public FiltroTranscodificacao(int resolucaoAlvo) {
        this.resolucaoAlvo = resolucaoAlvo;
    }

    @Override
    public Optional<Tarefa> processar(Tarefa t) {
        if (t.resolucao() < resolucaoAlvo) {
            return Optional.of(t); // nada a fazer: nao se aumenta a resolucao
        }
        return Optional.of(t.comArtefato(t.videoId() + "-" + resolucaoAlvo + "p.mp4"));
    }
}
