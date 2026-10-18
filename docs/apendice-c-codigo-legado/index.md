# Apêndice C - Explorando código legado

## Objetivos

Ao final deste apêndice, o leitor deverá ser capaz de:

- Definir código legado segundo Fox e Patterson e segundo Feathers e explicar por que a ausência
  de testes, e não a idade, é o critério decisivo.
- Relacionar a exploração de um repositório às teorias de compreensão de programas de Brooks,
  Pennington e Von Mayrhauser e Vans.
- Explorar uma base de código "de fora para dentro", estabelecer a verdade de referência com testes
  de caracterização e localizar costuras (*seams*) para isolar dependências.
- Usar o histórico do controle de versões para identificar *hotspots*, acoplamento temporal e
  distribuição do conhecimento, à maneira de Tornhill.
- Aplicar o roteiro de expedição em dez passos aos sete sistemas do livro e registrar padrões numa
  ficha *Known Uses*.
- Escolher ferramentas adequadas a cada passo da exploração.
- Contribuir com projetos de código aberto de forma ética e respeitosa.

## Introdução

Todo voo deste livro termina num pouso: a análise desce das características e dos estilos até o
código-fonte de um sistema real, grande, escrito por centenas de pessoas ao longo de anos (Capítulo
6). O leitor chega a esse código como um estranho. Não participou das decisões, não conhece as
convenções e não sabe que partes são essenciais e quais são acidentes históricos. A situação não é
excepcional: é a condição normal do trabalho em software. O SWEBOK v4 registra que pesquisas indicam
que cerca de metade do esforço total de manutenção é dedicada a compreender o software a ser
modificado, e que essa compreensão é mais difícil quando a evolução não foi documentada e os
desenvolvedores originais não estão disponíveis para explicá-la (WASHIZAKI, 2024, cap. 7, seção
2.1.1).

Este apêndice reúne as técnicas para essa expedição. A Seção C.1 define código legado. A Seção C.2
resume as teorias de compreensão de programas que explicam por que certas estratégias funcionam. A
Seção C.3 apresenta a exploração de fora para dentro de Fox e Patterson (2021, cap. 9), e a Seção C.4,
os testes de caracterização e as costuras de Feathers (2004). A Seção C.5 trata da arqueologia de
repositórios. A Seção C.6 consolida o roteiro de expedição em dez passos e o aplica aos sete sistemas,
a Seção C.7 apresenta a ficha *Known Uses*, a Seção C.8 lista ferramentas, e a Seção C.9 discute a ética
de contribuição em projetos de código aberto.

## C.1 O que é código legado

### C.1.1 Legado como sucesso

No uso corrente, "legado" tem conotação negativa: código velho, feio, que ninguém quer tocar. Fox e
Patterson (2021, p. 32) propõem começar pelo outro lado. Código legado é o software que, apesar da
idade, continua em uso porque atende às necessidades dos clientes; os autores registram que sessenta
por cento dos custos de manutenção correspondem a acrescentar funcionalidade ao software legado,
contra apenas dezessete por cento para corrigir defeitos, e concluem que software legado é software
bem-sucedido. Os sete sistemas deste livro são legados nesse sentido: todos acumulam anos de
desenvolvimento contínuo, descritos no Capítulo 2, e todos têm usuários que dependem deles.

A manutenção desse software é tradicionalmente classificada em quatro categorias, que Fox e Patterson
(2021, p. 270) atribuem a Lientz *et al.* (1978): **corretiva**, que repara defeitos; **perfectiva**, que
amplia a funcionalidade para atender a novos requisitos; **adaptativa**, que acompanha mudanças no
ambiente operacional mesmo sem nova funcionalidade; e **preventiva**, que melhora a estrutura para
facilitar a manutenção futura. As leis de evolução de software de Lehman, que os autores usam como
epígrafes do capítulo, explicam por que nenhuma das quatro pode ser dispensada: um sistema precisa ser
continuamente adaptado ou se torna progressivamente menos satisfatório, e sua complexidade cresce a
menos que se trabalhe para mantê-la ou reduzi-la (FOX; PATTERSON, 2021, p. 270 e 286).

### C.1.2 Legado como código sem testes

Feathers (2004) propôs uma definição operacional, adotada por Fox e Patterson (2021, p. 271-272):
código legado é código que não tem testes suficientes para ser modificado com confiança,
independentemente de quem o escreveu e de quando. Na formulação curta que os autores citam, "código
legado é código sem testes" (FOX; PATTERSON, 2021, p. 272). Por esse critério, o código que o próprio
leitor escreveu há três meses, em outro projeto, pode ser tão legado quanto um sistema de trinta
anos.

A definição decorre da distinção de Feathers entre duas maneiras de mudar software. Na primeira,
***Edit and Pray*** (editar e rezar), o desenvolvedor estuda uma pequena parte do código, faz a
alteração, verifica manualmente se algo quebrou e implanta esperando o melhor. Na segunda, ***Cover
and Modify*** (cobrir e modificar), ele cria primeiro testes que cubram o código a alterar e os usa
como rede de segurança para detectar mudanças de comportamento não intencionais (FOX; PATTERSON, 2021,
p. 271). A segunda é claramente preferível, mas esbarra no que Feathers (2004, cap. 2) chama de dilema
do código legado: para mudar o código com segurança, é preciso ter testes; para colocar testes, muitas
vezes é preciso mudar o código. As Seções C.3 e C.4 tratam de como sair desse círculo.

