package br.edu.ifal.arqsw.hub.conexoes;

import br.edu.ifal.arqsw.hub.hub.Assinante;
import br.edu.ifal.arqsw.hub.hub.Evento;

import java.util.ArrayList;
import java.util.List;

/**
 * Outro tipo de assinante: envia notificacoes push para o celular do usuario. So se
 * interessa por mensagens novas (tipo "posted").
 */
public final class NotificadorMovel implements Assinante {

    private final String usuarioId;
    private final List<String> notificacoes = new ArrayList<>();

    public NotificadorMovel(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    @Override
    public String usuarioId() {
        return usuarioId;
    }

    @Override
    public boolean deveReceber(Evento evento) {
        return "posted".equals(evento.tipo());
    }

    @Override
    public void notificar(Evento evento) {
        notificacoes.add(evento.dados());
    }

    public List<String> notificacoes() {
        return List.copyOf(notificacoes);
    }
}
