package br.edu.ifal.arqsw.armazenamento;

import br.edu.ifal.arqsw.armazenamento.nucleo.PluginDeArmazenamento;
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
 * FITNESS FUNCTIONS (Richards & Ford, cap. 6) do estilo MICROKERNEL (cap. 12):
 * o nucleo depende apenas de abstracoes e os plug-ins sao independentes entre si.
 */
class ArquiteturaTest {

    private static final String BASE = "br.edu.ifal.arqsw.armazenamento";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("o nucleo nao depende de nenhum plug-in")
    void nucleoIndependente() {
        noClasses().that().resideInAPackage(BASE + ".nucleo..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".plugin..")
                .because("o nucleo so conhece o contrato PluginDeArmazenamento")
                .check(classes);
    }

    @Test
    @DisplayName("os plug-ins nao dependem uns dos outros")
    void pluginsIsolados() {
        slices().matching(BASE + ".plugin.(*)..").should().notDependOnEachOther().check(classes);
    }

    @Test
    @DisplayName("todo plug-in implementa o contrato do nucleo")
    void pluginsImplementamContrato() {
        classes().that().resideInAPackage(BASE + ".plugin..")
                .and().haveSimpleNameStartingWith("Armazenamento")
                .should().implement(PluginDeArmazenamento.class)
                .check(classes);
    }

    @Test
    @DisplayName("so o adaptador S3 (e a raiz de composicao) conhece o SDK do S3")
    void sdkConfinado() {
        noClasses().that().resideOutsideOfPackages(BASE + ".plugin.s3..", BASE)
                .should().dependOnClassesThat().resideInAPackage(BASE + ".plugin.s3.sdk..")
                .check(classes);
    }

    @Test
    @DisplayName("nao ha ciclos entre os pacotes")
    void semCiclos() {
        slices().matching(BASE + ".(**)").should().beFreeOfCycles().check(classes);
    }
}