Fox e Patterson (2021, p. 272) organizam a saída em quatro passos: identificar os **pontos de
mudança**; se necessário, acrescentar **testes de caracterização** que capturem o comportamento atual;
verificar se os pontos de mudança precisam de **refatoração** para se tornarem testáveis, por exemplo
quebrando dependências; e, com o código coberto, **fazer a mudança**, acrescentando testes para o código
novo. Feathers (2004, cap. 2) propõe um algoritmo de mudança equivalente, em cinco passos: identificar
os pontos de mudança, encontrar os pontos de teste, quebrar dependências, escrever testes e, por fim,
fazer as mudanças e refatorar.

## C.2 Compreensão de programas

A exploração de código legado é um caso particular de **compreensão de programas**, tema estudado
pela psicologia cognitiva aplicada à programação desde os anos 1970. O SWEBOK v4 trata o tema como
técnica de manutenção e registra fatores que influenciam a compreensão: conhecimento do domínio,
práticas de programação, documentação e organização e apresentação do código (WASHIZAKI, 2024, cap. 7,
seções 2.1.1 e 4.1). Três modelos clássicos ajudam a entender por que certas estratégias de
exploração funcionam.

Brooks (1983) propôs uma teoria **descendente** (*top-down*) e guiada por hipóteses. Programar, para
ele, é construir uma cadeia de mapeamentos do domínio do problema, passando por domínios
intermediários, até o domínio da linguagem de programação; compreender um programa é reconstruir essa
cadeia. O leitor parte de uma hipótese global sobre o que o programa faz, deriva hipóteses
subsidiárias e as verifica contra o código, refinando-as ou descartando-as. Nessa verificação, ele se
apoia em ***beacons*** (balizas): traços característicos que sinalizam a presença de uma estrutura
conhecida, como uma troca de valores entre duas variáveis, que sugere uma ordenação. A teoria explica
por que o conhecimento do domínio acelera a leitura: quem sabe o que uma rede social federada precisa
fazer formula hipóteses melhores sobre o código do Mastodon.

Pennington (1987) estudou o movimento inverso, **ascendente** (*bottom-up*). Programadores diante de
código desconhecido em domínio pouco familiar constroem primeiro um **modelo do programa**, baseado
no fluxo de controle e nas estruturas de texto, e só depois um **modelo de situação**, que relaciona o
código aos objetos e às funções do mundo real. A leitura ascendente é mais lenta, mas é a única
disponível quando as hipóteses de domínio faltam.

Von Mayrhauser e Vans (1995) observaram programadores profissionais em tarefas de manutenção de
sistemas grandes e propuseram um **metamodelo integrado**, com três processos de compreensão, o modelo
descendente, o modelo de situação e o modelo de programa, alimentados por uma base de conhecimento.
A observação central é que os programadores alternam com frequência entre os três modelos, conforme
encontram trechos familiares ou estranhos, e que nenhuma estratégia isolada descreve o trabalho real
de compreensão em larga escala.

Argumenta-se aqui que o método das altitudes é uma aplicação deliberada dessas teorias. A descida dos
Capítulos 3 a 5 constrói, antes de abrir o código, o conhecimento de domínio e as hipóteses
estruturais que a leitura descendente de Brooks exige: quando o leitor chega ao `FeedManager` do
Mastodon, já espera encontrar uma *timeline* pré-computada e limitada. A subida de verificação do
Capítulo 6 é a confirmação ou a refutação dessas hipóteses. E, quando a hipótese falha, o leitor recorre
à leitura ascendente, linha a linha, até reconstruir um modelo de situação, exatamente a alternância
descrita por Von Mayrhauser e Vans.

## C.3 Explorar de fora para dentro

### C.3.1 Os passos de Fox e Patterson

Fox e Patterson (2021, p. 273) propõem compreender um sistema legado a partir do ponto de vista dos
clientes e dos desenvolvedores, seguindo passos "de fora para dentro" (*outside-in*): obter uma
**ramificação de rascunho** para executar a aplicação num ambiente de desenvolvimento; **aprender e
reproduzir as histórias de usuário**, com a ajuda de outros *stakeholders* se necessário; **examinar o
esquema do banco** e as relações entre as classes mais importantes; e **percorrer o código** para
quantificar sua qualidade e sua cobertura de testes.

O primeiro passo existe porque operar sobre o sistema em produção arriscaria dados de clientes e a
experiência dos usuários. A ramificação de rascunho nunca será integrada à linha principal e serve
apenas para experimentar (FOX; PATTERSON, 2021, p. 273). Para o banco de desenvolvimento, os autores
sugerem clonar o de produção, quando possível, ou extrair dele *fixtures* com o mesmo esquema. Com a
aplicação no ar, recomendam que um ou dois clientes experientes demonstrem o uso, comentando em voz alta
o que fazem e que mudanças desejam (FOX; PATTERSON, 2021, p. 274). Feathers (2004, cap. 16) propõe uma
técnica afim, a **refatoração de rascunho** (*scratch refactoring*): refatorar livremente para entender,
sabendo que todo o trabalho será descartado.

O passo do esquema reflete uma observação que os autores atribuem a Brooks e a Rob Pike: compreender as
estruturas de dados é a chave para compreender a lógica da aplicação (FOX; PATTERSON, 2021, p. 274). Um
diagrama de classes gerado automaticamente ajuda a identificar as classes **altamente conectadas**, que
provavelmente são centrais para as funções da aplicação. Em seguida, antes de abrir arquivos
específicos, convém obter estatísticas de linhas de código e de testes por arquivo, que indicam as
classes mais complexas, as mais bem testadas e as auxiliares; executar a suíte de testes e ler os
testes para entender as intenções dos desenvolvedores originais; e, então, dedicar cerca de uma hora,
recomendação que os autores tomam de Nierstrasz, Ducasse e Demeyer (2009), à inspeção das classes mais
importantes e dos pontos de mudança (FOX; PATTERSON, 2021, p. 275).

### C.3.2 Cartões CRC

