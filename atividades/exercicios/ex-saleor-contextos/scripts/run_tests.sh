#!/usr/bin/env bash
#
# Corretor automatico de exercicios de Arquitetura de Software (Java + Maven).
#
#   ./scripts/run_tests.sh               -> roda tudo e imprime a nota 0-100 (uso do aluno)
#   ./scripts/run_tests.sh compila       -> so a compilacao              (exit 0 = passou)
#   ./scripts/run_tests.sh comportamento -> so os testes de comportamento (exit 0 = todos passaram)
#   ./scripts/run_tests.sh arquitetura   -> so as fitness functions       (exit 0 = todas passaram)
#   ./scripts/run_tests.sh json          -> relatorio em JSON no stdout (consumido pelo autograder)
#
# A nota tem tres partes:
#   - compilacao                                    (PTS_COMPILA)
#   - testes de COMPORTAMENTO  (*ComportamentoTest) (PTS_COMPORTAMENTO, proporcional)
#   - testes de ARQUITETURA    (*ArquiteturaTest)   (PTS_ARQUITETURA, proporcional)
# Os testes de arquitetura sao "fitness functions" (Richards & Ford, cap. 6) escritas com ArchUnit.

set -uo pipefail

MODO="${1:-tudo}"

PTS_COMPILA=10
PTS_COMPORTAMENTO=50
PTS_ARQUITETURA=40

MVN="${MVN:-mvn}"
MVN_OPTS="-B -q -Dstyle.color=never"
REL="target/surefire-reports"

titulo() { printf '\n\033[1m== %s ==\033[0m\n' "$1" >&2; }
ok()     { printf '  \033[32m✓\033[0m %s\n' "$1" >&2; }
erro()   { printf '  \033[31m✗\033[0m %s\n' "$1" >&2; }

# SEGURANCA: o corretor executa codigo do ALUNO (os testes rodam as classes dele).
# Limites de CPU e de tamanho de arquivo + timeout de parede. Nao limitamos memoria
# virtual com ulimit -v: a JVM reserva muito espaco de enderecamento e nao subiria.
sandbox() {
  (
    ulimit -t 240     2>/dev/null || true
    ulimit -f 204800  2>/dev/null || true
    exec "$@"
  )
}

# ------------------------------------------------------------------ 1. compila
compilar() {
  titulo "1. Compilacao"
  if sandbox timeout -k 5 300s $MVN $MVN_OPTS -DskipTests test-compile > target-compile.log 2>&1; then
    ok "compilou"
    rm -f target-compile.log
    return 0
  fi
  erro "nao compilou"
  grep -E 'ERROR|error:' target-compile.log | head -15 | sed 's/^/      /' >&2
  rm -f target-compile.log
  return 1
}

# --------------------------------------------------- 2. roda todos os testes
# Roda o Maven uma vez; os relatorios XML do Surefire sao lidos depois por categoria.
TESTES_RODARAM=0
rodar_testes() {
  [ $TESTES_RODARAM -eq 1 ] && return 0
  rm -rf "$REL"
  sandbox timeout -k 5 300s $MVN $MVN_OPTS test > /dev/null 2>&1
  TESTES_RODARAM=1
}

# Le os XML do Surefire e imprime "nome<TAB>passou(1/0)<TAB>mensagem" para a categoria $1.
casos() {
  python3 - "$REL" "$1" <<'PY'
import sys, glob, xml.etree.ElementTree as ET
rel, cat = sys.argv[1], sys.argv[2]
for arq in sorted(glob.glob(f"{rel}/TEST-*{cat}Test.xml")):
    for tc in ET.parse(arq).getroot().iter("testcase"):
        falha = tc.find("failure")
        if falha is None:
            falha = tc.find("error")
        msg = "" if falha is None else (falha.get("message") or "").replace("\n", " ")[:300]
        nome = tc.get("name", "?").replace("\t", " ")
        print(f"{nome}\t{0 if falha is not None else 1}\t{msg}")
PY
}

