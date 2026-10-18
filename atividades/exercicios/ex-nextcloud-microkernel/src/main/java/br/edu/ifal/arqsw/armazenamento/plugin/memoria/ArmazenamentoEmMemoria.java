package br.edu.ifal.arqsw.armazenamento.plugin.memoria;

import br.edu.ifal.arqsw.armazenamento.nucleo.ArquivoInexistenteException;
import br.edu.ifal.arqsw.armazenamento.nucleo.PluginDeArmazenamento;

import java.util.HashMap;
import java.util.Map;

/** Plug-in em memoria (util em testes e como cache). Tem construtor sem argumentos. */
public class ArmazenamentoEmMemoria implements PluginDeArmazenamento {

    private final Map<String, byte[]> arquivos = new HashMap<>();

    @Override public String identificador() { return "memoria"; }

    @Override
    public void gravar(String caminho, byte[] dados) {
        arquivos.put(chave(caminho), dados.clone());
    }

    @Override
    public byte[] ler(String caminho) {
        byte[] dados = arquivos.get(chave(caminho));
        if (dados == null) {
            throw new ArquivoInexistenteException(caminho);
        }
        return dados.clone();
    }

    @Override
    public boolean existe(String caminho) {
        return arquivos.containsKey(chave(caminho));
    }

    private static String chave(String caminho) {
        return caminho.startsWith("/") ? caminho.substring(1) : caminho;
    }
}
