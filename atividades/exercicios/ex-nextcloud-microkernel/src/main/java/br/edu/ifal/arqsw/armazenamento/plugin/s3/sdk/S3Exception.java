package br.edu.ifal.arqsw.armazenamento.plugin.s3.sdk;

/** Excecao do SDK (simulado) do S3. NAO ALTERE: faz o papel de uma biblioteca externa. */
public class S3Exception extends RuntimeException {
    public S3Exception(String message) {
        super(message);
    }
}
