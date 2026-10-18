package br.edu.ifal.arqsw.frete.dominio;

/** Valor monetario em centavos (objeto de valor imutavel). */
public record Dinheiro(long centavos) {

    public static final Dinheiro ZERO = new Dinheiro(0);

    public static Dinheiro reais(long reais) {
        return new Dinheiro(reais * 100);
    }

    public static Dinheiro centavos(long centavos) {
        return new Dinheiro(centavos);
    }

    public Dinheiro mais(Dinheiro outro) {
        return new Dinheiro(centavos + outro.centavos);
    }

    public Dinheiro vezes(long fator) {
        return new Dinheiro(centavos * fator);
    }

    public boolean maiorOuIgualA(Dinheiro outro) {
        return centavos >= outro.centavos;
    }

    @Override
    public String toString() {
        return String.format("R$ %d,%02d", centavos / 100, Math.abs(centavos % 100));
    }
}
