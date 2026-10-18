package br.edu.ifal.arqsw.armazenamento.nucleo;

/**
 * Contrato que todo plug-in de armazenamento implementa (no Nextcloud: a interface IStorage).
 * Caminhos sao relativos ao armazenamento; uma barra inicial, se houver, e' ignorada.
 */
public interface PluginDeArmazenamento {

    /** Identificador unico do plug-in, por exemplo "s3", "local" ou "memoria". */
    String identificador();

    void gravar(String caminho, byte[] dados);

    /** @throws ArquivoInexistenteException se nao houver arquivo no caminho */
    byte[] ler(String caminho);

    boolean existe(String caminho);
}
