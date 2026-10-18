package br.edu.ifal.arqsw.hub.hub;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hub de eventos (Secao 6.3.3): mantem o indice de assinantes e entrega cada evento aos
 * interessados. Para quem publica, e' um Mediator; para as conexoes, um Subject do Observer.
 *
 * CODIGO LEGADO: a entrega esta incompleta (veja os TODO) e o hub expoe seus indices
 * internos, o que permitiu que os produtores "pulassem" o hub. Leia o README.
 */
public final class HubDeEventos implements Publicador {

    private final List<Assinante> assinantes = new CopyOnWriteArrayList<>();
    private final Map<String, Set<String>> membrosPorCanal = new ConcurrentHashMap<>();

    public void registrar(Assinante assinante) {
        assinantes.add(assinante);
    }

    public void remover(Assinante assinante) {
        // TODO (regra 3): retirar o assinante do indice; depois disso ele nao recebe mais nada
    }

    public void entrarNoCanal(String canalId, String usuarioId) {
        membrosPorCanal.computeIfAbsent(canalId, c -> ConcurrentHashMap.newKeySet()).add(usuarioId);
    }

    /** CODIGO LEGADO: expoe o indice de assinantes. */
    public List<Assinante> assinantes() {
        return List.copyOf(assinantes);
    }

    /** CODIGO LEGADO: expoe os membros de um canal. */
    public Set<String> membrosDo(String canalId) {
        return Set.copyOf(membrosPorCanal.getOrDefault(canalId, Set.of()));
    }

    @Override
    public void publicar(Evento evento) {
        Set<String> membros = evento.canalId() == null
                ? Set.of()
                : membrosPorCanal.getOrDefault(evento.canalId(), Set.of());
        for (Assinante a : assinantes) {
            boolean doCanal = membros.contains(a.usuarioId());
            // TODO (regra 1): evento com alcance de USUARIO (usuarioId preenchido) deve chegar
            //   a todas as conexoes desse usuario.
            // TODO (regra 2): respeitar o filtro do assinante, a.deveReceber(evento),
            //   como o ShouldSendEvent do Mattermost.
            if (doCanal) {
                a.notificar(evento);
            }
        }
    }
}
