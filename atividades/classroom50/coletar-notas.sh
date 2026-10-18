#!/usr/bin/env bash
# Baixa os envios e gera o scores.csv para lancar no diario.
#   ./coletar-notas.sh ex01-soma-vetor
set -euo pipefail
cd "$(dirname "$0")"
source ./turma.conf
EX="${1:?uso: ./coletar-notas.sh <slug-do-exercicio>}"

# 1) forca a agregacao das notas (senao roda so de madrugada)
gh workflow run collect-scores.yaml --repo "$ORG/classroom50" -f classroom="$TURMA"
echo "coleta disparada; aguardando alguns segundos..."; sleep 20

# 2) baixa os repositorios + scores.csv
gh teacher download -d ./notas "$ORG" "$TURMA" "$EX"
echo "pronto: procure o scores.csv na pasta ./notas/*/"
