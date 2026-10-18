package br.edu.ifal.arqsw.telemetria.transporte.http;

import br.edu.ifal.arqsw.telemetria.regras.Leitura;
import br.edu.ifal.arqsw.telemetria.regras.ParserDeTelemetria;
import br.edu.ifal.arqsw.telemetria.regras.Regra;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter do transporte HTTP: converte o corpo da requisicao numa Leitura e a entrega
 * a cadeia de regras. Depende de "regras", nunca o contrario.
 */
public final class AdaptadorHttp {

    private final Regra cadeia;

    public AdaptadorHttp(Regra cadeia) {
        this.cadeia = cadeia;
    }

    /** Corpo no formato "sensor-7;temperatura;31.5;1760000000". Devolve os eventos gerados. */
    public List<String> receber(String corpo) {
        Leitura leitura = ParserDeTelemetria.deCorpoHttp(corpo);
        List<String> eventos = new ArrayList<>();
        cadeia.tratar(leitura, eventos);
        return eventos;
    }
}
