# Atalhos (o Maven faz o trabalho de verdade)
.PHONY: all test run clean
all:
	mvn -B -q -DskipTests test-compile
test:
	./scripts/run_tests.sh
run:
	mvn -B -q -DskipTests compile && java -cp target/classes br.edu.ifal.arqsw.frete.App
clean:
	mvn -B -q clean
