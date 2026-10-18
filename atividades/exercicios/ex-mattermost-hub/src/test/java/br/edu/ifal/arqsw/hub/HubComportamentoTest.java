package br.edu.ifal.arqsw.hub;

import br.edu.ifal.arqsw.hub.conexoes.ConexaoWebSocket;
import br.edu.ifal.arqsw.hub.conexoes.NotificadorMovel;
import br.edu.ifal.arqsw.hub.hub.Evento;
import br.edu.ifal.arqsw.hub.hub.HubDeEventos;
import br.edu.ifal.arqsw.hub.produtores.ServicoDeMensagens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de COMPORTAMENTO: cada evento chega a quem deve, e so a quem deve? */
class HubComportamentoTest {

    private HubDeEventos hub;
    private ServicoDeMensagens mensagens;

    @BeforeEach
    void preparar() {
        hub = new HubDeEventos();
        mensagens = new ServicoDeMensagens(hub);
        hub.entrarNoCanal("geral", "ana");
        hub.entrarNoCanal("geral", "bia");
    }

    private ConexaoWebSocket conectar(String usuarioId) {
        ConexaoWebSocket conexao = new ConexaoWebSocket(usuarioId);
        hub.registrar(conexao);
        return conexao;
    }

    @Test
    @DisplayName("a mensagem publicada num canal chega aos membros do canal")
    void membroRecebe() {
        ConexaoWebSocket bia = conectar("bia");
        mensagens.publicarMensagem("geral", "ana", "oi");
        assertEquals(1, bia.recebidos().size());
        assertEquals("posted", bia.recebidos().get(0).tipo());
        assertEquals("ana: oi", bia.recebidos().get(0).dados());
    }

    @Test
    @DisplayName("quem nao e' membro do canal nao recebe a mensagem")
    void naoMembroNaoRecebe() {
        ConexaoWebSocket caio = conectar("caio");
        mensagens.publicarMensagem("geral", "ana", "oi");
        assertEquals(List.of(), caio.recebidos());
    }

    @Test
    @DisplayName("um usuario com duas conexoes recebe a mensagem nas duas")
    void duasConexoes() {
        ConexaoWebSocket navegador = conectar("bia");
        ConexaoWebSocket aplicativo = conectar("bia");
        mensagens.publicarMensagem("geral", "ana", "oi");
        assertEquals(1, navegador.recebidos().size());
        assertEquals(1, aplicativo.recebidos().size());
    }

    @Test
    @DisplayName("os eventos chegam a cada conexao na ordem em que foram publicados")
    void ordemPreservada() {
        ConexaoWebSocket bia = conectar("bia");
        mensagens.publicarMensagem("geral", "ana", "primeira");
        mensagens.publicarMensagem("geral", "ana", "segunda");
        assertEquals(List.of("ana: primeira", "ana: segunda"),
                bia.recebidos().stream().map(Evento::dados).toList());
    }

    @Test
    @DisplayName("mensagem vazia e' rejeitada")
    void mensagemVazia() {
        assertThrows(IllegalArgumentException.class, () -> mensagens.publicarMensagem("geral", "ana", " "));
    }

    @Test
    @DisplayName("outro tipo de assinante (notificador movel) tambem recebe a mensagem")
    void notificadorMovelRecebe() {
        NotificadorMovel celular = new NotificadorMovel("bia");
        hub.registrar(celular);
        mensagens.publicarMensagem("geral", "ana", "oi");
        assertEquals(List.of("ana: oi"), celular.notificacoes());
    }

    @Test
    @DisplayName("o filtro do assinante e' respeitado: canal silenciado nao entrega a mensagem")
    void canalSilenciado() {
        ConexaoWebSocket bia = conectar("bia");
        bia.silenciarCanal("geral");
        mensagens.publicarMensagem("geral", "ana", "oi");
        assertEquals(List.of(), bia.recebidos());
    }

    @Test
    @DisplayName("um evento com alcance de usuario chega so a esse usuario")
    void eventoDireto() {
        ConexaoWebSocket ana = conectar("ana");
        ConexaoWebSocket bia = conectar("bia");
        hub.publicar(new Evento("direct_added", null, "bia", "nova conversa"));
        assertEquals(1, bia.recebidos().size(), "a Bia deveria receber o evento direto");
        assertEquals(List.of(), ana.recebidos());
    }

    @Test
    @DisplayName("um assinante removido do hub nao recebe mais eventos")
    void assinanteRemovido() {
        ConexaoWebSocket bia = conectar("bia");
        hub.remover(bia);
        mensagens.publicarMensagem("geral", "ana", "oi");
        assertEquals(List.of(), bia.recebidos());
    }
}
