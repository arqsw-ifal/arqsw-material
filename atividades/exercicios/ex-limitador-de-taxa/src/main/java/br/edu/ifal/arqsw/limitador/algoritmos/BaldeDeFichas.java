package br.edu.ifal.arqsw.limitador.algoritmos;

import br.edu.ifal.arqsw.limitador.api.Decisao;
import br.edu.ifal.arqsw.limitador.api.LimitadorDeTaxa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Balde de fichas (token bucket, Listagem F.7): capacidade maxima de fichas e reposicao
 * continua a uma taxa fixa, calculada sob demanda a partir do relogio injetado.
 */
public final class BaldeDeFichas implements LimitadorDeTaxa {

    private static final class Balde {
        double fichas;
        long ultimaRecargaMs;

        Balde(double fichas, long agora) {
            this.fichas = fichas;
            this.ultimaRecargaMs = agora;
        }
    }

    private final long capacidade;
    private final double fichasPorMs;
    private final LongSupplier relogioMs;
    private final Map<String, Balde> baldes = new ConcurrentHashMap<>();

    public BaldeDeFichas(long capacidade, double fichasPorSegundo, LongSupplier relogioMs) {
        this.capacidade = capacidade;
        this.fichasPorMs = fichasPorSegundo / 1000.0;
        this.relogioMs = relogioMs;
    }

    @Override
    public Decisao avaliar(String chave) {
        long agora = relogioMs.getAsLong();
        Balde balde = baldes.computeIfAbsent(chave, k -> new Balde(capacidade, agora));
        synchronized (balde) {
            double repostas = (agora - balde.ultimaRecargaMs) * fichasPorMs;
            balde.fichas = Math.min(capacidade, balde.fichas + repostas);
            balde.ultimaRecargaMs = agora;
            if (balde.fichas >= 1.0) {
                balde.fichas -= 1.0;
                return Decisao.permitir((long) balde.fichas);
            }
            // TODO (comportamento): informar quanto tempo falta para a proxima ficha,
            //      isto e', (1 - fichas) / fichasPorMs, arredondado para cima.
            return Decisao.negar(0);
        }
    }

    /** Usado pelo filtro HTTP legado para montar cabecalhos. */
    public long capacidade() {
        return capacidade;
    }
}
