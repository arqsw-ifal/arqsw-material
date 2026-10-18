package br.edu.ifal.arqsw.armazenamento.nucleo;

/**
 * Excecao do CONTRATO do nucleo para "o servico externo nao respondeu".
 * Os plug-ins convertem as excecoes das bibliotecas externas nesta (no Nextcloud:
 * StorageNotAvailableException, do espaco publico OCP).
 */
public class ArmazenamentoIndisponivelException extends RuntimeException {
    public ArmazenamentoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
