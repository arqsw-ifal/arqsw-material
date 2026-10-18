package br.edu.ifal.arqsw.hub;

import br.edu.ifal.arqsw.hub.conexoes.ConexaoWebSocket;
import br.edu.ifal.arqsw.hub.conexoes.NotificadorMovel;
import br.edu.ifal.arqsw.hub.hub.HubDeEventos;
import br.edu.ifal.arqsw.hub.produtores.ServicoDeMensagens;

/**
 * Raiz de composicao: o UNICO lugar que conhece o hub concreto, os produtores e os
 * assinantes concretos, e os liga entre si.
 */
public class App {
    public static void main(String[] args) {
        var hub = new HubDeEventos();
        var navegadorDaBia = new ConexaoWebSocket("bia");
        var celularDaBia = new NotificadorMovel("bia");
        hub.registrar(navegadorDaBia);
        hub.registrar(celularDaBia);
        hub.entrarNoCanal("geral", "ana");
        hub.entrarNoCanal("geral", "bia");

        var mensagens = new ServicoDeMensagens(hub);
        mensagens.publicarMensagem("geral", "ana", "bom dia");

        System.out.println("navegador da Bia: " + navegadorDaBia.recebidos().size() + " evento(s)");
        System.out.println("celular da Bia: " + celularDaBia.notificacoes());
    }
}
