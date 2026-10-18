package br.edu.ifal.arqsw.voo;

import br.edu.ifal.arqsw.voo.barramento.Barramento;
import br.edu.ifal.arqsw.voo.mensagens.LeituraSensores;
import br.edu.ifal.arqsw.voo.mensagens.Topicos;
import br.edu.ifal.arqsw.voo.modulos.estimador.Estimador;

/** Ponto de entrada: simula uma leitura de sensores e imprime o torque resultante. */
public class App {
    public static void main(String[] args) {
        var barramento = new Barramento();
        var estimador = new Estimador(barramento);   // cria, por dentro, os outros modulos
        estimador.processar(new LeituraSensores(1_000, 2.0, 0.2, -0.1, 0.0));
        System.out.println(barramento.ultima(Topicos.SETPOINT_TORQUE).orElseThrow());
    }
}
