package br.edu.ifal.arqsw.limitador.algoritmos;

import br.edu.ifal.arqsw.limitador.api.Decisao;
import br.edu.ifal.arqsw.limitador.api.LimitadorDeTaxa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Contador de janela deslizante (Xu, 2020, cap. 4; Listagem F.8):
 * estimativa = atual + anterior * (fracao da janela anterior ainda coberta).
 *
 * CODIGO LEGADO: hoje e' apenas um contador de janela FIXA, e le o relogio do sistema.
 */
public final class JanelaDeslizante implements LimitadorDeTaxa {

    private static final class Contadores {
        long janela = -1;
        long atual;
    }

    private final long limite;
    private final long janelaMs;
    private final LongSupplier relogioMs;
    private final Map<String, Contadores> porChave = new ConcurrentHashMap<>();

    public JanelaDeslizante(long limite, long janelaMs, LongSupplier relogioMs) {
        this.limite = limite;
        this.janelaMs = janelaMs;
        this.relogioMs = relogioMs;
    }

    @Override
    public Decisao avaliar(String chave) {
        // TODO (comportamento): usar o relogio injetado (relogioMs), e nao o do sistema.
        long agora = System.currentTimeMillis();
        long janelaAtual = agora / janelaMs;
        Contadores c = porChave.computeIfAbsent(chave, k -> new Contadores());
        synchronized (c) {
            if (janelaAtual != c.janela) {
                c.atual = 0;
                c.janela = janelaAtual;
            }
            // TODO (comportamento): ponderar o contador da janela anterior pela fracao da janela
            //      anterior que ainda se sobrepoe a janela movel; arredondar a estimativa para baixo.
            if (c.atual < limite) {
                c.atual++;
                return Decisao.permitir(limite - c.atual);
            }
            return Decisao.negar(janelaMs - (agora % janelaMs));
        }
    }
}
