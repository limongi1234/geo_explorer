package br.com.dio.geoexplorer.service;

/** Erro de uso (entrada inválida, trilha inexistente etc.) com mensagem amigável ao usuário. */
public class GeoExplorerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GeoExplorerException(String mensagem) {
        super(mensagem);
    }
}
