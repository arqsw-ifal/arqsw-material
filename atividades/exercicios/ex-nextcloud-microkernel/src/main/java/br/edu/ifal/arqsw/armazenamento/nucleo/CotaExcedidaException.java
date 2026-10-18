package br.edu.ifal.arqsw.armazenamento.nucleo;

public class CotaExcedidaException extends RuntimeException {
    public CotaExcedidaException(String caminho, long necessario, long livre) {
        super("cota excedida ao gravar " + caminho + ": necessario " + necessario + " bytes, livre " + livre);
    }
}
