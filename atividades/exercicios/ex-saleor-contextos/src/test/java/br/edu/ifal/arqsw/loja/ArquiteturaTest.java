package br.edu.ifal.arqsw.loja;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * FITNESS FUNCTIONS (Richards & Ford, cap. 6): os contextos delimitados Pedido e Pagamento
 * so se falam pela camada anticorrupcao (Evans, 2003, cap. 14).
 */
class ArquiteturaTest {

    private static final String BASE = "br.edu.ifal.arqsw.loja";
    private static JavaClasses classes;

    @BeforeAll
    static void importar() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("o dominio de pedido nao depende do contexto de pagamento")
    void dominioDePedidoIsolado() {
        noClasses().that().resideInAPackage(BASE + ".pedido.dominio..")
                .should().dependOnClassesThat().resideInAnyPackage(BASE + ".pagamento..", BASE + ".sdk..")
                .check(classes);
    }

    @Test
    @DisplayName("so a camada anticorrupcao de pedido acessa o contexto de pagamento")
    void somenteAclAcessaPagamento() {
        noClasses().that().resideInAPackage(BASE + ".pedido..")
                .and().resideOutsideOfPackage(BASE + ".pedido.acl..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".pagamento..")
                .check(classes);
    }

    @Test
    @DisplayName("o contexto de pedido nao conhece o SDK do provedor")
    void pedidoSemSdk() {
        noClasses().that().resideInAPackage(BASE + ".pedido..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".sdk..")
                .check(classes);
    }

    @Test
    @DisplayName("o contexto de pagamento nao depende do contexto de pedido")
    void pagamentoNaoConhecePedido() {
        noClasses().that().resideInAPackage(BASE + ".pagamento..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".pedido..")
                .check(classes);
    }
}
