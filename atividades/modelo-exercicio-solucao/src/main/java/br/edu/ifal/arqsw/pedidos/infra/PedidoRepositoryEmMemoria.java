package br.edu.ifal.arqsw.pedidos.infra;

import br.edu.ifal.arqsw.pedidos.dominio.Pedido;
import br.edu.ifal.arqsw.pedidos.dominio.PedidoRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Implementacao em memoria do repositorio (num sistema real: JPA, JDBC, Redis...). */
public class PedidoRepositoryEmMemoria implements PedidoRepository {

    private static final PedidoRepositoryEmMemoria INSTANCIA = new PedidoRepositoryEmMemoria();

    private final Map<Long, Pedido> dados = new HashMap<>();
    private long sequencia = 0;

    /** Acesso global (Singleton). Cuidado: facilita o acoplamento indevido... */
    public static PedidoRepositoryEmMemoria instancia() {
        return INSTANCIA;
    }

    @Override
    public void salvar(Pedido pedido) {
        dados.put(pedido.id(), pedido);
    }

    @Override
    public Optional<Pedido> buscar(long id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public long proximoId() {
        return ++sequencia;
    }
}
