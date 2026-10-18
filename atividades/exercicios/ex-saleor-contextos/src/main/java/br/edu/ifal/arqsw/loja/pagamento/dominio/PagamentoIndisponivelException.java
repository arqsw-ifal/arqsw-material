package br.edu.ifal.arqsw.loja.pagamento.dominio;

/** O provedor nao respondeu, mesmo apos novas tentativas. */
public class PagamentoIndisponivelException extends RuntimeException {
    public PagamentoIndisponivelException(String chave) {
        super("provedor de pagamento indisponivel (chave " + chave + ")");
    }
}
