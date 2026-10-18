package br.edu.ifal.arqsw.telemetria;

import br.edu.ifal.arqsw.telemetria.regras.Alarme;
import br.edu.ifal.arqsw.telemetria.regras.Regra;
import br.edu.ifal.arqsw.telemetria.regras.SomenteGrandeza;
import br.edu.ifal.arqsw.telemetria.regras.condicao.AcimaDe;
import br.edu.ifal.arqsw.telemetria.transporte.http.AdaptadorHttp;
import br.edu.ifal.arqsw.telemetria.transporte.mqtt.AdaptadorMqtt;

/** Raiz de composicao: monta a cadeia de regras e a entrega aos dois transportes. */
public class App {
    public static void main(String[] args) {
        Regra cadeia = new SomenteGrandeza("temperatura");
        cadeia.entaoAplicar(new Alarme(new AcimaDe(30)));

        var http = new AdaptadorHttp(cadeia);
        var mqtt = new AdaptadorMqtt(cadeia, () -> 1760000000L);
        System.out.println("HTTP: " + http.receber("sensor-7;temperatura;31.5;1760000000"));
        System.out.println("MQTT: " + mqtt.aoPublicar("dispositivos/sensor-9/telemetria",
                "{\"umidade\":80,\"temperatura\":35.0}"));
    }
}
