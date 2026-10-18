package br.edu.ifal.arqsw.timeline.banco;

import br.edu.ifal.arqsw.timeline.aplicacao.GrafoSocial;

import java.util.List;

/** Adaptador: implementa a porta GrafoSocial consultando o banco. */
public class GrafoSocialNoBanco implements GrafoSocial {

    private final BancoDeDados banco;

    public GrafoSocialNoBanco(BancoDeDados banco) {
        this.banco = banco;
    }

    /** CODIGO LEGADO: expoe o banco a quem recebe o adaptador. Leia o README. */
    public BancoDeDados banco() {
        return banco;
    }

    @Override
    public long totalDeSeguidores(long autorId) {
        return banco.seguidoresDe(autorId).size();
    }

    @Override
    public List<Long> seguidoresAtivos(long autorId) {
        return banco.seguidoresDe(autorId).stream().filter(banco::estaAtivo).toList();
    }

    @Override
    public List<Long> celebridadesSeguidasPor(long usuarioId, long limiar) {
        return banco.seguidosPor(usuarioId).stream()
                .filter(autor -> totalDeSeguidores(autor) >= limiar)
                .toList();
    }
}
