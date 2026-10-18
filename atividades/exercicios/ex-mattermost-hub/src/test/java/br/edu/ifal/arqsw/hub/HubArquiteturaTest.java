package br.edu.ifal.arqsw.hub;

import br.edu.ifal.arqsw.hub.hub.Assinante;
import br.edu.ifal.arqsw.hub.hub.HubDeEventos;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * FITNESS FUNCTIONS (Richards & Ford, cap. 6; Apendice E): testes que verificam a ARQUITETURA.
 * As duas primeiras regras sao as da Listagem 6.3.14.
 */
class HubArquiteturaTest {

    private static final String BASE = "br.edu.ifal.arqsw.hub";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("os produtores nao conhecem os assinantes concretos")
    void produtoresNaoConhecemAssinantes() {
        noClasses().that().resideInAPackage(BASE + ".produtores..")
                .should().dependOnClassesThat().implement(Assinante.class)
                .because("no Observer, quem publica nao sabe quem escuta")
                .check(classes);
    }

    @Test
    @DisplayName("os produtores so enxergam o hub pela interface Publicador")
    void produtoresNaoConhecemOHubConcreto() {
        noClasses().that().resideInAPackage(BASE + ".produtores..")
                .should().dependOnClassesThat().areAssignableTo(HubDeEventos.class)
                .because("para quem publica, o hub e' um Mediator acessado por abstracao")
                .check(classes);
    }

    @Test
    @DisplayName("o hub nao depende das conexoes concretas")
    void hubNaoConheceConexoes() {
        noClasses().that().resideInAPackage(BASE + ".hub..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".conexoes..")
                .check(classes);
    }

    @Test
    @DisplayName("nao ha ciclos entre os pacotes")
    void semCiclos() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(classes);
    }
}
