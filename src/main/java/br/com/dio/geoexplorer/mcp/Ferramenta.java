package br.com.dio.geoexplorer.mcp;

import java.util.Map;
import java.util.function.Function;

/**
 * Ferramenta exposta pelo servidor MCP.
 *
 * @param nome        nome usado pelo cliente em {@code tools/call}
 * @param descricao   texto que o agente (IBM Bob) lê para decidir quando usar a ferramenta
 * @param inputSchema JSON Schema dos argumentos (também usado na validação)
 * @param handler     recebe os argumentos já validados e devolve o texto da resposta
 */
public record Ferramenta(
        String nome,
        String descricao,
        Map<String, Object> inputSchema,
        Function<Map<String, Object>, String> handler) {

    public Map<String, Object> definicao() {
        return Map.of("name", nome, "description", descricao, "inputSchema", inputSchema);
    }
}
