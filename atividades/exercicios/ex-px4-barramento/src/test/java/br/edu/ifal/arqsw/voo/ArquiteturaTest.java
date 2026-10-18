package br.edu.ifal.arqsw.voo;

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
 * FITNESS FUNCTIONS (Richards & Ford, cap. 6) da Secao 6.8.10 (Listagem 6.8.13):
 * os modulos de voo so se comunicam pelo barramento, como no uORB do PX4.
 */
class ArquiteturaTest {

    private static final String BASE = "br.edu.ifal.arqsw.voo";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("ADR-PX4-001: um módulo nunca chama outro módulo diretamente")
    void modulosNaoSeConhecem() {
        slices().matching(BASE + ".modulos.(*)..").should().notDependOnEachOther().check(classes);
    }

    @Test
    @DisplayName("não há ciclos de dependência entre os módulos")
    void semCiclosEntreModulos() {
        slices().matching(BASE + ".modulos.(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test
    @DisplayName("módulos dependem só do barramento, das mensagens e da biblioteca padrão")
    void modulosSoDependemDoContrato() {
        classes().that().resideInAPackage(BASE + ".modulos..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        BASE + ".barramento..", BASE + ".mensagens..", BASE + ".modulos..", "java..", "")
                .check(classes);
    }

    @Test
    @DisplayName("mensagens são dados puros: não dependem de módulos")
    void mensagensSaoContratos() {
        noClasses().that().resideInAPackage(BASE + ".mensagens..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".modulos..")
                .check(classes);
    }

    @Test
    @DisplayName("o barramento é genérico: não conhece mensagens nem módulos")
    void barramentoGenerico() {
        noClasses().that().resideInAPackage(BASE + ".barramento..")
                .should().dependOnClassesThat().resideInAnyPackage(BASE + ".mensagens..", BASE + ".modulos..")
                .check(classes);
    }
}
