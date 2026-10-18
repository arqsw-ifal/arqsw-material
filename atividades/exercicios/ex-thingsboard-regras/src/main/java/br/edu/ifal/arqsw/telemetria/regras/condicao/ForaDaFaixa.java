package br.edu.ifal.arqsw.telemetria.regras.condicao;

public final class ForaDaFaixa implements Condicao {

    private final double minimo;
    private final double maximo;

    public ForaDaFaixa(double minimo, double maximo) {
        if (minimo > maximo) {
            throw new IllegalArgumentException("faixa invalida");
        }
        this.minimo = minimo;
        this.maximo = maximo;
    }

    @Override
    public boolean satisfeitaPor(double valor) {
        // TODO (regra 2): satisfeita quando o valor esta abaixo do minimo ou acima do maximo
        return false;
    }
}
