package br.edu.ifal.arqsw.limitador;

import br.edu.ifal.arqsw.limitador.algoritmos.BaldeDeFichas;
import br.edu.ifal.arqsw.limitador.http.FiltroDeLimite;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Simula seis requisicoes de um mesmo cliente, com um relogio controlado. */
public class App {
    public static void main(String[] args) {
        AtomicLong relogio = new AtomicLong(0);
        var filtro = new FiltroDeLimite(new BaldeDeFichas(4, 2.0, relogio::get),
                chave -> new FiltroDeLimite.Resposta(200, Map.of()));
        for (int i = 1; i <= 6; i++) {
            System.out.println("requisicao " + i + ": " + filtro.tratar("ip-10.0.0.1").status());
        }
    }
}
