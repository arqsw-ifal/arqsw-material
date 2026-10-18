package br.edu.ifal.arqsw.telemetria.transporte.mqtt;

import br.edu.ifal.arqsw.telemetria.regras.Leitura;
import br.edu.ifal.arqsw.telemetria.regras.ParserDeTelemetria;
import br.edu.ifal.arqsw.telemetria.regras.Regra;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Adapter do transporte MQTT (como MqttTransportAdaptor no ThingsBoard, Secao 6.7.5):
 * converte cada publicacao em leituras e as entrega a cadeia de regras.
 */
public final class AdaptadorMqtt {

    private static final String PREFIXO = "dispositivos/";
    private static final String SUFIXO = "/telemetria";

    private final Regra cadeia;
    private final LongSupplier relogio;

    public AdaptadorMqtt(Regra cadeia, LongSupplier relogio) {
        this.cadeia = cadeia;
        this.relogio = relogio;
    }

    /** Topico "dispositivos/<id>/telemetria"; carga {"temperatura":31.5,"umidade":40}. */
    public List<String> aoPublicar(String topico, String carga) {
        if (!topico.startsWith(PREFIXO) || !topico.endsWith(SUFIXO)
                || topico.length() <= PREFIXO.length() + SUFIXO.length()) {
            throw new IllegalArgumentException("topico invalido: " + topico);
        }
        String dispositivo = topico.substring(PREFIXO.length(), topico.length() - SUFIXO.length());
        List<String> eventos = new ArrayList<>();
        for (Leitura leitura : ParserDeTelemetria.deCargaMqtt(dispositivo, carga, relogio.getAsLong())) {
            cadeia.tratar(leitura, eventos);
        }
        return eventos;
    }
}
