package br.edu.ifal.arqsw.telemetria.regras;

import br.edu.ifal.arqsw.telemetria.regras.condicao.Condicao;
import br.edu.ifal.arqsw.telemetria.transporte.mqtt.PublicadorDeAlarmesMqtt;

import java.util.List;

/**
 * Acao: gera um evento "ALARME <dispositivo> <grandeza>=<valor>" quando a condicao
 * (uma Strategy) e' satisfeita, e deixa a leitura seguir.
 *
 * CODIGO LEGADO: alem de registrar o evento, publica o alarme direto no broker MQTT,
 * o que amarra a regra a um transporte. Leia o README.
 */
public final class Alarme extends Regra {

    private final Condicao condicao;

    public Alarme(Condicao condicao) {
        this.condicao = condicao;
    }

    @Override
    protected boolean processar(Leitura leitura, List<String> eventos) {
        if (condicao.satisfeitaPor(leitura.valor())) {
            String evento = "ALARME " + leitura.dispositivo() + " " + leitura.grandeza() + "=" + leitura.valor();
            eventos.add(evento);
            PublicadorDeAlarmesMqtt.publicar("alarmes/" + leitura.dispositivo(), evento);
        }
        return true;
    }
}
