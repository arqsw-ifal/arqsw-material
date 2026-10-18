package br.edu.ifal.arqsw.telemetria.transporte.mqtt;

import java.util.ArrayList;
import java.util.List;

/** Simula a publicacao de mensagens num broker MQTT (aqui, apenas guarda em memoria). */
public final class PublicadorDeAlarmesMqtt {

    private static final List<String> PUBLICADAS = new ArrayList<>();

    private PublicadorDeAlarmesMqtt() { }

    public static void publicar(String topico, String mensagem) {
        PUBLICADAS.add(topico + " " + mensagem);
    }

    public static List<String> publicadas() {
        return List.copyOf(PUBLICADAS);
    }
}
