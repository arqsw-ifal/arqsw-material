package br.edu.ifal.arqsw.video.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Mensagem que trafega nos canos: imutavel e pequena (referencia ao arquivo, nao o conteudo).
 * Listagem 6.4.16.
 */
public record Tarefa(String videoId, String caminho, int resolucao, List<String> artefatos) {

    public Tarefa {
        artefatos = List.copyOf(artefatos);
    }

    public Tarefa(String videoId, String caminho, int resolucao) {
        this(videoId, caminho, resolucao, List.of());
    }

    public Tarefa comArtefato(String artefato) {
        var novos = new ArrayList<>(artefatos);
        novos.add(artefato);
        return new Tarefa(videoId, caminho, resolucao, novos);
    }
}
