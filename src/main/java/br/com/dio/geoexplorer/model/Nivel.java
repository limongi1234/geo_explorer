package br.com.dio.geoexplorer.model;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** Níveis de dificuldade dos desafios. */
public enum Nivel {
    INICIANTE("iniciante", "Iniciante"),
    INTERMEDIARIO("intermediario", "Intermediário"),
    AVANCADO("avancado", "Avançado");

    private final String codigo;
    private final String rotulo;

    Nivel(String codigo, String rotulo) {
        this.codigo = codigo;
        this.rotulo = rotulo;
    }

    public String codigo() {
        return codigo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Aceita "intermediário", "Intermediario", "INTERMEDIARIO" etc. */
    public static Optional<Nivel> de(String texto) {
        if (texto == null || texto.isBlank())
            return Optional.empty();

        String normalizado = normalizar(texto);
        return Arrays.stream(values())
                .filter(nivel -> nivel.codigo.equals(normalizado))
                .findFirst();
    }

    public static String opcoes() {
        return Arrays.stream(values()).map(Nivel::codigo).collect(Collectors.joining(", "));
    }

    /** Remove acentos, espaços nas pontas e converte para minúsculas. */
    public static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}
