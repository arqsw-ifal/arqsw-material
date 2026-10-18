package br.edu.ifal.arqsw.armazenamento.plugin.local;

import br.edu.ifal.arqsw.armazenamento.nucleo.ArmazenamentoIndisponivelException;
import br.edu.ifal.arqsw.armazenamento.nucleo.ArquivoInexistenteException;
import br.edu.ifal.arqsw.armazenamento.nucleo.PluginDeArmazenamento;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** Adapter para o sistema de arquivos local (java.nio). */
public class ArmazenamentoLocal implements PluginDeArmazenamento {

    private final Path raiz;

    public ArmazenamentoLocal(Path raiz) {
        this.raiz = raiz;
    }

    @Override public String identificador() { return "local"; }

    @Override
    public void gravar(String caminho, byte[] dados) {
        Path destino = resolver(caminho);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, dados);
        } catch (IOException e) {
            throw new ArmazenamentoIndisponivelException("falha ao gravar " + caminho, e);
        }
    }

    @Override
    public byte[] ler(String caminho) {
        try {
            return Files.readAllBytes(resolver(caminho));
        } catch (NoSuchFileException e) {
            throw new ArquivoInexistenteException(caminho);
        } catch (IOException e) {
            throw new ArmazenamentoIndisponivelException("falha ao ler " + caminho, e);
        }
    }

    @Override
    public boolean existe(String caminho) {
        return Files.isRegularFile(resolver(caminho));
    }

    private Path resolver(String caminho) {
        Path p = raiz.resolve(Caminhos.normalizar(caminho)).normalize();
        if (!p.startsWith(raiz)) {
            throw new IllegalArgumentException("caminho fora da raiz: " + caminho);
        }
        return p;
    }
}
