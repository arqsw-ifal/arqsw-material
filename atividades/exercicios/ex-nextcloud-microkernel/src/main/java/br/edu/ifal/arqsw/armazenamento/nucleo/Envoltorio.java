package br.edu.ifal.arqsw.armazenamento.nucleo;

/**
 * Decorator (Gamma et al., 1994, cap. 4), que no Nextcloud se chama Wrapper:
 * implementa o mesmo contrato e repassa cada operacao ao armazenamento envolvido.
 * As subclasses acrescentam UMA responsabilidade (cota, criptografia, ...).
 */
public abstract class Envoltorio implements PluginDeArmazenamento {

    protected final PluginDeArmazenamento interno;

    protected Envoltorio(PluginDeArmazenamento interno) {
        this.interno = interno;
    }

    @Override public String identificador() { return interno.identificador(); }
    @Override public void gravar(String caminho, byte[] dados) { interno.gravar(caminho, dados); }
    @Override public byte[] ler(String caminho) { return interno.ler(caminho); }
    @Override public boolean existe(String caminho) { return interno.existe(caminho); }
}
