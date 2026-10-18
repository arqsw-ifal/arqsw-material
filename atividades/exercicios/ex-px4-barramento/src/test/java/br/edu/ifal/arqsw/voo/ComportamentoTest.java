package br.edu.ifal.arqsw.voo;

import br.edu.ifal.arqsw.voo.barramento.Barramento;
import br.edu.ifal.arqsw.voo.mensagens.AtitudeVeiculo;
import br.edu.ifal.arqsw.voo.mensagens.LeituraSensores;
import br.edu.ifal.arqsw.voo.mensagens.SetpointTaxa;
import br.edu.ifal.arqsw.voo.mensagens.SetpointTorque;
import br.edu.ifal.arqsw.voo.mensagens.Topicos;
import br.edu.ifal.arqsw.voo.modulos.atitude.ControleAtitude;
import br.edu.ifal.arqsw.voo.modulos.estimador.Estimador;
import br.edu.ifal.arqsw.voo.modulos.taxa.ControleTaxa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de COMPORTAMENTO: o barramento e os modulos fazem o que devem fazer? */
class ComportamentoTest {

    private static final double EPS = 1e-9;
    private Barramento barramento;

    @BeforeEach
    void preparar() {
        barramento = new Barramento();
    }

    @Test
    @DisplayName("o barramento guarda a última amostra publicada em cada tópico")
    void ultimaAmostra() {
        barramento.publicar(Topicos.SETPOINT_TAXA, new SetpointTaxa(1, 2, 3));
        barramento.publicar(Topicos.SETPOINT_TAXA, new SetpointTaxa(4, 5, 6));
        assertEquals(new SetpointTaxa(4, 5, 6), barramento.ultima(Topicos.SETPOINT_TAXA).orElseThrow());
    }

    @Test
    @DisplayName("tópico ainda sem publicação devolve leitura vazia")
    void topicoVazio() {
        assertTrue(barramento.ultima(Topicos.ATITUDE).isEmpty());
    }

    @Test
    @DisplayName("o assinante é notificado a cada mensagem publicada")
    void assinanteNotificado() {
        List<SetpointTaxa> recebidas = new ArrayList<>();
        barramento.assinar(Topicos.SETPOINT_TAXA, recebidas::add);
        barramento.publicar(Topicos.SETPOINT_TAXA, new SetpointTaxa(1, 2, 3));
        barramento.publicar(Topicos.SETPOINT_TAXA, new SetpointTaxa(4, 5, 6));
        assertEquals(List.of(new SetpointTaxa(1, 2, 3), new SetpointTaxa(4, 5, 6)), recebidas);
    }

    @Test
    @DisplayName("vários assinantes recebem a mensagem, na ordem em que assinaram")
    void variosAssinantes() {
        List<String> ordem = new ArrayList<>();
        barramento.assinar(Topicos.SETPOINT_TAXA, m -> ordem.add("primeiro"));
        barramento.assinar(Topicos.SETPOINT_TAXA, m -> ordem.add("segundo"));
        barramento.publicar(Topicos.SETPOINT_TAXA, new SetpointTaxa(0, 0, 0));
        assertEquals(List.of("primeiro", "segundo"), ordem);
    }

    @Test
    @DisplayName("a mensagem chega apenas aos assinantes do próprio tópico")
    void isolamentoPorTopico() {
        List<Object> deTaxa = new ArrayList<>();
        List<Object> deAtitude = new ArrayList<>();
        barramento.assinar(Topicos.SETPOINT_TAXA, deTaxa::add);
        barramento.assinar(Topicos.ATITUDE, deAtitude::add);
        barramento.publicar(Topicos.SETPOINT_TAXA, new SetpointTaxa(1, 1, 1));
        assertEquals(1, deTaxa.size());
        assertEquals(0, deAtitude.size());
    }

    @Test
    @DisplayName("o estimador, disparado pelo tópico de sensores, publica a atitude normalizada")
    void estimadorNormaliza() {
        new Estimador(barramento);
        barramento.publicar(Topicos.SENSORES, new LeituraSensores(2_000, 0.0, 3.0, 4.0, 0.0));
        AtitudeVeiculo a = barramento.ultima(Topicos.ATITUDE).orElseThrow(
                () -> new AssertionError("nenhuma atitude foi publicada"));
        assertEquals(2_000, a.amostraUs());
        assertEquals(0.0, a.qw(), EPS);
        assertEquals(0.6, a.qx(), EPS);
        assertEquals(0.8, a.qy(), EPS);
        assertEquals(0.0, a.qz(), EPS);
    }

    @Test
    @DisplayName("o controle de atitude reage à atitude publicada no barramento")
    void controleAtitudeReage() {
        new ControleAtitude(barramento);
        barramento.publicar(Topicos.ATITUDE, new AtitudeVeiculo(1_000, 1.0, 0.1, -0.2, 0.0));
        SetpointTaxa sp = barramento.ultima(Topicos.SETPOINT_TAXA).orElseThrow(
                () -> new AssertionError("nenhum setpoint de taxa foi publicado"));
        assertEquals(-0.65, sp.rolagem(), EPS);
        assertEquals(1.3, sp.arfagem(), EPS);
        assertEquals(0.0, sp.guinada(), EPS);
    }

    @Test
    @DisplayName("o controle de taxa satura o torque no intervalo [-1, 1]")
    void controleTaxaSatura() {
        new ControleTaxa(barramento);
        barramento.publicar(Topicos.SETPOINT_TAXA, new SetpointTaxa(10.0, -10.0, 0.1));
        SetpointTorque t = barramento.ultima(Topicos.SETPOINT_TORQUE).orElseThrow(
                () -> new AssertionError("nenhum torque foi publicado"));
        assertEquals(1.0, t.rolagem(), EPS);
        assertEquals(-1.0, t.arfagem(), EPS);
        assertEquals(0.015, t.guinada(), EPS);
    }

    @Test
    @DisplayName("cadeia completa: sensores -> atitude -> taxa -> torque, preservando o instante da amostra")
    void cadeiaCompleta() {
        new Estimador(barramento);
        new ControleAtitude(barramento);
        new ControleTaxa(barramento);
        List<SetpointTorque> torques = new ArrayList<>();
        barramento.assinar(Topicos.SETPOINT_TORQUE, torques::add);

        barramento.publicar(Topicos.SENSORES, new LeituraSensores(5_000, 2.0, 0.0, 0.0, 0.2));

        assertEquals(1, torques.size(), "cada leitura deve gerar exatamente um torque");
        SetpointTorque t = torques.get(0);
        double qz = 0.2 / Math.sqrt(4.04);
        assertEquals(5_000, t.amostraUs());
        assertEquals(0.0, t.rolagem(), EPS);
        assertEquals(0.0, t.arfagem(), EPS);
        assertEquals(Math.max(-1, Math.min(1, 0.15 * -6.5 * qz)), t.guinada(), EPS);
    }
}
