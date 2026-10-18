package br.edu.ifal.arqsw.armazenamento.plugin.s3;

import br.edu.ifal.arqsw.armazenamento.nucleo.ArquivoInexistenteException;
import br.edu.ifal.arqsw.armazenamento.nucleo.PluginDeArmazenamento;
import br.edu.ifal.arqsw.armazenamento.plugin.local.Caminhos;
import br.edu.ifal.arqsw.armazenamento.plugin.s3.sdk.ClienteS3;

/**
 * Adapter (Gamma et al., 1994, cap. 4): traduz o contrato do nucleo para a API do SDK S3,
 * como apps/files_external/lib/Lib/Storage/AmazonS3.php no Nextcloud.
 *
 * CODIGO LEGADO: reaproveita um utilitario do plug-in LOCAL (plug-ins acoplados entre si)
 * e deixa vazar a excecao do SDK para quem usa o contrato. Leia o README.
 */
public class ArmazenamentoS3 implements PluginDeArmazenamento {

    private final ClienteS3 cliente;
    private final String bucket;

    public ArmazenamentoS3(ClienteS3 cliente, String bucket) {
        this.cliente = cliente;
        this.bucket = bucket;
    }

    @Override public String identificador() { return "s3"; }

    // TODO (regra 4): converter S3Exception em ArmazenamentoIndisponivelException
    //                 em todas as operacoes.

    @Override
    public void gravar(String caminho, byte[] dados) {
        cliente.putObject(bucket, Caminhos.normalizar(caminho), dados);
    }

    @Override
    public byte[] ler(String caminho) {
        byte[] dados = cliente.getObject(bucket, Caminhos.normalizar(caminho));
        if (dados == null) {
            throw new ArquivoInexistenteException(caminho);
        }
        return dados;
    }

    @Override
    public boolean existe(String caminho) {
        return cliente.doesObjectExist(bucket, Caminhos.normalizar(caminho));
    }
}
