package br.com.dio.geoexplorer.model;

import java.util.List;

/** Desafio prático associado a uma trilha e a um nível. */
public record Desafio(Nivel nivel, String titulo, String descricao, List<String> criterios) {

    public Desafio {
        criterios = List.copyOf(criterios);
    }
}