Para registrar o que se descobre, Fox e Patterson (2021, p. 276-277) sugerem os **cartões CRC**
(classe, responsabilidades, colaboradores), propostos em 1989 como apoio ao projeto orientado a objetos.
Cada cartão identifica uma classe, à esquerda suas responsabilidades, o que ela sabe e o que ela faz, e
à direita as classes com que colabora. Na exploração de código legado, os cartões documentam as
classes encontradas ao seguir o fluxo de uma história, da ação do *controller* que trata um passo
até os modelos e as visões envolvidos. O cartão é deliberadamente pequeno: se as responsabilidades
não cabem nele, a classe provavelmente tem responsabilidades demais, o que já é uma descoberta.

### C.3.3 Comentários e *commits* como documentação

Fox e Patterson (2021, p. 279-281) observam que, no código legado, os comentários costumam faltar ou
contradizer o código, e propõem que, ao concluir testes de caracterização, o explorador registre o que
aprendeu em comentários. Bons comentários documentam o que não é óbvio no código, como unidades,
invariantes e contornos de defeitos, e estão num nível de abstração acima dele. As mensagens de
*commit* seguem orientação semelhante, com uma divisão de papéis: a informação histórica, como por que
uma função foi removida, pertence ao *commit*; a informação de que alguém precisa para usar o código
como está pertence ao comentário, onde não pode deixar de ser vista. A divisão é a base da arqueologia
de repositórios da Seção C.5, que só funciona quando o histórico contém o "porquê".

## C.4 Estabelecer a verdade: testes de caracterização e costuras

### C.4.1 Testes de caracterização

Um **teste de caracterização** é escrito depois do fato e captura o comportamento atual de um trecho de
código, inclusive seus defeitos, para que esse comportamento possa ser preservado enquanto o código
é modificado (FOX; PATTERSON, 2021, p. 277). O objetivo não é verificar se o código está correto, mas
fixar uma **verdade de referência** (*ground truth*). Feathers (2004, cap. 13) descreve a técnica para
código que ainda não se entende: escrever um teste com uma asserção que provavelmente falhará, executá-lo
e usar a mensagem de erro para ajustar a asserção ao comportamento real, repetindo até obter cobertura
suficiente (FOX; PATTERSON, 2021, p. 278). No exemplo dos autores, a caracterização de um método de
conversão de dias em anos revela um defeito semelhante ao que travou tocadores Zune da Microsoft em
31 de dezembro de 2008.

Fox e Patterson (2021, p. 277) recomendam começar por testes de caracterização **de integração**, que
fazem poucas suposições sobre o funcionamento interno e capturam apenas o comportamento visível, e só
depois descer aos testes de unidade. Testes antigos que falham devem ser marcados como pendentes, com
um comentário, em vez de corrigidos antes de o código ser entendido (FOX; PATTERSON, 2021, p. 279). A
Listagem C.1 aplica a técnica em Java 17.

**Listagem C.1** – Testes de caracterização com JUnit 5 (ilustrativo e simplificado).

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Código legado: ninguém sabe ao certo de onde vem a regra de desconto regional.
class CalculadoraDeFrete {
    double calcular(double pesoKg, String uf) {
        double base = pesoKg <= 1 ? 12.0 : 12.0 + (Math.ceil(pesoKg) - 1) * 3.5;
        if (uf.equals("AL") || uf.equals("PE")) base *= 0.9;
        return Math.round(base * 100) / 100.0;
    }
}

class CalculadoraDeFreteCaracterizacaoTest {
    private final CalculadoraDeFrete calc = new CalculadoraDeFrete();

    // 1. Escreveu-se 0.0 como valor esperado; 2. a falha mostrou o valor real;
    // 3. o valor observado foi copiado para cá. O teste registra o que o código FAZ.
    @ParameterizedTest
    @CsvSource({"0.5, AL, 10.8", "1.0, PE, 10.8", "2.3, SP, 19.0", "0.0, SP, 12.0"})
    void preservaComportamentoAtual(double peso, String uf, double esperado) {
        assertEquals(esperado, calc.calcular(peso, uf), 0.001);
    }

    // Comportamento questionável, mas atual: UF nula lança exceção.
    // Corrigi-lo é outra mudança, com outro teste.
    @Test
    void ufNulaLancaExcecao() {
        assertThrows(NullPointerException.class, () -> calc.calcular(1.0, null));
    }
}
```

Fonte: elaborado pelo autor (2026), com base na técnica de Feathers (2004, cap. 13) descrita por Fox
e Patterson (2021, p. 278).

Os testes de caracterização diferem dos testes de unidade convencionais quanto aos objetivos. Ambos
ampliam a cobertura e documentam o comportamento, mas testar condições de contorno e prevenir a volta
de defeitos antigos são objetivos dos testes convencionais, e não dos de caracterização (FOX;
PATTERSON, 2021, p. 279). Por isso, um teste de caracterização que fixa um comportamento defeituoso,
como o segundo teste da Listagem C.1, é correto enquanto o defeito não for deliberadamente corrigido.

### C.4.2 Costuras

Para escrever testes em código que não foi feito para ser testado, é preciso controlar suas
dependências. Feathers define uma **costura** (*seam*) como um lugar em que se pode alterar o
comportamento do programa sem editar naquele lugar (FEATHERS, 2004 *apud* FOX; PATTERSON, 2021, p.
244). Toda costura tem um **ponto de habilitação**, o lugar em que se decide qual comportamento usar.
Feathers (2004, cap. 4) distingue costuras de **pré-processamento**, típicas de C e C++, costuras de
**ligação**, em que se substitui uma biblioteca na compilação ou na carga, e costuras de **objeto**, em
que se substitui uma implementação por polimorfismo, as mais comuns em linguagens orientadas a objetos.
Fox e Patterson (2021, p. 286) dedicam a Seção 9.6 à refatoração de métodos para criar costuras, com
operações do catálogo de Fowler como extrair método e substituir número mágico por constante.

A Listagem C.2 mostra uma costura de objeto típica: a dependência oculta do relógio do sistema impede
testar o comportamento de um dia específico, e um `Clock` injetado resolve o problema sem alterar os
chamadores existentes.

**Listagem C.2** – Criação de uma costura de objeto (ilustrativo e simplificado).

```java
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;

