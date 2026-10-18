package br.edu.ifal.arqsw.voo.modulos.taxa;

import br.edu.ifal.arqsw.voo.barramento.Barramento;
import br.edu.ifal.arqsw.voo.mensagens.SetpointTaxa;
import br.edu.ifal.arqsw.voo.mensagens.SetpointTorque;
import br.edu.ifal.arqsw.voo.mensagens.Topicos;
import br.edu.ifal.arqsw.voo.modulos.estimador.Estimador;

/** Controlador de taxa angular: transforma o setpoint de taxa em torque normalizado. */
public final class ControleTaxa {

    static final double GANHO = 0.15;   // ganho proporcional ilustrativo

    private final Barramento barramento;

    public ControleTaxa(Barramento barramento) {
        this.barramento = barramento;
        // TODO (comportamento): assinar o topico SETPOINT_TAXA.
    }

    /** Chamado diretamente pelo ControleAtitude (atalho). */
    public void processar(SetpointTaxa setpoint) {
        // ATALHO: pergunta ao Estimador o instante da amostra, em vez de ler a ultima
        //         atitude publicada no barramento. Isso fecha um ciclo entre os modulos.
        long amostraUs = Estimador.ultimaAmostraUs();
        // TODO (comportamento): saturar cada eixo do torque no intervalo [-1, 1].
        barramento.publicar(Topicos.SETPOINT_TORQUE, new SetpointTorque(amostraUs,
                GANHO * setpoint.rolagem(), GANHO * setpoint.arfagem(), GANHO * setpoint.guinada()));
    }
}
