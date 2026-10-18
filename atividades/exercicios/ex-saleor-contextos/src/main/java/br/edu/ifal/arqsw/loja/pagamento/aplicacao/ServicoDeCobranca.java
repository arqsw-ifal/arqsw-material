package br.edu.ifal.arqsw.loja.pagamento.aplicacao;

import br.edu.ifal.arqsw.loja.pagamento.dominio.RespostaDeCobranca;
import br.edu.ifal.arqsw.loja.sdk.xpay.XPayCharge;
import br.edu.ifal.arqsw.loja.sdk.xpay.XPayClient;

/**
 * Servico de cobranca do contexto de Pagamento (Listagem 6.6.10 do livro).
 * Traduz o modelo do provedor (XPayCharge) para a linguagem do contexto (RespostaDeCobranca).
 */
public class ServicoDeCobranca {

    private final XPayClient provedor;

    public ServicoDeCobranca(XPayClient provedor) {
        this.provedor = provedor;
    }

    public RespostaDeCobranca cobrar(String chaveIdempotencia, long valorCentavos) {
        // TODO (regra 4): IDEMPOTENCIA. Guardar o resultado de cada chave e, numa repeticao:
        //   - mesma chave e mesmo valor  -> devolver o resultado guardado SEM chamar o provedor;
        //   - mesma chave e outro valor  -> ConflitoDeIdempotenciaException.
        // TODO (regra 5): em XPayTimeoutException, tentar de novo (ate 3 vezes) com a MESMA chave;
        //   se nenhuma tentativa responder, lancar PagamentoIndisponivelException.
        XPayCharge charge = provedor.charge(valorCentavos, chaveIdempotencia);
        return traduzir(charge);
    }

    private static RespostaDeCobranca traduzir(XPayCharge charge) {
        boolean aprovada = "succeeded".equals(charge.status());
        return new RespostaDeCobranca(aprovada, charge.id(), charge.amountCents(),
                aprovada ? null : charge.declineCode());
    }
}
