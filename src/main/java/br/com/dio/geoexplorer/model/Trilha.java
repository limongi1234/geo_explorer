package br.com.dio.geoexplorer.model;

import java.util.List;

/** Trilha de aprendizagem de uma tecnologia. */
public record Trilha(
        String id,
        String nome,
        List<String> aliases,
        String descricao,
        int cargaHoraria,
        int xp,
        List<Modulo> modulos,
        List<Desafio> desafios,
        List<Card> cards) {

    public Trilha {
        aliases = List.copyOf(aliases);
        modulos = List.copyOf(modulos);
        desafios = List.copyOf(desafios);
        cards = List.copyOf(cards);
    }

    public List<Desafio> desafiosDoNivel(Nivel nivel) {
        return desafios.stream().filter(desafio -> desafio.nivel() == nivel).toList();
    }
}
