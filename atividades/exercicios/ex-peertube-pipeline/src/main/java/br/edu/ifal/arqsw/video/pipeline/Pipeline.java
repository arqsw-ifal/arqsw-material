package br.edu.ifal.arqsw.video.pipeline;

import br.edu.ifal.arqsw.video.filtros.FiltroManifesto;
import br.edu.ifal.arqsw.video.filtros.FiltroMiniatura;
import br.edu.ifal.arqsw.video.filtros.FiltroTranscodificacao;
import br.edu.ifal.arqsw.video.filtros.FiltroValidacao;

import java.util.List;
import java.util.Optional;

/**
 * Conector (o "cano"): encadeia os filtros na ordem dada.
 *
 * CODIGO LEGADO: o conector conhece os filtros concretos (metodo padrao) e ignora o descarte
 * feito por um filtro testador. Leia o README.
 */
public final class Pipeline {

    private final List<Filtro> filtros;

    public Pipeline(List<Filtro> filtros) {
        this.filtros = List.copyOf(filtros);
    }

    /** O pipeline "de fabrica": 480p, 720p, miniatura e manifesto. */
    public static Pipeline padrao() {
        return new Pipeline(List.of(new FiltroValidacao(), new FiltroTranscodificacao(480),
                new FiltroTranscodificacao(720), new FiltroMiniatura(), new FiltroManifesto()));
    }

    public Optional<Tarefa> executar(Tarefa inicial) {
        Tarefa atual = inicial;
        for (Filtro f : filtros) {
            // TODO (regra 1): se um filtro devolver Optional.empty(), a tarefa foi descartada:
            //   o fluxo deve parar ali e o pipeline deve devolver Optional.empty().
            atual = f.processar(atual).orElse(atual);
        }
        return Optional.of(atual);
    }
}
