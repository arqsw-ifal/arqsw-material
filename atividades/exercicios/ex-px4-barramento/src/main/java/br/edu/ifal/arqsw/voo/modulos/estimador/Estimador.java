package br.edu.ifal.arqsw.voo.modulos.estimador;

import br.edu.ifal.arqsw.voo.barramento.Barramento;
import br.edu.ifal.arqsw.voo.mensagens.AtitudeVeiculo;
import br.edu.ifal.arqsw.voo.mensagens.LeituraSensores;
import br.edu.ifal.arqsw.voo.mensagens.Topicos;
import br.edu.ifal.arqsw.voo.modulos.atitude.ControleAtitude;

/**
 * Estimador de atitude (papel do EKF2, muito simplificado): recebe a leitura dos sensores e
 * publica a atitude estimada. A "estimativa" didatica apenas normaliza o quaternio medido.
 */
public final class Estimador {

    // ATALHO: estado global que o controle de taxa le diretamente (cria um ciclo entre modulos)
    private static volatile long ultimaAmostraUs;

    private final Barramento barramento;
    private final ControleAtitude controleAtitude;   // ATALHO: o estimador conhece o proximo modulo

    public Estimador(Barramento barramento) {
        this.barramento = barramento;
        this.controleAtitude = new ControleAtitude(barramento);
        // TODO (comportamento): o estimador deve ser disparado pelo topico SENSORES,
        //      e nao por uma chamada direta de quem o criou.
    }

    /** Chamado diretamente pela App (atalho): deveria ser um callback do topico SENSORES. */
    public void processar(LeituraSensores leitura) {
        AtitudeVeiculo atitude = estimar(leitura);
        ultimaAmostraUs = atitude.amostraUs();
        barramento.publicar(Topicos.ATITUDE, atitude);
        controleAtitude.processar(atitude);           // ATALHO: chamada direta ao outro modulo
    }

    public static long ultimaAmostraUs() {
        return ultimaAmostraUs;
    }

    private static AtitudeVeiculo estimar(LeituraSensores l) {
        double norma = Math.sqrt(l.qw() * l.qw() + l.qx() * l.qx() + l.qy() * l.qy() + l.qz() * l.qz());
        return new AtitudeVeiculo(l.amostraUs(), l.qw() / norma, l.qx() / norma, l.qy() / norma, l.qz() / norma);
    }
}
