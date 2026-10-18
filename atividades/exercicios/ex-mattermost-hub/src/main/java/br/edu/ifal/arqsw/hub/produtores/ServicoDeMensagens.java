package br.edu.ifal.arqsw.hub.produtores;

import br.edu.ifal.arqsw.hub.conexoes.ConexaoWebSocket;
import br.edu.ifal.arqsw.hub.hub.Assinante;
import br.edu.ifal.arqsw.hub.hub.Evento;
import br.edu.ifal.arqsw.hub.hub.HubDeEventos;
import br.edu.ifal.arqsw.hub.hub.Publicador;

import java.util.Set;

/**
 * Produtor de eventos: publica mensagens num canal.
 *
 * CODIGO LEGADO: recebe um Publicador, mas o converte para o hub concreto e entrega o evento
 * ele mesmo, so as conexoes WebSocket. Leia o README.
 */
public final class ServicoDeMensagens {

    private final Publicador publicador;

    public ServicoDeMensagens(Publicador publicador) {
        this.publicador = publicador;
    }

    public void publicarMensagem(String canalId, String autor, String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("mensagem vazia");
        }
        // 1. gravar no repositorio (omitido); 2. so entao anunciar
        Evento evento = new Evento("posted", canalId, null, autor + ": " + texto);

        // ATALHO: "para ganhar desempenho", entrega direto as conexoes, sem passar pelo hub.
        HubDeEventos hub = (HubDeEventos) publicador;
        Set<String> membros = hub.membrosDo(canalId);
        for (Assinante a : hub.assinantes()) {
            if (a instanceof ConexaoWebSocket conexao && membros.contains(conexao.usuarioId())) {
                conexao.notificar(evento);
            }
        }
    }
}
