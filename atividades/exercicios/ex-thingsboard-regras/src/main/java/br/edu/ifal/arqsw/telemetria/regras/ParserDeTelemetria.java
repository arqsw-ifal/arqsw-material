package br.edu.ifal.arqsw.telemetria.regras;

import java.util.ArrayList;
import java.util.List;

/**
 * CODIGO LEGADO: a traducao dos FORMATOS DE TRANSPORTE (corpo HTTP, carga util MQTT) mora
 * no pacote das regras, que assim passa a conhecer os protocolos. Leia o README.
 */
public final class ParserDeTelemetria {

    private ParserDeTelemetria() { }

    /** Corpo HTTP no formato "sensor-7;temperatura;31.5;1760000000". */
    public static Leitura deCorpoHttp(String corpo) {
        // TODO (regra 3): corpo malformado deve gerar IllegalArgumentException
        String[] p = corpo.split(";");
        return new Leitura(p[0], p[1], Double.parseDouble(p[2]), Long.parseLong(p[3]));
    }

    /** Carga util MQTT no formato {"temperatura":31.5,"umidade":40}. */
    public static List<Leitura> deCargaMqtt(String dispositivo, String carga, long instante) {
        List<Leitura> leituras = new ArrayList<>();
        String[] pares = carga.replace("{", "").replace("}", "").split(",");
        // TODO (regra 4): converter TODOS os pares, e nao so o primeiro
        String[] kv = pares[0].split(":");
        leituras.add(new Leitura(dispositivo, kv[0].trim().replace("\"", ""),
                Double.parseDouble(kv[1].trim()), instante));
        return leituras;
    }
}
