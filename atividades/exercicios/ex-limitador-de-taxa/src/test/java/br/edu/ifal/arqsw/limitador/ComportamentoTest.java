package br.edu.ifal.arqsw.limitador;

import br.edu.ifal.arqsw.limitador.algoritmos.BaldeDeFichas;
import br.edu.ifal.arqsw.limitador.algoritmos.JanelaDeslizante;
import br.edu.ifal.arqsw.limitador.api.Decisao;
import br.edu.ifal.arqsw.limitador.api.LimitadorDeTaxa;
import br.edu.ifal.arqsw.limitador.http.FiltroDeLimite;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de COMPORTAMENTO dos limitadores (Listagem F.9). O relogio e' falso e controlado
 * pelo teste, o que torna os resultados deterministicos.
 */
class ComportamentoTest {

    private final AtomicLong relogio = new AtomicLong(0);

    private int permitidas(LimitadorDeTaxa l, String chave, int tentativas) {
        int ok = 0;
        for (int i = 0; i < tentativas; i++) {
            if (l.avaliar(chave).permitida()) ok++;
        }
        return ok;
    }

    private static FiltroDeLimite.Resposta ok(String chave) {
        return new FiltroDeLimite.Resposta(200, Map.of());
    }

    @Test
    @DisplayName("balde de fichas aceita uma rajada até a capacidade")
    void baldeRajada() {
        LimitadorDeTaxa balde = new BaldeDeFichas(4, 2.0, relogio::get);   // Xu, Figura 4-4
        assertEquals(4, permitidas(balde, "c1", 10));
    }

    @Test
    @DisplayName("balde de fichas repõe fichas à taxa configurada")
    void baldeReposicao() {
        LimitadorDeTaxa balde = new BaldeDeFichas(4, 2.0, relogio::get);
        permitidas(balde, "c1", 10);
        relogio.addAndGet(1_000);                                          // 1 s depois: +2 fichas
        assertEquals(2, permitidas(balde, "c1", 10));
    }

    @Test
    @DisplayName("balde de fichas informa quanto esperar pela próxima ficha")
    void baldeEsperar() {
        LimitadorDeTaxa balde = new BaldeDeFichas(4, 2.0, relogio::get);
        permitidas(balde, "c1", 4);
        Decisao negada = balde.avaliar("c1");
        assertFalse(negada.permitida());
        assertEquals(500, negada.esperarMs());                             // 2 fichas/s: 0,5 s
    }

    @Test
    @DisplayName("chaves distintas têm limites independentes")
    void chavesIndependentes() {
        LimitadorDeTaxa balde = new BaldeDeFichas(2, 1.0, relogio::get);
        assertEquals(2, permitidas(balde, "ana", 5));
        assertEquals(2, permitidas(balde, "bruno", 5));
    }

    @Test
    @DisplayName("janela deslizante atenua a rajada na fronteira entre janelas")
    void deslizanteFronteira() {
        LimitadorDeTaxa l = new JanelaDeslizante(5, 60_000, relogio::get);
        relogio.set(59_000);
        assertEquals(5, permitidas(l, "c1", 5));
        relogio.set(60_500);                                               // a anterior ainda pesa ~99%
        assertEquals(1, permitidas(l, "c1", 5));                           // janela fixa deixaria 5
    }

    @Test
    @DisplayName("janela deslizante reproduz o exemplo numérico de Xu (limite 7: 3 + 5 x 0,7)")
    void deslizanteExemploDeXu() {
        LimitadorDeTaxa l = new JanelaDeslizante(7, 60_000, relogio::get);
        relogio.set(30_000);
        assertEquals(5, permitidas(l, "c1", 5));
        relogio.set(75_000);
        assertEquals(3, permitidas(l, "c1", 3));
        relogio.set(78_000);                                               // 30% da janela atual
        assertTrue(l.avaliar("c1").permitida());                           // 3 + 3,5 = 6,5 -> 6 < 7
        assertFalse(l.avaliar("c1").permitida());                          // 4 + 3,5 = 7,5 -> 7
    }

    @Test
    @DisplayName("janela deslizante libera o limite inteiro após duas janelas sem tráfego")
    void deslizanteRecupera() {
        LimitadorDeTaxa l = new JanelaDeslizante(5, 60_000, relogio::get);
        assertEquals(5, permitidas(l, "c1", 8));
        relogio.set(130_000);
        assertEquals(5, permitidas(l, "c1", 8));
    }

    @Test
    @DisplayName("o filtro repassa à API a requisição permitida")
    void filtroRepassa() {
        var filtro = new FiltroDeLimite(new BaldeDeFichas(1, 1.0, relogio::get), ComportamentoTest::ok);
        assertEquals(200, filtro.tratar("ip-10.0.0.1").status());
    }

    @Test
    @DisplayName("o filtro devolve 429 com o cabeçalho Retry-After em segundos")
    void filtro429ComRetryAfter() {
        var filtro = new FiltroDeLimite(new BaldeDeFichas(1, 1.0, relogio::get), ComportamentoTest::ok);
        filtro.tratar("ip-10.0.0.1");
        FiltroDeLimite.Resposta r = filtro.tratar("ip-10.0.0.1");
        assertEquals(429, r.status());
        assertEquals("1", r.cabecalhos().get("Retry-After"));
    }

    @Test
    @DisplayName("o filtro funciona com qualquer estratégia (janela deslizante)")
    void filtroComQualquerEstrategia() {
        var filtro = new FiltroDeLimite(new JanelaDeslizante(1, 1_000, relogio::get), ComportamentoTest::ok);
        assertEquals(200, filtro.tratar("ip-10.0.0.1").status());
        FiltroDeLimite.Resposta r = filtro.tratar("ip-10.0.0.1");
        assertEquals(429, r.status());
        assertEquals("1", r.cabecalhos().get("Retry-After"));
    }
}
