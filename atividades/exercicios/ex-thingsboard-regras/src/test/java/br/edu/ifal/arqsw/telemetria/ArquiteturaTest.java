package br.edu.ifal.arqsw.telemetria;

import br.edu.ifal.arqsw.telemetria.regras.Regra;
import br.edu.ifal.arqsw.telemetria.regras.condicao.Condicao;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * FITNESS FUNCTIONS (Richards & Ford, cap. 6): o motor de regras nao conhece os protocolos
 * de transporte (Listagem 6.7.13 do livro).
 */
class ArquiteturaTest {

    private static final String BASE = "br.edu.ifal.arqsw.telemetria";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("as regras nao dependem do transporte (MQTT/HTTP)")
    void regrasNaoConhecemTransporte() {
        noClasses().that().resideInAPackage(BASE + ".regras..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".transporte..")
                .because("o motor de regras deve funcionar com qualquer protocolo")
                .check(classes);
    }

    @Test
    @DisplayName("toda classe do pacote regras e' um elo da cadeia (exceto Leitura)")
    void todaRegraEstendeRegra() {
        classes().that().resideInAPackage(BASE + ".regras")
                .and().haveSimpleNameNotEndingWith("Leitura")
                .and().areNotAnonymousClasses()
                .and().areTopLevelClasses()
                .should().beAssignableTo(Regra.class)
                .check(classes);
    }

    @Test
    @DisplayName("toda condicao implementa a Strategy Condicao")
    void condicoesImplementamStrategy() {
        classes().that().resideInAPackage(BASE + ".regras.condicao..")
                .and().areNotInterfaces()
                .should().implement(Condicao.class)
                .check(classes);
    }

    @Test
    @DisplayName("os transportes nao dependem uns dos outros")
    void transportesIsolados() {
        slices().matching(BASE + ".transporte.(*)..").should().notDependOnEachOther().check(classes);
    }

    @Test
    @DisplayName("nao ha ciclos entre os pacotes")
    void semCiclos() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(classes);
    }
}
