package br.edu.ifal.arqsw.armazenamento;

import br.edu.ifal.arqsw.armazenamento.nucleo.Cota;
import br.edu.ifal.arqsw.armazenamento.nucleo.PluginDeArmazenamento;
import br.edu.ifal.arqsw.armazenamento.nucleo.RegistroDePlugins;

import java.nio.charset.StandardCharsets;

/**
 * Ponto de entrada. Depois da refatoracao, este deve ser o UNICO lugar (fora dos proprios
 * plug-ins) que conhece classes concretas de plug-in: a raiz de composicao.
 */
public class App {
    public static void main(String[] args) {
        RegistroDePlugins registro = new RegistroDePlugins();
        registro.carregarDoClasspath();
        System.out.println("plug-ins: " + registro.identificadores());

        PluginDeArmazenamento comCota = new Cota(registro.obter("memoria"), 16);
        comCota.gravar("/notas/a.txt", "ola, nucleo".getBytes(StandardCharsets.UTF_8));
        System.out.println(new String(comCota.ler("notas/a.txt"), StandardCharsets.UTF_8));
    }
}
