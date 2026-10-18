package br.edu.ifal.arqsw.timeline.aplicacao;

import br.edu.ifal.arqsw.timeline.banco.BancoDeDados;
import br.edu.ifal.arqsw.timeline.banco.GrafoSocialNoBanco;

import java.util.List;

/**
 * Timeline inicial com fanout hibrido: fanout on write (push) para a maioria dos autores e
 * fanout on read (pull) para as celebridades (Secao 6.2.10, Listagem 6.2.14).
 *
 * CODIGO LEGADO: o servico recebe as portas no construtor, mas "fura" a porta do grafo
 * social e consulta o banco diretamente. Alem disso, o caminho de leitura nunca foi
 * terminado. Leia o README antes de mexer.
 */
public class ServicoDeTimeline {

    private final CacheDeTimelines cache;
    private final GrafoSocial grafo;
    private final PostsPorAutor posts;
    private final long limiarCelebridade;

    public ServicoDeTimeline(CacheDeTimelines cache, GrafoSocial grafo,
                             PostsPorAutor posts, long limiarCelebridade) {
        this.cache = cache;
        this.grafo = grafo;
        this.posts = posts;
        this.limiarCelebridade = limiarCelebridade;
    }

    /** Caminho de escrita (no Mastodon, executado pelo DistributionWorker). */
    public void aoPublicar(long autorId, long postId) {
        cache.inserir(autorId, postId); // o autor ve a propria publicacao

        // ATALHO: "era mais rapido" ir direto ao banco do que acrescentar um metodo a porta.
        BancoDeDados banco = ((GrafoSocialNoBanco) grafo).banco();
        if (banco.seguidoresDe(autorId).size() >= limiarCelebridade) {
            return; // celebridade: sem fanout on write
        }
        // TODO (regra 1): so os seguidores ATIVOS recebem o fanout (ACTIVE_DURATION, Listagem 6.2.8)
        for (long seguidor : banco.seguidoresDe(autorId)) {
            cache.inserir(seguidor, postId);
        }
    }

    /** Caminho de leitura. */
    public List<Long> timeline(long usuarioId, int limite) {
        // TODO (regra 2): fanout on read. Mesclar as publicacoes recentes das celebridades
        //   seguidas pelo usuario, sem repeticoes, do maior ID para o menor, respeitando o limite.
        //   Enquanto isso nao for feito, as celebridades "somem" das timelines.
        return cache.recentes(usuarioId, limite);
    }
}
