package br.edu.ifal.arqsw.telemetria.regras.condicao;

public final class AcimaDe implements Condicao {

    private final double limite;

    public AcimaDe(double limite) {
        this.limite = limite;
    }

    @Override
    public boolean satisfeitaPor(double valor) {
        return valor > limite;
    }
}
