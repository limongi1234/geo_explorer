package br.com.dio.geoexplorer.model;

import java.util.List;

/** Módulo de uma trilha de aprendizagem. */
public record Modulo(String titulo, int horas, List<String> topicos) {

    public Modulo {
        topicos = List.copyOf(topicos);
    }
}