// Depois da refatoração. Antes, o método chamava LocalDate.now() diretamente,
// e não havia como testar o multiplicador de sexta-feira sem esperar a sexta-feira.
class TarifaDinamica {
    private final Clock relogio;                       // a costura

    TarifaDinamica(Clock relogio) {                     // ponto de habilitação nos testes
        this.relogio = relogio;
    }

    TarifaDinamica() {                                  // preserva os chamadores atuais
        this(Clock.systemDefaultZone());
    }

    double multiplicador() {
        return LocalDate.now(relogio).getDayOfWeek() == DayOfWeek.FRIDAY ? 1.5 : 1.0;
    }
}
```

Fonte: elaborado pelo autor (2026), com base em Feathers (2004, cap. 4).

Quando não há tempo para cobrir o código existente, Feathers (2004, cap. 6) propõe técnicas para ao
menos testar o código novo: o **método** ou a **classe broto** (*sprout*), em que a funcionalidade nova
é escrita e testada separadamente e apenas chamada do código antigo, e o **método** ou a **classe
envoltório** (*wrap*), em que o comportamento antigo é envolvido pelo novo. Ambas limitam o
crescimento da área não testada sem exigir que ela seja coberta de imediato.

## C.5 Arqueologia de repositórios

### C.5.1 O histórico como fonte

O código mostra o estado atual do sistema; o histórico do controle de versões mostra como ele chegou
lá. Cada *commit* registra o que mudou, quando, por quem e, se a equipe for disciplinada, por quê. Para
o arquiteto que explora um sistema legado, o histórico responde a perguntas que o código não responde:
que partes mudam com frequência, que arquivos mudam juntos, quem conhece cada parte, quando e por que um
trecho surgiu. Fox e Patterson (2021, p. 271) incluem as mensagens de *commit* entre as formas de
documentação "próximas do código" que métodos ágeis valorizam.

Tornhill (2015) sistematizou essa leitura por analogia com a criminologia: assim como a análise
geográfica de crimes concentra a investigação nas áreas em que os delitos se repetem, a análise do
histórico concentra a atenção nas partes do código em que o esforço e os defeitos se acumulam. O autor
organiza a análise em três frentes, que correspondem às três partes de seu livro: a evolução do código,
a arquitetura e os aspectos sociais do desenvolvimento.

### C.5.2 *Hotspots*

Um ***hotspot*** é um arquivo que combina alta **frequência de mudança** com alta **complexidade**
(TORNHILL, 2015). A frequência vem do histórico; a complexidade pode ser estimada de forma simples,
pelo número de linhas, ou por métricas como a complexidade baseada em indentação, que o autor propõe
por ser independente de linguagem. A justificativa é econômica: código complexo que nunca muda custa
pouco, por mais feio que seja; código complexo que muda toda semana custa caro e concentra o risco de
defeitos. Como a distribuição das mudanças costuma ser muito desigual, poucos arquivos concentram a
maior parte delas, e a análise de *hotspots* indica onde a refatoração, os testes de caracterização e
a leitura cuidadosa rendem mais. Para o arquiteto que precisa escolher onde pousar, os *hotspots* são
candidatos naturais a pontos de mudança.

### C.5.3 Acoplamento temporal e conhecimento

O **acoplamento temporal** (ou de mudança) ocorre quando dois arquivos mudam juntos com frequência nos
mesmos *commits* (TORNHILL, 2015). Ele revela dependências que a análise estática não vê, como entre um
modelo e o serializador que o expõe, entre um cliente e o servidor de um protocolo, ou entre um teste e
o código que ele testa, e serve para avaliar a arquitetura: se arquivos de componentes que deveriam ser
independentes mudam sempre juntos, a fronteira desenhada no diagrama não corresponde à fronteira real.
A análise social, por sua vez, usa os autores dos *commits* para mapear quem conhece cada parte do
código, identificar partes que dependem de uma única pessoa e verificar a correspondência entre a
estrutura do código e a das equipes, em diálogo com a Lei de Conway (Seção 7.2.3).

### C.5.4 Comandos

A Listagem C.3 reúne comandos Git para uma primeira arqueologia, usando o Mastodon como exemplo. O
último comando produz o formato de entrada da ferramenta *code-maat*, de Tornhill, que calcula
frequências, acoplamentos temporais e mapas de autoria (TORNHILL, 2015).

**Listagem C.3** – Comandos de arqueologia de repositório (exemplo com o Mastodon).

```bash
# 1. Clonar com histórico completo e fixar a versão estudada
git clone https://github.com/mastodon/mastodon.git && cd mastodon
git checkout v4.7.3

# 2. Tamanho por linguagem
cloc --vcs=git .

# 3. Arquivos mais alterados no ano anterior à tag (candidatos a hotspot)
git log v4.7.3 --since="12 months ago" --format=format: --name-only \
  | grep -v '^$' | sort | uniq -c | sort -rn | head -20

# 4. Quem conhece um diretório: autores por número de commits
git shortlog -sn --no-merges v4.7.3 -- app/services/

# 5. História de um arquivo, inclusive através de renomeações
git log --follow --oneline v4.7.3 -- app/services/fan_out_on_write_service.rb

# 6. Quando e por que um trecho apareceu ou mudou (pickaxe)
git log -S "MAX_ITEMS" --oneline v4.7.3 -- app/lib/feed_manager.rb

