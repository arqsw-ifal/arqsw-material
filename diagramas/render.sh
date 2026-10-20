#!/usr/bin/env bash
# Renderiza todos os diagramas D2 de diagramas/d2/*.d2 para docs/assets/diagramas/*.svg.
# Requer o D2 (https://d2lang.com): brew install d2
# Os SVGs gerados são versionados; o deploy do site não precisa do D2.
set -euo pipefail
cd "$(dirname "$0")"
for f in d2/*.d2; do
  nome="$(basename "$f" .d2)"
  d2 --pad 20 "$f" "../docs/assets/diagramas/$nome.svg"
done
