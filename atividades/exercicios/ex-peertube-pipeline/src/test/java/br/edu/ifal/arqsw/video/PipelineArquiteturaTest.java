package br.edu.ifal.arqsw.video;

import br.edu.ifal.arqsw.video.pipeline.Filtro;
import br.edu.ifal.arqsw.video.pipeline.Tarefa;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * FITNESS FUNCTIONS (Richards & Ford, cap. 6; Apendice E): testes que verificam a ARQUITETURA.
 * A primeira regra e' a da Listagem 6.4.17; a segunda a "calibra", como sugere a Secao 6.4.10.
 */
class PipelineArquiteturaTest {

    private static final String BASE = "br.edu.ifal.arqsw.video";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("os filtros nao dependem uns dos outros")
    void filtrosNaoDependemUnsDosOutros() {
        noClasses().that().implement(Filtro.class)
                .should().dependOnClassesThat().implement(Filtro.class)
                .because("em pipes and filters, os filtros so se conhecem pelo formato da mensagem")
                .check(classes);
    }

    @Test
    @DisplayName("os filtros dependem apenas da interface Filtro, da mensagem Tarefa e do JDK")
    void filtrosSoConhecemOContrato() {
        classes().that().implement(Filtro.class)
                .should().onlyDependOnClassesThat(resideInAPackage("java..")
                        .or(type(Filtro.class)).or(type(Tarefa.class)))
                .check(classes);
    }

    @Test
    @DisplayName("o conector Pipeline nao conhece os filtros concretos")
    void pipelineNaoConheceFiltros() {
        noClasses().that().resideInAPackage(BASE + ".pipeline..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".filtros..")
                .because("trocar, remover ou reordenar filtros nao deve alterar o conector")
                .check(classes);
    }

    @Test
    @DisplayName("nao ha ciclos entre os pacotes")
    void semCiclos() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(classes);
    }
}
