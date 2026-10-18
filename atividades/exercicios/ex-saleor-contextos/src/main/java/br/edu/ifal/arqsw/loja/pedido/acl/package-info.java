/**
 * Camada anticorrupcao (Evans, 2003, cap. 14) do contexto de Pedido: o UNICO pacote de
 * Pedido autorizado a conhecer o contexto de Pagamento. Aqui fica o adaptador que implementa
 * a porta de pagamento definida em pedido.dominio e traduz entre as duas linguagens.
 */
package br.edu.ifal.arqsw.loja.pedido.acl;
