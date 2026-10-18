package br.edu.ifal.arqsw.armazenamento.nucleo;

import br.edu.ifal.arqsw.armazenamento.plugin.memoria.ArmazenamentoEmMemoria;
import br.edu.ifal.arqsw.armazenamento.plugin.s3.ArmazenamentoS3;
import br.edu.ifal.arqsw.armazenamento.plugin.s3.sdk.ClienteS3;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Registro de plug-ins do microkernel (Richards e Ford, 2020, cap. 12).
 *
 * CODIGO LEGADO: a "descoberta" de plug-ins e' feita com new, e por isso o NUCLEO
 * importa plug-ins concretos e ate o SDK do S3. Leia o README.
 */
public final class RegistroDePlugins {

    private final Map<String, PluginDeArmazenamento> plugins = new TreeMap<>();

    public void registrar(PluginDeArmazenamento plugin) {
        // TODO (regra 2): identificador ja registrado -> IllegalStateException
        plugins.put(plugin.identificador(), plugin);
    }

    public PluginDeArmazenamento obter(String identificador) {
        // TODO (regra 1): identificador desconhecido -> IllegalArgumentException (hoje devolve null)
        return plugins.get(identificador);
    }

    public Set<String> identificadores() {
        return plugins.keySet();
    }

    /** Descobre os plug-ins disponiveis. */
    public void carregarDoClasspath() {
        registrar(new ArmazenamentoEmMemoria());
        registrar(new ArmazenamentoS3(new ClienteS3(), "padrao"));
    }
}