# Mostra o resultado de uma categoria; define as globais CAT_OK e CAT_TOTAL.
CAT_OK=0; CAT_TOTAL=0
avaliar() {
  local cat="$1" rotulo="$2" nome passou msg
  titulo "$rotulo"
  rodar_testes
  CAT_OK=0; CAT_TOTAL=0
  while IFS=$'\t' read -r nome passou msg; do
    CAT_TOTAL=$((CAT_TOTAL + 1))
    if [ "$passou" = "1" ]; then
      ok "$nome"; CAT_OK=$((CAT_OK + 1))
    else
      erro "$nome"
      [ -n "$msg" ] && echo "      ${msg:0:220}" >&2
    fi
  done < <(casos "$cat")
  echo "  -> $CAT_OK de $CAT_TOTAL" >&2
  [ "$CAT_TOTAL" -gt 0 ] && [ "$CAT_OK" -eq "$CAT_TOTAL" ]
}

proporcional() {  # $1=acertos $2=total $3=peso
  if [ "$2" -eq 0 ]; then echo 0; else echo $(( $1 * $3 / $2 )); fi
}

case "$MODO" in
  compila)       compilar; exit $? ;;
  comportamento) compilar && avaliar Comportamento "2. Testes de comportamento"; exit $? ;;
  arquitetura)   compilar && avaliar Arquitetura "3. Testes de arquitetura (fitness functions)"; exit $? ;;
  tudo|json) ;;
  *) echo "modo desconhecido: $MODO" >&2; exit 2 ;;
esac

NOTA_C=0; NOTA_B=0; NOTA_A=0
B_OK=0; B_TOT=0; A_OK=0; A_TOT=0
if compilar; then
  NOTA_C=$PTS_COMPILA
  avaliar Comportamento "2. Testes de comportamento"
  B_OK=$CAT_OK; B_TOT=$CAT_TOTAL
  NOTA_B=$(proporcional $B_OK $B_TOT $PTS_COMPORTAMENTO)
  avaliar Arquitetura "3. Testes de arquitetura (fitness functions)"
  A_OK=$CAT_OK; A_TOT=$CAT_TOTAL
  NOTA_A=$(proporcional $A_OK $A_TOT $PTS_ARQUITETURA)
fi
NOTA=$((NOTA_C + NOTA_B + NOTA_A))

if [ "$MODO" = "json" ]; then
  passou() { [ "$1" -eq "$2" ] && echo true || echo false; }
  cat <<JSON
{"tests": [
  {"test-name": "Compilacao", "passed": $([ $NOTA_C -gt 0 ] && echo true || echo false), "score": $NOTA_C, "max-score": $PTS_COMPILA},
  {"test-name": "Comportamento ($B_OK de $B_TOT testes)", "passed": $(passou $NOTA_B $PTS_COMPORTAMENTO), "score": $NOTA_B, "max-score": $PTS_COMPORTAMENTO},
  {"test-name": "Arquitetura ($A_OK de $A_TOT fitness functions)", "passed": $(passou $NOTA_A $PTS_ARQUITETURA), "score": $NOTA_A, "max-score": $PTS_ARQUITETURA}
]}
JSON
else
  printf '\n\033[1mNOTA: %d / 100\033[0m  (compilacao %d · comportamento %d · arquitetura %d)\n' \
    $NOTA $NOTA_C $NOTA_B $NOTA_A >&2
  { [ -n "${GITHUB_STEP_SUMMARY:-}" ] && printf '## Nota: %d / 100\n\n| Criterio | Pontos |\n|---|---|\n| Compilacao | %d/%d |\n| Comportamento (%d/%d) | %d/%d |\n| Arquitetura (%d/%d) | %d/%d |\n' \
      $NOTA $NOTA_C $PTS_COMPILA $B_OK $B_TOT $NOTA_B $PTS_COMPORTAMENTO $A_OK $A_TOT $NOTA_A $PTS_ARQUITETURA >> "$GITHUB_STEP_SUMMARY"; } || true
fi
exit 0
