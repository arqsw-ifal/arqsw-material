package br.edu.ifal.arqsw.limitador;

import br.edu.ifal.arqsw.limitador.api.LimitadorDeTaxa;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.implement;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * FITNESS FUNCTIONS (Richards & Ford, cap. 6) da Secao F.5.5: o middleware HTTP conhece apenas a
 * estrategia LimitadorDeTaxa, e os algoritmos sao testaveis com um relogio injetado.
 */
class ArquiteturaTest {

    private static final String BASE = "br.edu.ifal.arqsw.limitador";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("o filtro HTTP não depende do pacote de algoritmos")
    void filtroNaoConheceAlgoritmos() {
        noClasses().that().resideInAPackage(BASE + ".http..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".algoritmos..")
                .check(classes);
    }

    @Test
    @DisplayName("o filtro HTTP depende só da interface LimitadorDeTaxa, nunca de uma implementação")
    void filtroSoConheceAInterface() {
        noClasses().that().resideInAPackage(BASE + ".http..")
                .should().dependOnClassesThat(are(implement(LimitadorDeTaxa.class)).and(not(INTERFACES)))
                .check(classes);
    }

    @Test
    @DisplayName("os algoritmos usam o relógio injetado, nunca o relógio do sistema")
    void relogioInjetado() {
        noClasses().that().resideInAPackage(BASE + ".algoritmos..")
                .should().callMethod(System.class, "currentTimeMillis")
                .orShould().callMethod(System.class, "nanoTime")
                .orShould().callMethod(Instant.class, "now")
                .check(classes);
    }

    @Test
    @DisplayName("os algoritmos não conhecem o HTTP")
    void algoritmosSemHttp() {
        noClasses().that().resideInAnyPackage(BASE + ".algoritmos..", BASE + ".api..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".http..")
                .check(classes);
    }

    @Test
    @DisplayName("as implementações de LimitadorDeTaxa ficam no pacote de algoritmos")
    void implementacoesNoLugar() {
        classes().that().implement(LimitadorDeTaxa.class).and().areNotInterfaces()
                .should().resideInAPackage(BASE + ".algoritmos..")
                .check(classes);
    }
}
