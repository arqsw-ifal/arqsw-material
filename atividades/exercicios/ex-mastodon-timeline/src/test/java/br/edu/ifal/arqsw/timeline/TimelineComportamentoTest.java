package br.edu.ifal.arqsw.timeline;

import br.edu.ifal.arqsw.timeline.aplicacao.ServicoDeTimeline;
import br.edu.ifal.arqsw.timeline.banco.BancoDeDados;
import br.edu.ifal.arqsw.timeline.banco.GrafoSocialNoBanco;
import br.edu.ifal.arqsw.timeline.banco.PostsPorAutorNoBanco;
import br.edu.ifal.arqsw.timeline.cache.CacheEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de COMPORTAMENTO: a timeline mostra o que deve, na ordem certa? */
class TimelineComportamentoTest {

    private static final long LIMIAR = 3;
    // Ana: autora comum. Bia: leitora ativa. Caio: leitor inativo. Rita: celebridade.
    private static final long ANA = 1, BIA = 2, CAIO = 3, RITA = 10;

    private BancoDeDados banco;
    private CacheEmMemoria cache;
    private ServicoDeTimeline servico;

    @BeforeEach
    void preparar() {
        banco = new BancoDeDados();
        cache = new CacheEmMemoria(800);
        servico = new ServicoDeTimeline(cache, new GrafoSocialNoBanco(banco),
                new PostsPorAutorNoBanco(banco), LIMIAR);
        for (long u = 1; u <= 12; u++) {
            banco.cadastrarUsuario(u, u != CAIO);
        }
    }

    /** Grava a publicacao no "banco" e dispara a distribuicao, como o PostStatusService. */
    private void publicar(long autorId, long postId) {
        banco.registrarPost(autorId, postId);
        servico.aoPublicar(autorId, postId);
    }

    /** Torna Rita uma celebridade: Bia e mais dois usuarios a seguem. */
    private void ritaCelebridade() {
        banco.seguir(BIA, RITA);
        banco.seguir(4, RITA);
        banco.seguir(5, RITA);
    }

    @Test
    @DisplayName("o autor ve a propria publicacao na timeline")
    void autorVeAPropriaPublicacao() {
        publicar(ANA, 100);
        assertEquals(List.of(100L), servico.timeline(ANA, 10));
    }

    @Test
    @DisplayName("fanout on write: o seguidor ativo recebe a publicacao de um autor comum")
    void seguidorAtivoRecebe() {
        banco.seguir(BIA, ANA);
        publicar(ANA, 100);
        assertEquals(List.of(100L), cache.recentes(BIA, 10), "a publicacao deveria estar no cache da Bia");
        assertEquals(List.of(100L), servico.timeline(BIA, 10));
    }

    @Test
    @DisplayName("o seguidor inativo nao recebe fanout on write (so os ativos)")
    void seguidorInativoNaoRecebe() {
        banco.seguir(BIA, ANA);
        banco.seguir(CAIO, ANA);
        publicar(ANA, 100);
        assertEquals(List.of(), cache.recentes(CAIO, 10), "o cache do Caio (inativo) deveria ficar vazio");
    }

    @Test
    @DisplayName("a celebridade nao faz fanout on write: o cache dos seguidores nao recebe a publicacao")
    void celebridadeSemFanoutNaEscrita() {
        ritaCelebridade();
        publicar(RITA, 200);
        assertFalse(cache.recentes(BIA, 10).contains(200L), "o cache da Bia nao deveria conter a publicacao da Rita");
        assertTrue(cache.recentes(RITA, 10).contains(200L), "a Rita deveria ver a propria publicacao");
    }

    @Test
    @DisplayName("fanout on read: a timeline do seguidor inclui as publicacoes da celebridade")
    void timelineIncluiCelebridade() {
        ritaCelebridade();
        publicar(RITA, 200);
        assertEquals(List.of(200L), servico.timeline(BIA, 10));
    }

    @Test
    @DisplayName("a timeline mescla cache e celebridades, da mais recente para a mais antiga")
    void mesclaOrdenada() {
        banco.seguir(BIA, ANA);
        ritaCelebridade();
        publicar(ANA, 100);
        publicar(RITA, 150);
        publicar(ANA, 180);
        publicar(RITA, 200);
        assertEquals(List.of(200L, 180L, 150L, 100L), servico.timeline(BIA, 10));
    }

    @Test
    @DisplayName("a timeline vem do maior ID (mais recente) para o menor")
    void ordemDecrescente() {
        banco.seguir(BIA, ANA);
        publicar(ANA, 100);
        publicar(ANA, 300);
        publicar(ANA, 200);
        assertEquals(List.of(300L, 200L, 100L), servico.timeline(BIA, 10));
    }

    @Test
    @DisplayName("a timeline respeita o limite pedido")
    void respeitaLimite() {
        banco.seguir(BIA, ANA);
        for (long p = 101; p <= 105; p++) {
            publicar(ANA, p);
        }
        assertEquals(List.of(105L, 104L, 103L), servico.timeline(BIA, 3));
    }

    @Test
    @DisplayName("uma publicacao que esta no cache e tambem vem da leitura aparece uma vez so")
    void semRepeticoes() {
        banco.seguir(BIA, RITA);
        publicar(RITA, 150);       // Rita ainda e' autora comum: fanout on write para a Bia
        banco.seguir(4, RITA);
        banco.seguir(5, RITA);     // agora Rita e' celebridade: 150 tambem vem do fanout on read
        publicar(RITA, 200);
        assertEquals(List.of(200L, 150L), servico.timeline(BIA, 10));
    }
}
