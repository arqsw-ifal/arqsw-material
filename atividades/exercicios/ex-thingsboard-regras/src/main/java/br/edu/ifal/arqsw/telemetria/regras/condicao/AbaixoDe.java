package br.edu.ifal.arqsw.telemetria.regras.condicao;

public final class AbaixoDe implements Condicao {

    private final double limite;

    public AbaixoDe(double limite) {
        this.limite = limite;
    }

    @Override
    public boolean satisfeitaPor(double valor) {
        // TODO (regra 2): satisfeita quando o valor esta estritamente abaixo do limite
        return false;
    }
}
