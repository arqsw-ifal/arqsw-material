package br.edu.ifal.arqsw.video;

import br.edu.ifal.arqsw.video.pipeline.Pipeline;
import br.edu.ifal.arqsw.video.pipeline.Tarefa;

/** Ponto de entrada. Depois da refatoracao, deve ser a raiz de composicao do pipeline. */
public class App {
    public static void main(String[] args) {
        var pipeline = Pipeline.padrao();
        System.out.println(pipeline.executar(new Tarefa("v1", "/uploads/v1.mp4", 1080)));
        System.out.println(pipeline.executar(new Tarefa("v2", "/uploads/v2.avi", 720)));
    }
}
