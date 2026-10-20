package br.com.dio.geoexplorer.service;

import java.time.LocalDate;

/** Certificado gerado para um aluno ao concluir uma trilha. */
public record Certificado(
        String nome,
        String trilha,
        int cargaHoraria,
        LocalDate data,
        String codigo,
        String nomeArquivo,
        String markdown) {
}
