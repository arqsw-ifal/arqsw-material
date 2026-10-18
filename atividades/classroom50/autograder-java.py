#!/usr/bin/env python3
"""
Autograder do Classroom 50 para exercicios em Java (Maven + JUnit + ArchUnit).

Onde colocar (no repositorio de configuracao <org>/classroom50):
  <turma>/autograder.py                      -> padrao da turma
  <turma>/autograders/<slug>/autograder.py   -> so para um exercicio

Ele NAO reimplementa a correcao: chama `scripts/run_tests.sh json` do repositorio do
aluno (fonte unica de verdade, a mesma que o aluno roda com `make test`) e traduz a
saida para o schema classroom50/result/v1, gravando ./result.json.

Precisa do runtime com Java e Maven -- veja runtime-java.json.
"""

import datetime
import json
import os
import subprocess
import sys
from pathlib import Path

CORRETOR = ["bash", "scripts/run_tests.sh", "json"]
TIMEOUT_S = 600   # a primeira execucao baixa as dependencias do Maven


def agora():
    return datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def envelope(tests):
    """Monta o result.json no formato que o Classroom 50 espera."""
    commit = os.environ.get("COMMIT_URL", "")
    return {
        "schema": "classroom50/result/v1",
        "classroom": os.environ.get("CLASSROOM", ""),
        "assignment": os.environ.get("ASSIGNMENT", ""),
        "submission": os.environ.get("SUBMISSION_TAG", ""),
        "commit": commit,
        "release": os.environ.get("RELEASE_URL", ""),
        "review": os.environ.get("REVIEW_URL") or commit,
        "datetime": agora(),
        "score": sum(t["score"] for t in tests),
        "max-score": sum(t["max-score"] for t in tests),
        "tests": tests,
    }


def zerado(motivo):
    """Sem conseguir corrigir, devolve 0 com o motivo visivel para o aluno."""
    return [{"test-name": motivo, "passed": False, "score": 0, "max-score": 100}]


def main():
    if not Path("scripts/run_tests.sh").is_file():
        tests = zerado("scripts/run_tests.sh nao encontrado no repositorio")
    else:
        try:
            proc = subprocess.run(CORRETOR, capture_output=True, text=True, timeout=TIMEOUT_S)
        except subprocess.TimeoutExpired:
            tests = zerado(f"o corretor passou de {TIMEOUT_S}s (laco infinito?)")
        else:
            sys.stderr.write(proc.stderr)       # logs legiveis no stderr; JSON no stdout
            try:
                tests = json.loads(proc.stdout)["tests"]
            except (json.JSONDecodeError, KeyError, TypeError):
                sys.stderr.write("\n--- stdout inesperado do corretor ---\n")
                sys.stderr.write(proc.stdout[:2000])
                tests = zerado("falha ao interpretar a saida do corretor")

    resultado = envelope(tests)
    Path("result.json").write_text(json.dumps(resultado, indent=2), encoding="utf-8")

    linhas = [
        f"## Correcao automatica — {resultado['score']}/{resultado['max-score']}",
        "",
        "| Criterio | Pontos |",
        "|---|---|",
    ]
    for t in tests:
        marca = "✅" if t["passed"] else "❌"
        linhas.append(f"| {marca} {t['test-name']} | {t['score']}/{t['max-score']} |")
    Path("release-body.md").write_text("\n".join(linhas) + "\n", encoding="utf-8")

    print(f"nota: {resultado['score']}/{resultado['max-score']}")
    return 0   # nota baixa nao e' falha de infraestrutura


if __name__ == "__main__":
    sys.exit(main())
