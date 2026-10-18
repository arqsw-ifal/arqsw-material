# Segurança do corretor automático

O corretor **executa código do aluno**: os testes JUnit e ArchUnit carregam e rodam as classes
dele. Esse código não é confiável. As defesas são em camadas:

| Ameaça | Defesa |
|---|---|
| **Escrever no repositório / roubar o token** | `permissions: contents: read` no workflow: o token do job é somente-leitura. |
| **Laço infinito / trava** | `timeout -k 5 300s` de parede em cada chamada ao Maven + `ulimit -t 240` de CPU + `timeout-minutes: 10` no job. |
| **Encher o disco** | `ulimit -f` (200 MB por arquivo). |
| **Bomba de memória** | A JVM tem *heap* máximo padrão (¼ da RAM do runner). **Não** usamos `ulimit -v`: a JVM reserva um espaço de endereçamento enorme e não subiria. |
| **Persistência entre envios** | O runner do GitHub Actions é efêmero: cada execução começa de uma máquina limpa. |

## A fraqueza que sobra: o aluno controla o repositório

No modelo atual o repositório do aluno contém **os testes e o corretor**. Um aluno mal
intencionado pode editar `src/test/` ou `scripts/run_tests.sh` para se dar 100. Por isso:

1. o README do exercício pede para não alterar esses arquivos (e o histórico do Git denuncia);
2. **numa avaliação valendo nota**, configure o autograder do Classroom 50 para **copiar os
   testes e o corretor do professor** por cima dos do aluno antes de rodar (ou rode-os a partir
   do repositório de configuração), compilando **apenas** o `src/main/` do aluno;
3. confira por amostragem o `git log -- src/test scripts` dos envios.

## Dependências

O `pom.xml` fixa as versões (JUnit 5.10.2, ArchUnit 1.3.0, plugins Maven). Atualize-as
deliberadamente, nunca com intervalos de versão.
