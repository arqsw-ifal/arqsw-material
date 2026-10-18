package br.edu.ifal.arqsw.voo.barramento;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Barramento publish/subscribe com topicos tipados, no estilo do uORB do PX4 (Listagem 6.8.12).
 * Guarda a ultima amostra de cada topico e notifica os assinantes a cada publicacao.
 */
public final class Barramento {

    private final Map<Topico<?>, Object> ultimaAmostra = new ConcurrentHashMap<>();
    private final Map<Topico<?>, List<Consumer<Object>>> assinantes = new ConcurrentHashMap<>();

    /** Publica uma mensagem: atualiza a "ultima amostra" do topico e avisa os assinantes. */
    public <T> void publicar(Topico<T> topico, T mensagem) {
        Objects.requireNonNull(mensagem, "mensagem nula");
        ultimaAmostra.put(topico, topico.tipo().cast(mensagem));
        // TODO (comportamento): notificar os assinantes do topico, na ordem em que assinaram.
        //      Hoje a mensagem fica guardada, mas ninguem e' avisado da chegada dela.
    }

    /** Registra um assinante: ele sera chamado a cada nova mensagem do topico. */
    public <T> void assinar(Topico<T> topico, Consumer<T> aoChegar) {
        assinantes.computeIfAbsent(topico, t -> new CopyOnWriteArrayList<>())
                  .add(m -> aoChegar.accept(topico.tipo().cast(m)));
    }

    /** Leitura nao bloqueante da ultima amostra publicada (vazia se nada foi publicado). */
    public <T> Optional<T> ultima(Topico<T> topico) {
        return Optional.ofNullable(ultimaAmostra.get(topico)).map(topico.tipo()::cast);
    }
}
