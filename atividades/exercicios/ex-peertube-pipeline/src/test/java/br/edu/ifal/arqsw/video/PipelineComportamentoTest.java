package br.edu.ifal.arqsw.video;

import br.edu.ifal.arqsw.video.filtros.FiltroManifesto;
import br.edu.ifal.arqsw.video.filtros.FiltroMiniatura;
import br.edu.ifal.arqsw.video.filtros.FiltroTranscodificacao;
import br.edu.ifal.arqsw.video.filtros.FiltroValidacao;
import br.edu.ifal.arqsw.video.pipeline.Filtro;
import br.edu.ifal.arqsw.video.pipeline.Pipeline;
import br.edu.ifal.arqsw.video.pipeline.Tarefa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de COMPORTAMENTO: o pipeline produz os artefatos certos, na ordem certa? */
class PipelineComportamentoTest {

    /** O pipeline usado na maioria dos testes, montado aqui (e nao pelo proprio Pipeline). */
    private static Pipeline padrao() {
        return new Pipeline(List.of(new FiltroValidacao(), new FiltroTranscodificacao(480),
                new FiltroTranscodificacao(720), new FiltroMiniatura(), new FiltroManifesto()));
    }

    @Test
    @DisplayName("video 1080p gera 480p, 720p, miniatura e manifesto, nessa ordem")
    void video1080p() {
        Tarefa saida = padrao().executar(new Tarefa("v1", "/up/v1.mp4", 1080)).orElseThrow();
        assertEquals(List.of("v1-480p.mp4", "v1-720p.mp4", "v1-miniatura.jpg", "v1-master.m3u8"),
                saida.artefatos());
    }

    @Test
    @DisplayName("video 360p nao e' transcodificado para resolucoes maiores")
    void video360pSemVersoes() {
        Tarefa saida = padrao().executar(new Tarefa("v2", "/up/v2.mp4", 360)).orElseThrow();
        assertTrue(saida.artefatos().stream().noneMatch(a -> a.endsWith(".mp4")), saida.artefatos().toString());
    }

    @Test
    @DisplayName("a tarefa original nao e' modificada (mensagem imutavel)")
    void tarefaImutavel() {
        Tarefa inicial = new Tarefa("v1", "/up/v1.mp4", 1080);
        padrao().executar(inicial);
        assertEquals(List.of(), inicial.artefatos());
    }

    @Test
    @DisplayName("pipeline sem filtros devolve a tarefa inalterada")
    void pipelineVazio() {
        Tarefa inicial = new Tarefa("v1", "/up/v1.mp4", 1080);
        assertEquals(Optional.of(inicial), new Pipeline(List.of()).executar(inicial));
    }

    @Test
    @DisplayName("a ordem dos filtros define a ordem dos artefatos")
    void ordemDosFiltros() {
        Pipeline invertido = new Pipeline(List.of(new FiltroMiniatura(), new FiltroTranscodificacao(720),
                new FiltroTranscodificacao(480)));
        Tarefa saida = invertido.executar(new Tarefa("v1", "/up/v1.mp4", 1080)).orElseThrow();
        assertEquals(List.of("v1-miniatura.jpg", "v1-720p.mp4", "v1-480p.mp4"), saida.artefatos());
    }

    @Test
    @DisplayName("video com resolucao zero e' descartado pelo filtro de validacao")
    void resolucaoZeroDescartada() {
        assertEquals(Optional.empty(), padrao().executar(new Tarefa("v3", "/up/v3.mp4", 0)));
    }

    @Test
    @DisplayName("video em formato nao suportado e' descartado")
    void formatoNaoSuportado() {
        assertEquals(Optional.empty(), padrao().executar(new Tarefa("v4", "/up/v4.avi", 720)));
        assertTrue(padrao().executar(new Tarefa("v5", "/up/v5.MKV", 720)).isPresent(),
                "a extensao nao deve diferenciar maiusculas de minusculas");
    }

    @Test
    @DisplayName("tarefa descartada nao chega aos filtros seguintes")
    void descarteInterrompeOFluxo() {
        AtomicInteger chamadas = new AtomicInteger();
        Filtro descarta = t -> Optional.empty();
        Filtro contador = t -> {
            chamadas.incrementAndGet();
            return Optional.of(t);
        };
        Optional<Tarefa> saida = new Pipeline(List.of(descarta, contador))
                .executar(new Tarefa("v1", "/up/v1.mp4", 1080));
        assertEquals(Optional.empty(), saida);
        assertEquals(0, chamadas.get(), "o filtro seguinte nao deveria ter sido chamado");
    }

    @Test
    @DisplayName("sem versoes transcodificadas nao ha manifesto")
    void semVersoesSemManifesto() {
        Tarefa saida = padrao().executar(new Tarefa("v2", "/up/v2.mp4", 360)).orElseThrow();
        assertEquals(List.of("v2-miniatura.jpg"), saida.artefatos());
    }
}
