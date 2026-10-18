package br.edu.ifal.arqsw.limitador.http;

import br.edu.ifal.arqsw.limitador.algoritmos.BaldeDeFichas;
import br.edu.ifal.arqsw.limitador.api.Decisao;
import br.edu.ifal.arqsw.limitador.api.LimitadorDeTaxa;

import java.util.Map;
import java.util.function.Function;

/** Middleware HTTP: aplica o limitador e traduz a decisao para HTTP (Listagem F.6). */
public final class FiltroDeLimite {

    public record Resposta(int status, Map<String, String> cabecalhos) { }

    private final BaldeDeFichas balde;                 // ATALHO: so funciona com o balde de fichas
    private final Function<String, Resposta> proximo;  // a API protegida

    public FiltroDeLimite(LimitadorDeTaxa limitador, Function<String, Resposta> proximo) {
        this.balde = (BaldeDeFichas) limitador;        // ATALHO: conversao para a classe concreta
        this.proximo = proximo;
    }

    public Resposta tratar(String chaveDoCliente) {
        Decisao d = balde.avaliar(chaveDoCliente);
        if (!d.permitida()) {
            // TODO (comportamento): incluir o cabecalho Retry-After, em segundos inteiros,
            //      arredondando esperarMs para cima (minimo de 1 segundo).
            return new Resposta(429, Map.of("X-RateLimit-Limit", Long.toString(balde.capacidade())));
        }
        return proximo.apply(chaveDoCliente);
    }
}
