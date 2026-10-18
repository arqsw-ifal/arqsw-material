package br.edu.ifal.arqsw.pedidos;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * FITNESS FUNCTIONS (Richards & Ford, cap. 6): testes que verificam a ARQUITETURA.
 * Eles falham quando uma dependencia proibida entre camadas aparece no codigo.
 */
class ArquiteturaTest {

    private static final String BASE = "br.edu.ifal.arqsw.pedidos";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("camadas: web -> aplicacao -> dominio <- infra")
    void camadas() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
                .layer("Web").definedBy(BASE + ".web..")
                .layer("Aplicacao").definedBy(BASE + ".aplicacao..")
                .layer("Dominio").definedBy(BASE + ".dominio..")
                .layer("Infra").definedBy(BASE + ".infra..")
                .whereLayer("Web").mayNotBeAccessedByAnyLayer()
                .whereLayer("Aplicacao").mayOnlyBeAccessedByLayers("Web")
                .whereLayer("Infra").mayNotBeAccessedByAnyLayer()
                .check(classes);
    }

    @Test
    @DisplayName("o dominio nao depende da infraestrutura")
    void dominioIndependente() {
        noClasses().that().resideInAPackage(BASE + ".dominio..")
                .should().dependOnClassesThat().resideInAnyPackage(BASE + ".infra..", BASE + ".web..")
                .check(classes);
    }

    @Test
    @DisplayName("o controller nao acessa o repositorio diretamente")
    void controllerSemAtalho() {
        noClasses().that().resideInAPackage(BASE + ".web..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".infra..")
                .check(classes);
    }

    @Test
    @DisplayName("nao ha ciclos entre os pacotes")
    void semCiclos() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(classes);
    }
}
