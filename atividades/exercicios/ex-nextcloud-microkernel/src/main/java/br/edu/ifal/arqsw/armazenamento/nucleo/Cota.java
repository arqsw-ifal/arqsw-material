package br.edu.ifal.arqsw.armazenamento.nucleo;

/**
 * Decorator de cota (como lib/private/Files/Storage/Wrapper/Quota.php do Nextcloud):
 * decide ANTES de delegar se a gravacao cabe no limite.
 */
public final class Cota extends Envoltorio {

    private final long limiteBytes;

    public Cota(PluginDeArmazenamento interno, long limiteBytes) {
        super(interno);
        this.limiteBytes = limiteBytes;
    }

    public long limiteBytes() { return limiteBytes; }

    /** Bytes ocupados pelos arquivos gravados por meio desta cota. */
    public long usadoBytes() {
        // TODO (regra 3): devolver a soma dos tamanhos ATUAIS dos arquivos gravados
        return 0;
    }

    public long espacoLivre() {
        return limiteBytes - usadoBytes();
    }

    @Override
    public void gravar(String caminho, byte[] dados) {
        // TODO (regra 3): se a gravacao nao couber na cota, lancar CotaExcedidaException
        //                 SEM chamar o plug-in envolvido. Sobrescrever um arquivo libera
        //                 o espaco que ele ocupava antes.
        super.gravar(caminho, dados);
    }
}