# 7. Entrada para o code-maat
git log v4.7.3 --pretty=format:'[%h] %an %ad %s' --date=short --numstat > evolucao.log
```

Fonte: elaborado pelo autor (2026). Os caminhos referem-se à *tag* v4.7.3, conforme a Seção 6.2.

O histórico também tem armadilhas. *Commits* de reformatação em massa, migrações de linguagem e
movimentações de diretórios inflam a frequência de mudança de arquivos que não mudaram em substância.
Arquivos gerados, como esquemas e traduções, aparecem como *hotspots* falsos. A integração por
*squash* reduz vários *commits* a um só, o que distorce o acoplamento temporal. E a "data de doze meses"
mede coisas diferentes num projeto que lança versões semanais e num que lança versões anuais. Os
números do histórico, como as métricas de código discutidas por Fox e Patterson (2021, p. 281-283), são
indicadores para orientar a leitura, e não conclusões.

O interativo a seguir desenha o mapa de *hotspots* a partir da saída do comando 7 da Listagem C.3.
Cada arquivo é posicionado pela frequência de mudança, contada como o número de *commits* que o
alteraram na janela escolhida, e pelo tamanho, estimado como a soma das linhas adicionadas menos a
das removidas em todo o histórico, aproximação da complexidade que Tornhill (2015) admite. A
estimativa só é fiel quando o log começa na criação dos arquivos, e por isso os comandos devem ser
executados sobre um clone com histórico completo. O exemplo pré-carregado usa caminhos do Mastodon
v4.7.3 citados nos Capítulos 2 a 6, mas seus números são hipotéticos e servem apenas para
ilustrar a leitura; a opção de ocultar arquivos gerados permite observar como esquemas, traduções e
arquivos de travamento de dependências se apresentariam como *hotspots* falsos.

<iframe src="../interativos/hotspots.html" title="Mapa de hotspots a partir do histórico do Git"
  style="width:100%; height:660px; border:1px solid #2A4468; border-radius:10px;"
  loading="lazy"></iframe>

## C.6 O roteiro de expedição

### C.6.1 Os dez passos

O roteiro a seguir consolida as técnicas deste apêndice e as organiza pelas altitudes do livro. Ele
adapta os passos de fora para dentro de Fox e Patterson (2021, seção 9.2) e o algoritmo de mudança de
Feathers (2004, cap. 2), acrescentando a arqueologia de repositórios e os produtos de cada capítulo. A
Tabela C.1 resume os passos.

**Tabela C.1** – Roteiro de expedição em dez passos.

| Passo | Ação | Altitude | Produto |
|---|---|---|---|
| 1 | **Fixar a versão.** Escolher uma *tag* estável, clonar com histórico completo e usar *permalinks* com a *tag* em toda citação de código. | Todas | Referência reprodutível |
| 2 | **Ler a documentação** de arquitetura, de instalação e de administração, e as licenças. | <span class="altitude a100">100.000 pés</span> | Diagrama de contexto preliminar |
| 3 | **Rodar o sistema** num ambiente descartável, de preferência com a implantação oficial, e reproduzir como usuário as histórias principais. | <span class="altitude a100">100.000 pés</span> | Lista de histórias e operação-chave |
| 4 | **Ler a implantação**: arquivos de Docker Compose, manifestos e configuração. | <span class="altitude a50">50.000 pés</span> | Diagrama de *containers* |
| 5 | **Mapear a árvore** até o segundo ou terceiro nível e identificar o particionamento, técnico ou por domínio. | <span class="altitude a10">10.000 pés</span> | Hipóteses de componentes |
| 6 | **Ler o esquema de dados**: migrações, modelos, definições de mensagens. | <span class="altitude a10">10.000 pés</span> | Entidades centrais |
| 7 | **Medir**: linhas por linguagem, *hotspots*, acoplamento temporal, dependências entre pacotes. | <span class="altitude a10">10.000 pés</span> | Lista priorizada de pontos de pouso |
| 8 | **Seguir uma requisição** da operação-chave, da rota ao armazenamento e de volta, lendo os testes do caminho. | <span class="altitude solo">Solo</span> | Diagrama dinâmico; cartões CRC |
| 9 | **Caracterizar** com testes o comportamento do ponto de mudança, quando houver intenção de alterá-lo. | <span class="altitude solo">Solo</span> | Testes de caracterização |
| 10 | **Registrar e subir**: fichas *Known Uses*, ADRs reconstruídos e a subida de verificação. | Todas | Fichas, ADRs, tabela de subida |

Fonte: elaborado pelo autor (2026), com base em Fox e Patterson (2021, cap. 9), Feathers (2004) e
Tornhill (2015).

O roteiro é uma descida de altitude aplicada ao código: os passos 2 e 3 constroem as hipóteses de
domínio, os passos 4 a 7 constroem as hipóteses estruturais, e só o passo 8 abre arquivos com
intenção de lê-los linha a linha. A ordem protege contra o erro mais comum de quem chega a um
repositório grande, que é começar pelo primeiro arquivo do primeiro diretório e perder-se em
detalhes antes de saber o que procura. O passo 10 fecha o ciclo com a subida de verificação da Seção
6.1.4, em que cada trecho lido é associado a um componente, a uma decisão e a uma característica.

### C.6.2 O roteiro aplicado aos sete sistemas

A Tabela C.2 mostra o ponto de partida de cada expedição feita no livro: a versão fixada, a fonte
principal para a altitude de *containers*, a operação-chave seguida no passo 8 e o primeiro arquivo
do caminho. Todos os dados vêm das seções indicadas.

**Tabela C.2** – O roteiro de expedição aplicado aos sete sistemas.

| Sistema | Versão | Fonte para os *containers* (passo 4) | Operação-chave (passo 8) | Primeiro arquivo do caminho | Seções |
|---|---|---|---|---|---|
| Mastodon | `v4.7.3` | `docker-compose.yml` | Publicar | `app/controllers/api/v1/statuses_controller.rb` | 4.2, 5.2, 6.2 |
| Mattermost | `v11.11.1` | Documentação de alta disponibilidade | Publicar uma mensagem | `server/channels/api4/post.go` | 4.3, 5.3, 6.3 |
| PeerTube | `v8.3.1` | Docker Compose de referência | Do *upload* à publicação | `server/core/controllers/api/videos/upload.ts` | 4.4, 5.4, 6.4 |
| Nextcloud | `v35.0.2` | Nextcloud All-in-One | Enviar um arquivo por WebDAV | `lib/public/Files/Storage/IStorage.php` | 4.5, 5.5, 6.5 |
| Saleor | `3.23.42` | `pyproject.toml`, `Dockerfile` e `docker-compose.yml` do `saleor-platform` | Concluir o *checkout* | `saleor/checkout/complete_checkout.py` | 4.6, 5.6, 6.6 |
| ThingsBoard | `v4.3.1.6` | Documentação da topologia de microsserviços | Ingerir telemetria | `common/transport/transport-api/.../DefaultTransportService.java` | 4.7, 5.7, 6.7 |
| PX4 | `v1.17.0` | Documentação de arquitetura e da ponte uXRCE-DDS | Um ciclo do laço de controle | arquivos `.msg` em `msg/` | 4.8, 5.8, 6.8 |

Fonte: elaborado pelo autor (2026), com base nos Capítulos 4 a 6.

A tabela evidencia que o passo 4 depende do tipo de sistema. Em aplicações web auto-hospedadas, como o
Mastodon e o PeerTube, o arquivo de Docker Compose é a fonte mais confiável para o diagrama de
*containers*, porque é executável e, portanto, não pode estar desatualizado em relação à versão. No
PX4, os *containers* são nós físicos e processos em computadores distintos, e a fonte é a documentação
de arquitetura do projeto. No PX4, também o passo 6 muda de natureza: o "esquema" não está em migrações de banco,
mas nas definições de mensagens uORB, que a Seção 6.8.1 trata como o modelo de domínio do sistema. A
escolha do primeiro arquivo, por sua vez, segue a operação-chave: no Mastodon, o *controller* de
publicações; no Nextcloud, o contrato de armazenamento, porque a operação atravessa a camada WebDAV
até chegar a ele.

## C.7 A ficha *Known Uses*

Gamma *et al.* (1994, cap. 1) descrevem cada padrão com um conjunto fixo de seções, entre as quais
*Known Uses*, os usos conhecidos em sistemas reais. A Seção 6.1.3 explicou por que este livro
atualiza essa prática com sistemas contemporâneos. A Listagem C.4 é o modelo da ficha.

**Listagem C.4** – Modelo da ficha *Known Uses*.

```markdown
### <Padrão> em <Projeto> (<tag>)

