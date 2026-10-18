package br.edu.ifal.arqsw.timeline;

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
 * FITNESS FUNCTIONS (Richards & Ford, cap. 6; Apendice E): testes que verificam a ARQUITETURA.
 * A regra central e' a da Listagem 6.2.15: o servico de timeline so fala com portas.
 */
class TimelineArquiteturaTest {

    private static final String BASE = "br.edu.ifal.arqsw.timeline";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("a aplicacao da timeline nao depende do pacote de banco")
    void timelineNaoAcessaBanco() {
        noClasses().that().resideInAPackage(BASE + ".aplicacao..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".banco..")
                .because("a timeline e lida do cache; o banco e detalhe de infraestrutura")
                .check(classes);
    }

    @Test
    @DisplayName("a aplicacao depende apenas das proprias portas e do JDK")
    void aplicacaoSoConhecePortas() {
        classes().that().resideInAPackage(BASE + ".aplicacao..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(BASE + ".aplicacao..", "java..")
                .because("cache e banco concretos so sao conhecidos pela raiz de composicao")
                .check(classes);
    }

    @Test
    @DisplayName("as portas de saida da aplicacao sao interfaces")
    void portasSaoInterfaces() {
        classes().that().resideInAPackage(BASE + ".aplicacao..")
                .and().doNotHaveSimpleName("ServicoDeTimeline")
                .should().beInterfaces()
                .check(classes);
    }

    @Test
    @DisplayName("nao ha ciclos entre os pacotes")
    void semCiclos() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(classes);
    }
}
