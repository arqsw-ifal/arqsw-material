package br.edu.ifal.arqsw.telemetria;

import br.edu.ifal.arqsw.telemetria.regras.Alarme;
import br.edu.ifal.arqsw.telemetria.regras.Leitura;
import br.edu.ifal.arqsw.telemetria.regras.Regra;
import br.edu.ifal.arqsw.telemetria.regras.SomenteGrandeza;
import br.edu.ifal.arqsw.telemetria.regras.condicao.AbaixoDe;
import br.edu.ifal.arqsw.telemetria.regras.condicao.AcimaDe;
import br.edu.ifal.arqsw.telemetria.regras.condicao.ForaDaFaixa;
import br.edu.ifal.arqsw.telemetria.transporte.http.AdaptadorHttp;
import br.edu.ifal.arqsw.telemetria.transporte.mqtt.AdaptadorMqtt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de COMPORTAMENTO: o motor de regras e os transportes fazem o que devem fazer? */
class ComportamentoTest {

    private static Leitura leitura(String grandeza, double valor) {
        return new Leitura("sensor-7", grandeza, valor, 1760000000L);
    }

    private static List<String> tratar(Regra cadeia, Leitura leitura) {
        List<String> eventos = new ArrayList<>();
        cadeia.tratar(leitura, eventos);
        return eventos;
    }

    @Test
    @DisplayName("alarme acima do limite gera evento formatado")
    void alarmeGeraEvento() {
        assertEquals(List.of("ALARME sensor-7 temperatura=31.5"),
                tratar(new Alarme(new AcimaDe(30)), leitura("temperatura", 31.5)));
    }

    @Test
    @DisplayName("leitura dentro do limite nao gera evento")
    void dentroDoLimite() {
        assertEquals(List.of(), tratar(new Alarme(new AcimaDe(30)), leitura("temperatura", 29.9)));
    }

    @Test
    @DisplayName("filtro interrompe a cadeia para outras grandezas")
    void filtroInterrompe() {
        Regra cadeia = new SomenteGrandeza("temperatura");
        cadeia.entaoAplicar(new Alarme(new AcimaDe(30)));
        assertEquals(List.of(), tratar(cadeia, leitura("umidade", 99)));
    }

    @Test
    @DisplayName("filtro deixa passar a grandeza configurada para o proximo elo")
    void filtroDeixaPassar() {
        Regra cadeia = new SomenteGrandeza("temperatura");
        cadeia.entaoAplicar(new Alarme(new AcimaDe(30)));
        assertEquals(List.of("ALARME sensor-7 temperatura=31.5"), tratar(cadeia, leitura("temperatura", 31.5)));
    }

    @Test
    @DisplayName("cadeia de tres elos aplica todos os alarmes em ordem")
    void cadeiaDeTresElos() {
        Regra cadeia = new SomenteGrandeza("temperatura");
        cadeia.entaoAplicar(new Alarme(new AcimaDe(30))).entaoAplicar(new Alarme(new ForaDaFaixa(0, 50)));
        assertEquals(List.of("ALARME sensor-7 temperatura=55.0", "ALARME sensor-7 temperatura=55.0"),
                tratar(cadeia, leitura("temperatura", 55)));
    }

    @Test
    @DisplayName("condicao AbaixoDe e' satisfeita so abaixo do limite")
    void abaixoDe() {
        AbaixoDe c = new AbaixoDe(10);
        assertTrue(c.satisfeitaPor(9.9));
        assertFalse(c.satisfeitaPor(10));
        assertFalse(c.satisfeitaPor(11));
    }

    @Test
    @DisplayName("condicao ForaDaFaixa e' satisfeita so fora da faixa")
    void foraDaFaixa() {
        ForaDaFaixa c = new ForaDaFaixa(18, 26);
        assertTrue(c.satisfeitaPor(17.9));
        assertTrue(c.satisfeitaPor(26.1));
        assertFalse(c.satisfeitaPor(18));
        assertFalse(c.satisfeitaPor(22));
        assertFalse(c.satisfeitaPor(26));
    }

    @Test
    @DisplayName("adaptador HTTP converte o corpo e entrega a leitura a cadeia")
    void adaptadorHttp() {
        AdaptadorHttp http = new AdaptadorHttp(new Alarme(new AcimaDe(30)));
        assertEquals(List.of("ALARME sensor-7 temperatura=31.5"),
                http.receber("sensor-7;temperatura;31.5;1760000000"));
    }

    @Test
    @DisplayName("adaptador HTTP rejeita corpo malformado")
    void httpMalformado() {
        AdaptadorHttp http = new AdaptadorHttp(new Alarme(new AcimaDe(30)));
        assertThrows(IllegalArgumentException.class, () -> http.receber("sensor-7;temperatura"));
        assertThrows(IllegalArgumentException.class, () -> http.receber("sensor-7;temperatura;quente;1"));
    }

    @Test
    @DisplayName("adaptador MQTT entrega todas as grandezas da carga util")
    void adaptadorMqtt() {
        AdaptadorMqtt mqtt = new AdaptadorMqtt(new Alarme(new AcimaDe(30)), () -> 1760000000L);
        assertEquals(List.of("ALARME sensor-7 temperatura=31.5"),
                mqtt.aoPublicar("dispositivos/sensor-7/telemetria", "{\"umidade\":20,\"temperatura\":31.5}"));
        assertThrows(IllegalArgumentException.class, () -> mqtt.aoPublicar("v1/outro", "{\"umidade\":20}"));
    }
}