- **Onde:** <permalink para o arquivo e as linhas, com a tag>
- **Participantes:** <papel no padrão → classe, interface ou arquivo real>
- **Problema que resolve aqui:** <por que o projeto precisou do padrão>
- **Característica arquitetural que protege:** <ligação com o Top-3 do Capítulo 3>
- **Decisão relacionada:** <ADR reconstruído, se houver>
- **Consequências (trade-offs):** <o que ganha e o que paga, neste projeto>
- **Desvios do padrão canônico:** <o que difere da descrição do GoF ou de Fowler>
```

Fonte: elaborado pelo autor (2026), com base em Gamma *et al.* (1994, cap. 1).

A ficha acrescenta dois campos ao modelo anterior do curso. A **decisão relacionada** liga o padrão,
na altitude do solo, ao ADR que ele concretiza, como pede a subida de verificação. Os **desvios do
padrão canônico** registram que padrões em código real raramente aparecem na forma do catálogo, e que
a diferença é, ela própria, informação arquitetural. A ficha a seguir, preenchida a partir da Seção
6.5.6, serve de exemplo.

!!! example "*Decorator* em Nextcloud (`v35.0.2`)"
    - **Onde:** [`lib/private/Files/Storage/Wrapper/Wrapper.php`](https://github.com/nextcloud/server/blob/v35.0.2/lib/private/Files/Storage/Wrapper/Wrapper.php),
      com decoradores concretos em `Quota.php` e `Encryption.php` no mesmo diretório.
    - **Participantes:** componente → `IStorage` (`lib/public/Files/Storage/IStorage.php`);
      componente concreto → as implementações de armazenamento, com base em `Common.php`;
      decorador → `Wrapper`; decoradores concretos → `Quota`, `Encryption`; montagem da cadeia →
      `SetupManager`.
    - **Problema que resolve aqui:** acrescentar cotas, criptografia e outras políticas a qualquer
      armazenamento, local ou externo, sem modificar cada implementação.
    - **Característica que protege:** extensibilidade e segurança, do Top-3 da Seção 3.5.7.
    - **Decisão relacionada:** ADR-001 (Seção 4.5.7): funcionalidades entregues como *apps*, que
      podem registrar seus próprios *wrappers*.
    - **Consequências:** políticas combináveis e independentes do *backend*; em troca, a ordem da
      cadeia importa e a depuração atravessa várias camadas de indireção.
    - **Desvios:** a cadeia não é montada pelo cliente, como no exemplo canônico, mas por funções
      registradas com nome e prioridade no `SetupManager`, o que a aproxima de um pequeno
      microkernel; e os decoradores recebem o componente num *array* de parâmetros
      (`['storage' => $storage, ...]`), e não como argumento tipado (Seção 6.5.6).

## C.8 Ferramentas

A escolha de ferramentas depende da linguagem e do passo do roteiro. A Tabela C.3 lista ferramentas
consolidadas por categoria; as citadas por Fox e Patterson são as do ecossistema Ruby on Rails, útil
diretamente para o Mastodon.

**Tabela C.3** – Ferramentas para a exploração de código legado.

| Categoria | Ferramentas | Passos | Observação |
|---|---|---|---|
| Busca textual | `git grep`, ripgrep, busca de código do GitHub | 5, 8 | `git grep` busca na versão fixada, sem arquivos não versionados |
| Navegação semântica | IDE com "ir para definição" e "encontrar usos" | 8 | Essencial em linguagens com inferência de tipos ou metaprogramação |
| Tamanho e composição | `cloc`, `scc`; `rails stats` em aplicações Rails | 7 | Fox e Patterson (2021, p. 275) usam as estatísticas do Rails |
| Histórico | `git log`, `git blame`, `git bisect`, *code-maat* | 7, 8 | Ver Seção C.5 |
| Dependências | `jdeps` (JDK), ArchUnit (Java); `go mod graph` (Go); analisadores de *imports* em Python e TypeScript | 5, 7 | ArchUnit também verifica regras (Apêndice E) |
| Qualidade | SonarQube; `flog`, `reek` e SimpleCov em Ruby | 7 | Métricas orientam a leitura; ver Fox e Patterson (2021, p. 281-285) |
| Diagramas a partir do código | `railroady` (Rails), geradores de diagramas de classes das IDEs | 6 | Fox e Patterson (2021, p. 274-275) |
| Execução e observação | Docker Compose, depuradores, *logs* em nível de depuração | 3, 8 | Sempre em ambiente descartável |
| Registro | Mermaid, Structurizr (Apêndice A), cartões CRC | 8, 10 | Diagramas como código versionados com as notas |

Fonte: elaborado pelo autor (2026), com base em Fox e Patterson (2021, cap. 9) e Tornhill (2015).

Duas advertências acompanham a tabela. A primeira é que métricas estáticas são distorcidas por
metaprogramação: Fox e Patterson (2021, p. 285) observam que mecanismos dinâmicos do Ruby podem alterar
o grafo de controle em tempo de execução, o que vale também para os *callbacks* do Active Record do
Mastodon e para a injeção de dependências por anotações do Spring no ThingsBoard. A segunda é que
nenhuma ferramenta substitui a pergunta que orienta a busca. Um *hotspot* só é relevante se estiver no
caminho da operação-chave ou de uma decisão em estudo.

## C.9 Ética de contribuição em projetos de código aberto

Explorar um repositório público não exige permissão; contribuir com ele cria obrigações. A primeira é
**respeitar a licença**. Os sete sistemas usam licenças diferentes, descritas no Capítulo 2: o
Mastodon, o PeerTube e o núcleo do servidor do Nextcloud usam a AGPL-3.0; o Mattermost combina AGPL v3.0
no núcleo e Apache 2.0 em partes do código, num modelo *open core* (Seção 2.3); o Saleor e o PX4 usam a
BSD de três cláusulas; e o ThingsBoard é estudado na série 4.3, sob Apache 2.0, porque a série 4.4
adotou a *Business Source License* (Seção 2.7). A distinção tem consequências práticas. A AGPL estende
o *copyleft* da GPL ao uso pela rede: quem modifica o programa e o oferece a usuários que interagem com
ele remotamente deve oferecer-lhes o código-fonte da versão modificada (FREE SOFTWARE FOUNDATION,
2007, seção 13). Uma instância de Mastodon ou de PeerTube modificada para um trabalho de disciplina e
aberta ao público está sujeita a essa obrigação.

A segunda obrigação é **respeitar o tempo dos mantenedores**. Projetos de código aberto são mantidos,
em boa parte, por poucas pessoas, e cada *issue* ou *pull request* consome atenção. Antes de contribuir,
o leitor deve ler o guia de contribuição e o código de conduta do projeto, quando existirem, procurar
*issues* e discussões anteriores sobre o mesmo tema e seguir o processo indicado, que pode exigir
discussão prévia, assinatura de acordos de contribuição ou testes específicos. Argumenta-se aqui que
atividades de disciplina não devem gerar contribuições em massa a projetos reais: um *pull request*
deve resolver um problema que os mantenedores reconheçam, e não cumprir uma exigência de avaliação.
Os exercícios deste livro, por isso, são resolvidos em cópias locais ou em versões didáticas, como as
do Capítulo 6.

A terceira obrigação diz respeito a **vulnerabilidades**. Quem encontra uma possível falha de segurança
durante a exploração não deve publicá-la numa *issue* aberta, mas seguir a política de divulgação
coordenada do projeto, em geral descrita num arquivo de política de segurança ou no canal privado de
relato de vulnerabilidades da plataforma de hospedagem, dando aos mantenedores tempo para corrigir o
problema antes da divulgação.

A quarta obrigação é de **atribuição e honestidade intelectual**. Trechos de código citados devem vir
com *permalink* e licença; reconstruções, como os ADRs deste livro, devem ser declaradas como tais (Seção
B.9); e críticas ao projeto devem distinguir fatos verificáveis de interpretações. O registro respeitoso
das decisões alheias, inclusive das que parecem equivocadas, é coerente com o princípio que Fox e
Patterson (2021, p. 32) enunciam ao abrir o tema: software legado é software bem-sucedido, e suas
imperfeições são, em boa parte, o preço desse sucesso.

## Síntese do apêndice

Código legado é, ao mesmo tempo, software bem-sucedido, porque continua em uso, e software arriscado de
modificar, porque não tem testes suficientes. A exploração desse código é um problema de compreensão
de programas, e as teorias de Brooks, Pennington e Von Mayrhauser e Vans explicam por que convém
construir hipóteses de domínio e de estrutura antes de ler o código linha a linha, alternando entre
leitura descendente e ascendente conforme a familiaridade com o trecho. Fox e Patterson traduzem essa
orientação num roteiro de fora para dentro, com execução em ambiente descartável, histórias de usuário,
esquema de dados, métricas e cartões CRC. Feathers oferece as técnicas para sair do dilema do código
legado: testes de caracterização para fixar a verdade de referência e costuras para isolar
dependências.

A arqueologia de repositórios acrescenta a dimensão temporal: *hotspots*, acoplamento temporal e mapas
de conhecimento indicam onde a leitura rende mais e onde as fronteiras arquiteturais não correspondem
às fronteiras reais de mudança. O roteiro de expedição em dez passos integra essas técnicas às
altitudes do livro e foi aplicado aos sete sistemas, com resultados registrados em fichas *Known
Uses*, ADRs reconstruídos e subidas de verificação. Por fim, a exploração de projetos de código aberto
tem dimensão ética: respeitar licenças, o tempo dos mantenedores, as políticas de segurança e a autoria
é parte do trabalho do arquiteto.

## Exercícios

1. Explique a diferença entre a definição de código legado de Fox e Patterson e a de Feathers. Um
   sistema novo pode ser legado?
2. Compare as teorias de Brooks e de Pennington. Em que situação de exploração cada estratégia é mais
   adequada?
3. Aplique os passos 1 a 4 do roteiro a um dos sete sistemas e compare o diagrama de *containers*
   obtido com o do Capítulo 4.
4. Execute os comandos 3 e 4 da Listagem C.3 sobre o repositório de outro dos sete sistemas. Os
   arquivos mais alterados coincidem com os componentes destacados no Capítulo 5? Explique as
   diferenças.
5. Identifique, no histórico de um dos sistemas, um par de arquivos com acoplamento temporal e discuta
   se ele indica um problema arquitetural.
6. Escreva testes de caracterização em Java 17 para um método de um projeto seu sem testes, seguindo a
   técnica da Listagem C.1.
7. Identifique uma dependência oculta num código Java seu e crie uma costura de objeto, indicando o
   ponto de habilitação.
8. Construa cartões CRC para as cinco classes mais conectadas do caminho da operação-chave de um dos
   sistemas.
9. Preencha uma ficha *Known Uses* para um padrão identificado no Capítulo 6 que não tenha ficha
   completa.
10. Leia o guia de contribuição de um dos sete projetos e descreva, passo a passo, como uma correção
    de documentação deveria ser proposta.
11. Discuta as obrigações da AGPL-3.0 para uma instância de PeerTube modificada e aberta ao público por
    uma instituição de ensino.

## Glossário do apêndice

`Código legado`
:   Código que continua em uso por atender a necessidades reais; na definição de Feathers, código sem
    testes suficientes para ser modificado com confiança.

`Teste de caracterização`
:   Teste escrito depois do fato para capturar o comportamento atual do código, inclusive defeitos,
    como verdade de referência.

`Costura (seam)`
:   Lugar em que se pode alterar o comportamento do programa sem editar naquele lugar.

`Ponto de habilitação`
:   Lugar em que se decide qual comportamento uma costura usará.

`Ponto de mudança`
:   Lugar do código em que uma alteração pretendida precisará ser feita.

`Hotspot`
:   Arquivo que combina alta frequência de mudança e alta complexidade.

`Acoplamento temporal`
:   Tendência de dois ou mais arquivos mudarem juntos nos mesmos *commits*.

`Cartão CRC`
:   Cartão que registra uma classe, suas responsabilidades e seus colaboradores.

## Referências

BROOKS, R. Towards a theory of the comprehension of computer programs. **International Journal of
Man-Machine Studies**, v. 18, n. 6, p. 543-554, 1983.

FEATHERS, M. **Working effectively with legacy code**. Upper Saddle River: Prentice Hall, 2004.

FOX, A.; PATTERSON, D. **Engineering software as a service**: an agile approach using cloud
computing. 2. ed., versão 2.0b7. [*S. l.*]: Strawberry Canyon, 2021.

FREE SOFTWARE FOUNDATION. **GNU Affero General Public License**: version 3. 2007. Disponível em:
<https://www.gnu.org/licenses/agpl-3.0.html>. Acesso em: 18 out. 2026.

GAMMA, E. *et al.* **Design patterns**: elements of reusable object-oriented software. Reading:
Addison-Wesley, 1994.

LIENTZ, B. P.; SWANSON, E. B.; TOMPKINS, G. E. Characteristics of application software maintenance.
**Communications of the ACM**, v. 21, n. 6, p. 466-471, 1978.

NIERSTRASZ, O.; DUCASSE, S.; DEMEYER, S. **Object-oriented reengineering patterns**. Bern: Square
Bracket Associates, 2009.

PENNINGTON, N. Stimulus structures and mental representations in expert comprehension of computer
programs. **Cognitive Psychology**, v. 19, n. 3, p. 295-341, 1987.

TORNHILL, A. **Your code as a crime scene**: use forensic techniques to arrest defects,
bottlenecks, and bad design in your programs. Raleigh: The Pragmatic Bookshelf, 2015.

VON MAYRHAUSER, A.; VANS, A. M. Program comprehension during software maintenance and evolution.
**Computer**, v. 28, n. 8, p. 44-55, 1995.

WASHIZAKI, H. (ed.). **Guide to the software engineering body of knowledge (SWEBOK Guide)**:
version 4.0. Los Alamitos: IEEE Computer Society, 2024.

## Material complementar

!!! todo "Para desenvolver"
    Roteiro guiado, em vídeo, da execução dos passos 1 a 7 sobre o repositório do Mastodon.
