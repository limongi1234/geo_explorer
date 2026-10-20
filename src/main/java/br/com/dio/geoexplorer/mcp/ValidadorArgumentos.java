package br.com.dio.geoexplorer.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Valida argumentos de ferramentas contra o subconjunto de JSON Schema usado no projeto:
 * {@code type} (string/integer), {@code required}, {@code enum}, {@code minimum}, {@code maximum},
 * {@code minLength}, {@code maxLength} e {@code additionalProperties: false}.
 *
 * <p>Toda entrada vinda do agente é tratada como não confiável e validada antes de chegar ao serviço.</p>
 */
final class ValidadorArgumentos {

    private ValidadorArgumentos() {
    }

    @SuppressWarnings("unchecked")
    static List<String> validar(Map<String, Object> schema, Map<String, Object> args) {
        List<String> erros = new ArrayList<>();
        Map<String, Object> propriedades = (Map<String, Object>) schema.getOrDefault("properties", Map.of());
        List<Object> obrigatorios = (List<Object>) schema.getOrDefault("required", List.of());

        for (Object campo : obrigatorios)
            if (estaAusente(args.get(campo.toString())))
                erros.add("Campo obrigatório ausente: " + campo);

        if (Boolean.FALSE.equals(schema.get("additionalProperties")))
            for (String campo : args.keySet())
                if (!propriedades.containsKey(campo))
                    erros.add("Campo não permitido: " + campo);

        for (var entrada : propriedades.entrySet())
            validarCampo(entrada.getKey(), (Map<String, Object>) entrada.getValue(), args.get(entrada.getKey()), erros);

        return erros;
    }

    private static boolean estaAusente(Object valor) {
        return valor == null || (valor instanceof String texto && texto.isBlank());
    }

    private static void validarCampo(String campo, Map<String, Object> regra, Object valor, List<String> erros) {
        if (valor == null)
            return;

        switch (String.valueOf(regra.get("type"))) {
            case "string" -> validarTexto(campo, regra, valor, erros);
            case "integer" -> validarInteiro(campo, regra, valor, erros);
            default -> {
                // tipos não usados no projeto
            }
        }
    }

    private static void validarTexto(String campo, Map<String, Object> regra, Object valor, List<String> erros) {
        if (!(valor instanceof String texto)) {
            erros.add(campo + " deve ser texto");
            return;
        }

        if (regra.get("minLength") instanceof Number min && texto.strip().length() < min.intValue())
            erros.add(campo + " deve ter ao menos " + min + " caractere(s)");

        if (regra.get("maxLength") instanceof Number max && texto.length() > max.intValue())
            erros.add(campo + " deve ter no máximo " + max + " caracteres");

        if (regra.get("enum") instanceof List<?> opcoes && !opcoes.contains(texto))
            erros.add(campo + " deve ser um de: " + opcoes);
    }

    private static void validarInteiro(String campo, Map<String, Object> regra, Object valor, List<String> erros) {
        if (!ehInteiro(valor)) {
            erros.add(campo + " deve ser um número inteiro");
            return;
        }

        long numero = ((Number) valor).longValue();

        if (regra.get("minimum") instanceof Number min && numero < min.longValue())
            erros.add(campo + " deve ser no mínimo " + min);

        if (regra.get("maximum") instanceof Number max && numero > max.longValue())
            erros.add(campo + " deve ser no máximo " + max);
    }

    private static boolean ehInteiro(Object valor) {
        return valor instanceof Long || valor instanceof Integer
                || (valor instanceof Double d && d == Math.rint(d) && !d.isInfinite());
    }
}
