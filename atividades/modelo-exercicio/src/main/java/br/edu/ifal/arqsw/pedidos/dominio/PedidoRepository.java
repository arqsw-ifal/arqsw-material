package br.edu.ifal.arqsw.pedidos.dominio;

import java.util.Optional;

/**
 * Repository (Evans, DDD cap. 6; Fowler, PoEAA cap. 13).
 * A INTERFACE mora no dominio; a IMPLEMENTACAO mora na infraestrutura.
 * E' isso que permite ao dominio nao depender do banco (inversao de dependencia).
 */
public interface PedidoRepository {
    void salvar(Pedido pedido);
    Optional<Pedido> buscar(long id);
    long proximoId();
}
