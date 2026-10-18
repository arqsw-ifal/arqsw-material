#!/usr/bin/env bash
#
# Cria (ou atualiza) a turma e os exercicios no Classroom 50, a partir de
# turma.conf + exercicios.tsv.
#
#   ./provisionar.sh              -> SIMULA (dry-run): so mostra o que faria
#   ./provisionar.sh --executar   -> executa de verdade
#
# E' idempotente: o que ja existe e' pulado, entao da para rodar de novo
# depois de acrescentar uma linha em exercicios.tsv.
#
# Pre-requisitos (uma vez):
#   gh extension install foundation50/gh-teacher   # o repo da extensao e' gh-teacher
#   gh teacher login
#   gh teacher init <ORG>        # cria o repo de configuracao <ORG>/classroom50

set -uo pipefail
cd "$(dirname "$0")"

EXECUTAR=0
[ "${1:-}" = "--executar" ] && EXECUTAR=1

# shellcheck source=turma.conf
[ -f turma.conf ] || { echo "faltou o turma.conf (veja turma.conf.exemplo)"; exit 1; }
source ./turma.conf
: "${ORG:?defina ORG no turma.conf}"
: "${TURMA:?defina TURMA no turma.conf}"

# Detecta o autograder e o runtime da disciplina (autograder-*.py / runtime-*.json),
# para este script ser identico em todas as disciplinas.
AUTOGRADER="$(ls autograder-*.py 2>/dev/null | head -1)"
RUNTIME="$(ls runtime-*.json 2>/dev/null | head -1)"

azul()  { printf '\033[36m%s\033[0m\n' "$1"; }
ok()    { printf '  \033[32m✓\033[0m %s\n' "$1"; }
pulou() { printf '  \033[33m=\033[0m %s\n' "$1"; }
falha() { printf '  \033[31m✗\033[0m %s\n' "$1"; }

# executa (ou so mostra) um comando
rodar() {
  if [ $EXECUTAR -eq 1 ]; then
    if "$@" >/tmp/c50.log 2>&1; then ok "$*"; else falha "$* -> $(tail -1 /tmp/c50.log)"; return 1; fi
  else
    printf '  \033[90m$ %s\033[0m\n' "$*"
  fi
}

command -v gh >/dev/null || { echo "gh (GitHub CLI) nao encontrado"; exit 1; }
if ! gh extension list 2>/dev/null | grep -q 'gh-teacher'; then
  echo "AVISO: a extensao gh-teacher nao parece instalada."
  echo "       gh extension install foundation50/gh-teacher"
  [ $EXECUTAR -eq 1 ] && exit 1
fi

[ $EXECUTAR -eq 1 ] || azul "== SIMULACAO (nada sera criado). Use --executar para valer. =="

# ---------------------------------------------------------------------- turma
azul "== Turma: $TURMA ($ORG) =="
# consulta tambem na simulacao (e' somente leitura), para o dry-run refletir a realidade
if gh teacher classroom list "$ORG" --json 2>/dev/null | grep -q "\"$TURMA\""; then
  pulou "turma '$TURMA' ja existe"
else
  rodar gh teacher classroom add "$ORG" "$TURMA" --name "$TURMA_NOME" --term "$TURMA_PERIODO"
fi

# --------------------------------------------------------- autograder padrao
if [ -n "$AUTOGRADER" ]; then
  azul "== Autograder padrao da turma ($AUTOGRADER) =="
  rodar gh teacher autograder set-default "$ORG" "$TURMA" --from "./$AUTOGRADER"
fi

# ------------------------------------------------------------------- turma/csv
if [ -n "${ROSTER_CSV:-}" ] && [ -f "$ROSTER_CSV" ]; then
  azul "== Lista de alunos ($ROSTER_CSV) =="
  rodar gh teacher roster import "$ORG" "$TURMA" "$ROSTER_CSV"
fi

# ------------------------------------------------------------------ exercicios
azul "== Exercicios =="
[ -f exercicios.tsv ] || { echo "faltou exercicios.tsv"; exit 1; }

# tambem somente leitura: assim a simulacao ja mostra o que seria pulado
existentes="$(gh teacher assignment list "$ORG" "$TURMA" --json 2>/dev/null || true)"

while IFS=$'\t' read -r slug nome template prazo; do
  # ignora comentarios e linhas vazias
  case "${slug:-}" in ''|'#'*) continue ;; esac

  if [ -n "$existentes" ] && printf '%s' "$existentes" | grep -q "\"$slug\""; then
    pulou "$slug (ja existe)"
    continue
  fi

  # template pode vir como "owner/repo" (outra org) ou so "repo" (assume $ORG)
  case "$template" in */*) tmpl="$template" ;; *) tmpl="$ORG/$template" ;; esac

  args=( gh teacher assignment add "$ORG" "$TURMA" "$slug"
         --name "$nome" --template "$tmpl" --runtime "./$RUNTIME" )
  [ -n "${prazo:-}" ] && [ "$prazo" != "-" ] && args+=( --due "$prazo" )

  rodar "${args[@]}"
done < exercicios.tsv

echo
if [ $EXECUTAR -eq 1 ]; then
  azul "Pronto. Confira em https://classroom50.org e distribua o link aos alunos."
else
  azul "Simulacao concluida. Se estiver certo: ./provisionar.sh --executar"
fi
