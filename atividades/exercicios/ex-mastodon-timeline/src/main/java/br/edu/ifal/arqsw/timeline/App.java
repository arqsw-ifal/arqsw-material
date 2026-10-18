package br.edu.ifal.arqsw.timeline;

import br.edu.ifal.arqsw.timeline.aplicacao.ServicoDeTimeline;
import br.edu.ifal.arqsw.timeline.banco.BancoDeDados;
import br.edu.ifal.arqsw.timeline.banco.GrafoSocialNoBanco;
import br.edu.ifal.arqsw.timeline.banco.PostsPorAutorNoBanco;
import br.edu.ifal.arqsw.timeline.cache.CacheEmMemoria;

/**
 * Raiz de composicao: o UNICO lugar que conhece todas as classes concretas e as liga
 * entre si. Simula Ana (autora comum), Bia (leitora) e Rita (celebridade).
 */
public class App {

    private static final long ANA = 1, BIA = 2, RITA = 10;

    public static void main(String[] args) {
        var banco = new BancoDeDados();
        var servico = new ServicoDeTimeline(new CacheEmMemoria(800),
                new GrafoSocialNoBanco(banco), new PostsPorAutorNoBanco(banco), 3);

        for (long u = 1; u <= 12; u++) {
            banco.cadastrarUsuario(u, true);
        }
        banco.seguir(BIA, ANA);
        banco.seguir(BIA, RITA);
        banco.seguir(4, RITA);
        banco.seguir(5, RITA);

        long[][] publicacoes = {{ANA, 100}, {RITA, 150}, {ANA, 180}, {RITA, 200}};
        for (long[] p : publicacoes) {
            banco.registrarPost(p[0], p[1]); // grava no "banco"...
            servico.aoPublicar(p[0], p[1]);  // ...e distribui (num sistema real, um job)
        }
        System.out.println("timeline da Bia: " + servico.timeline(BIA, 10));
    }
}
