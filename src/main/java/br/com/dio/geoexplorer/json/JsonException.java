package br.com.dio.geoexplorer.json;

/** Erro de leitura ou escrita de JSON. */
public class JsonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JsonException(String mensagem) {
        super(mensagem);
    }
}
