package br.edu.ifal.arqsw.armazenamento.plugin.s3.sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * SDK SIMULADO de um servico de objetos compativel com S3. NAO ALTERE: faz o papel de
 * uma biblioteca externa, com a interface DELA (buckets, chaves, putObject...),
 * que nada tem a ver com o contrato do nucleo.
 */
public class ClienteS3 {

    private final Map<String, Map<String, byte[]>> buckets = new HashMap<>();
    private boolean foraDoAr;

    /** Simula uma queda do servico: as chamadas seguintes lancam S3Exception. */
    public void simularQueda(boolean foraDoAr) {
        this.foraDoAr = foraDoAr;
    }

    public void putObject(String bucket, String key, byte[] body) {
        verificar();
        buckets.computeIfAbsent(bucket, b -> new HashMap<>()).put(key, body.clone());
    }

    /** Devolve null se a chave nao existir (como varios SDKs reais). */
    public byte[] getObject(String bucket, String key) {
        verificar();
        byte[] body = buckets.getOrDefault(bucket, Map.of()).get(key);
        return body == null ? null : body.clone();
    }

    public boolean doesObjectExist(String bucket, String key) {
        verificar();
        return buckets.getOrDefault(bucket, Map.of()).containsKey(key);
    }

    public Set<String> listKeys(String bucket) {
        verificar();
        return new TreeSet<>(buckets.getOrDefault(bucket, Map.of()).keySet());
    }

    private void verificar() {
        if (foraDoAr) {
            throw new S3Exception("503 Service Unavailable");
        }
    }
}
