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
        Map<String, Object> props = (Map<String, Object>) schema.getOrDefault("properties", Map.of());

        for (Object obrig : (List<Object>) schema.getOrDefault("required", List.of())) {
            Object valor = args.get(obrig.toString());
            if (valor == null || (valor instanceof String s && s.isBlank())) {
                erros.add("Campo obrigatório ausente: " + obrig);
            }
        }

        if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
            for (String chave : args.keySet()) {
                if (!props.containsKey(chave)) {
                    erros.add("Campo não permitido: " + chave);
                }
            }
        }

        for (var entrada : props.entrySet()) {
            Object valor = args.get(entrada.getKey());
            if (valor == null) {
                continue;
            }
            Map<String, Object> regra = (Map<String, Object>) entrada.getValue();
            String campo = entrada.getKey();
            switch (String.valueOf(regra.get("type"))) {
                case "string" -> {
                    if (!(valor instanceof String s)) {
                        erros.add(campo + " deve ser texto");
                        continue;
                    }
                    if (regra.get("minLength") instanceof Number min && s.strip().length() < min.intValue()) {
                        erros.add(campo + " deve ter ao menos " + min + " caractere(s)");
                    }
                    if (regra.get("maxLength") instanceof Number max && s.length() > max.intValue()) {
                        erros.add(campo + " deve ter no máximo " + max + " caracteres");
                    }
                    if (regra.get("enum") instanceof List<?> opcoes && !opcoes.contains(s)) {
                        erros.add(campo + " deve ser um de: " + opcoes);
                    }
                }
                case "integer" -> {
                    boolean inteiro = valor instanceof Long || valor instanceof Integer
                            || (valor instanceof Double d && d == Math.rint(d) && !d.isInfinite());
                    if (!inteiro) {
                        erros.add(campo + " deve ser um número inteiro");
                        continue;
                    }
                    long n = ((Number) valor).longValue();
                    if (regra.get("minimum") instanceof Number min && n < min.longValue()) {
                        erros.add(campo + " deve ser no mínimo " + min);
                    }
                    if (regra.get("maximum") instanceof Number max && n > max.longValue()) {
                        erros.add(campo + " deve ser no máximo " + max);
                    }
                }
                default -> {
                    // tipos não usados no projeto
                }
            }
        }
        return erros;
    }
}
