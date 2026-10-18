package br.edu.ifal.arqsw.hub.conexoes;

import br.edu.ifal.arqsw.hub.hub.Assinante;
import br.edu.ifal.arqsw.hub.hub.Evento;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Uma conexao WebSocket de um cliente (navegador ou aplicativo). No Mattermost, a WebConn
 * tem um canal de envio; aqui, os eventos recebidos ficam numa lista.
 */
public final class ConexaoWebSocket implements Assinante {

    private final String usuarioId;
    private final Set<String> canaisSilenciados = new HashSet<>();
    private final List<Evento> recebidos = new ArrayList<>();

    public ConexaoWebSocket(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    /** O usuario silenciou o canal: eventos desse canal nao devem chegar a esta conexao. */
    public void silenciarCanal(String canalId) {
        canaisSilenciados.add(canalId);
    }

    @Override
    public String usuarioId() {
        return usuarioId;
    }

    @Override
    public boolean deveReceber(Evento evento) {
        return evento.canalId() == null || !canaisSilenciados.contains(evento.canalId());
    }

    @Override
    public void notificar(Evento evento) {
        recebidos.add(evento);
    }

    public List<Evento> recebidos() {
        return List.copyOf(recebidos);
    }
}
