package br.edu.ifal.arqsw.telemetria.regras.condicao;

/** Strategy (Gamma et al., 1994, cap. 5): o criterio que dispara um alarme. */
public interface Condicao {
    boolean satisfeitaPor(double valor);
}
