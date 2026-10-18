package br.edu.ifal.arqsw.hub.hub;

/**
 * Evento imutavel. O "alcance" indica a quem ele interessa, como o WebsocketBroadcast do
 * Mattermost: um canal ({@code canalId}) ou um unico usuario ({@code usuarioId}).
 */
public record Evento(String tipo, String canalId, String usuarioId, String dados) {
}
