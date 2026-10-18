package br.edu.ifal.arqsw.armazenamento;

import br.edu.ifal.arqsw.armazenamento.nucleo.ArmazenamentoIndisponivelException;
import br.edu.ifal.arqsw.armazenamento.nucleo.Cota;
import br.edu.ifal.arqsw.armazenamento.nucleo.CotaExcedidaException;
import br.edu.ifal.arqsw.armazenamento.nucleo.PluginDeArmazenamento;
import br.edu.ifal.arqsw.armazenamento.nucleo.RegistroDePlugins;
import br.edu.ifal.arqsw.armazenamento.plugin.local.ArmazenamentoLocal;
import br.edu.ifal.arqsw.armazenamento.plugin.memoria.ArmazenamentoEmMemoria;
import br.edu.ifal.arqsw.armazenamento.plugin.s3.ArmazenamentoS3;
import br.edu.ifal.arqsw.armazenamento.plugin.s3.sdk.ClienteS3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de COMPORTAMENTO: o microkernel e seus plug-ins fazem o que devem fazer? */
class ComportamentoTest {

    private static byte[] bytes(int n) {
        return new byte[n];
    }

    @Test
    @DisplayName("registro devolve o plug-in pelo identificador")
    void registroDevolvePlugin() {
        RegistroDePlugins registro = new RegistroDePlugins();
        PluginDeArmazenamento memoria = new ArmazenamentoEmMemoria();
        registro.registrar(memoria);
        assertSame(memoria, registro.obter("memoria"));
    }

    @Test
    @DisplayName("registro rejeita identificador desconhecido")
    void registroRejeitaDesconhecido() {
        RegistroDePlugins registro = new RegistroDePlugins();
        assertThrows(IllegalArgumentException.class, () -> registro.obter("dropbox"));
    }

    @Test
    @DisplayName("registro rejeita identificador duplicado")
    void registroRejeitaDuplicado() {
        RegistroDePlugins registro = new RegistroDePlugins();
        registro.registrar(new ArmazenamentoEmMemoria());
        assertThrows(IllegalStateException.class, () -> registro.registrar(new ArmazenamentoEmMemoria()));
    }

    @Test
    @DisplayName("carregarDoClasspath descobre o plug-in em memoria")
    void descobrePlugins() {
        RegistroDePlugins registro = new RegistroDePlugins();
        registro.carregarDoClasspath();
        assertEquals("memoria", registro.obter("memoria").identificador());
    }

    @Test
    @DisplayName("plug-in em memoria grava e le")
    void memoriaGravaELe() {
        PluginDeArmazenamento p = new ArmazenamentoEmMemoria();
        p.gravar("/docs/a.txt", new byte[] {1, 2, 3});
        assertArrayEquals(new byte[] {1, 2, 3}, p.ler("docs/a.txt"));
        assertTrue(p.existe("docs/a.txt"));
    }

    @Test
    @DisplayName("plug-in local grava e le no disco")
    void localGravaELe(@TempDir Path raiz) {
        PluginDeArmazenamento p = new ArmazenamentoLocal(raiz);
        p.gravar("/fotos/praia.png", new byte[] {9, 8});
        assertTrue(Files.exists(raiz.resolve("fotos/praia.png")));
        assertArrayEquals(new byte[] {9, 8}, p.ler("fotos/praia.png"));
    }

    @Test
    @DisplayName("adaptador S3 traduz o caminho em chave sem barra inicial")
    void s3TraduzCaminho() {
        ClienteS3 sdk = new ClienteS3();
        PluginDeArmazenamento p = new ArmazenamentoS3(sdk, "arquivos");
        p.gravar("/fotos//praia.png", new byte[] {7});
        assertEquals(Set.of("fotos/praia.png"), sdk.listKeys("arquivos"));
        assertArrayEquals(new byte[] {7}, p.ler("fotos/praia.png"));
    }

    @Test
    @DisplayName("adaptador S3 converte a falha do SDK na excecao do contrato")
    void s3ConverteFalha() {
        ClienteS3 sdk = new ClienteS3();
        PluginDeArmazenamento p = new ArmazenamentoS3(sdk, "arquivos");
        sdk.simularQueda(true);
        assertThrows(ArmazenamentoIndisponivelException.class, () -> p.gravar("a.txt", bytes(1)));
        assertThrows(ArmazenamentoIndisponivelException.class, () -> p.ler("a.txt"));
        assertThrows(ArmazenamentoIndisponivelException.class, () -> p.existe("a.txt"));
    }

    @Test
    @DisplayName("cota recusa gravacao acima do limite sem tocar no plug-in")
    void cotaRecusaExcesso() {
        PluginDeArmazenamento interno = new ArmazenamentoEmMemoria();
        Cota cota = new Cota(interno, 5);
        assertThrows(CotaExcedidaException.class, () -> cota.gravar("grande.bin", bytes(6)));
        assertFalse(interno.existe("grande.bin"));
        cota.gravar("pequeno.bin", bytes(5));
        assertEquals(0, cota.espacoLivre());
    }

    @Test
    @DisplayName("cota nao conta duas vezes um arquivo sobrescrito")
    void cotaSobrescrita() {
        Cota cota = new Cota(new ArmazenamentoEmMemoria(), 10);
        cota.gravar("a.bin", bytes(8));
        assertDoesNotThrow(() -> cota.gravar("a.bin", bytes(8)));
        assertEquals(8, cota.usadoBytes());
        assertThrows(CotaExcedidaException.class, () -> cota.gravar("b.bin", bytes(3)));
    }
}
