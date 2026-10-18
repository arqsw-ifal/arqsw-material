package br.edu.ifal.arqsw.voo.modulos.atitude;

import br.edu.ifal.arqsw.voo.barramento.Barramento;
import br.edu.ifal.arqsw.voo.mensagens.AtitudeVeiculo;
import br.edu.ifal.arqsw.voo.mensagens.SetpointTaxa;
import br.edu.ifal.arqsw.voo.mensagens.Topicos;
import br.edu.ifal.arqsw.voo.modulos.taxa.ControleTaxa;

/** Controlador de atitude: recebe a atitude e produz setpoints de taxa angular. */
public final class ControleAtitude {

    static final double GANHO = 6.5;   // lei de controle omitida: ganho proporcional ilustrativo

    private final Barramento barramento;
    private final ControleTaxa controleTaxa;   // ATALHO: conhece o modulo seguinte da cascata

    public ControleAtitude(Barramento barramento) {
        this.barramento = barramento;
        this.controleTaxa = new ControleTaxa(barramento);
        // TODO (comportamento): assinar o topico ATITUDE; a chegada de uma atitude deve
        //      disparar o controlador, como a SubscriptionCallbackWorkItem do PX4.
    }

    /** Chamado diretamente pelo Estimador (atalho). */
    public void processar(AtitudeVeiculo atitude) {
        SetpointTaxa setpoint = new SetpointTaxa(
                -GANHO * atitude.qx(), -GANHO * atitude.qy(), -GANHO * atitude.qz());
        barramento.publicar(Topicos.SETPOINT_TAXA, setpoint);
        controleTaxa.processar(setpoint);          // ATALHO: chamada direta ao outro modulo
    }
}
