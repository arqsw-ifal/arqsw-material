package br.edu.ifal.arqsw.frete;

import br.edu.ifal.arqsw.frete.dominio.Modalidade;
import br.edu.ifal.arqsw.frete.dominio.PoliticaDeFrete;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FITNESS FUNCTIONS (Richards & Ford, cap. 6): o padrao Strategy da Listagem 1.2 mantido
 * por testes. O dominio conhece apenas a abstracao PoliticaDeFrete (OCP e DIP).
 */
class ArquiteturaTest {

    private static final String BASE = "br.edu.ifal.arqsw.frete";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("o domínio não depende de implementações concretas de frete, só da interface")
    void dominioSoConheceAInterface() {
        noClasses().that().resideInAPackage(BASE + ".dominio..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".frete..")
                .check(classes);
    }

    @Test
    @DisplayName("o domínio não escolhe o frete com switch sobre a modalidade")
    void semSwitchSobreModalidade() {
        noClasses().that().resideInAPackage(BASE + ".dominio..")
                .and().doNotHaveFullyQualifiedName(Modalidade.class.getName())
                .should().callMethod(Modalidade.class, "ordinal")
                .orShould().callMethod(Modalidade.class, "values")
                .because("cada modalidade deve ser uma implementacao de PoliticaDeFrete (Strategy)")
                .check(classes);
    }

    @Test
    @DisplayName("existe ao menos uma implementação de PoliticaDeFrete por modalidade (Frete<Modalidade>)")
    void umaImplementacaoPorModalidade() {
        Set<String> implementacoes = classes.stream()
                .filter(c -> c.isAssignableTo(PoliticaDeFrete.class))
                .filter(c -> !c.isInterface() && !c.getModifiers().contains(JavaModifier.ABSTRACT))
                .map(JavaClass::getSimpleName)
                .collect(Collectors.toSet());
        List<String> faltando = new ArrayList<>();
        for (Modalidade m : Modalidade.values()) {
            String nome = "Frete" + m.name().charAt(0) + m.name().substring(1).toLowerCase();
            if (!implementacoes.contains(nome)) faltando.add(nome);
        }
        assertTrue(faltando.isEmpty(), "faltam implementacoes de PoliticaDeFrete: " + faltando
                + " (encontradas: " + implementacoes + ")");
    }

    @Test
    @DisplayName("as implementações de PoliticaDeFrete ficam no pacote frete")
    void implementacoesNoPacoteFrete() {
        classes().that().implement(PoliticaDeFrete.class).and().areNotInterfaces()
                .should().resideInAPackage(BASE + ".frete..")
                .check(classes);
    }

    @Test
    @DisplayName("não há ciclos entre os pacotes")
    void semCiclos() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(classes);
    }
}
