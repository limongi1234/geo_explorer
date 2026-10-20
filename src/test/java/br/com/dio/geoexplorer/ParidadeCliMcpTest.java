package br.com.dio.geoexplorer;

import br.com.dio.geoexplorer.command.CommandRouter;
import br.com.dio.geoexplorer.json.Json;
import br.com.dio.geoexplorer.mcp.McpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Garante que o CLI e o servidor MCP produzem o mesmo conteúdo para a mesma entrada.
 * Como os dois usam o mesmo {@code ExplorerService}, este teste protege a decisão de
 * arquitetura de não duplicar regras de negócio.
 */
@DisplayName("Paridade: CLI e MCP devolvem o mesmo conteúdo")
class ParidadeCliMcpTest {

    @Test
    @DisplayName("/trilha == buscar_trilha")
    void trilha() throws IOException {
        assertEquals(cli("/trilha python"), mcp("buscar_trilha", "{\"tecnologia\":\"python\"}"));
    }

    @Test
    @DisplayName("/desafio == gerar_desafio com a mesma semente")
    void desafio() throws IOException {
        assertEquals(cli("/desafio java intermediario"),
                mcp("gerar_desafio", "{\"tecnologia\":\"java\",\"nivel\":\"intermediario\"}"));
    }

    @Test
    @DisplayName("/cards == gerar_cards com a mesma semente")
    void cards() throws IOException {
        assertEquals(cli("/cards react 2"), mcp("gerar_cards", "{\"tecnologia\":\"react\",\"quantidade\":2}"));
    }

    private static String cli(String linha) throws IOException {
        var roteador = new CommandRouter(TestSupport.servico(), Files.createTempDirectory("paridade"));
        return roteador.executar(linha).texto();
    }

    private static String mcp(String ferramenta, String args) {
        var servidor = new McpServer(TestSupport.servico());
        String resposta = servidor.processar("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\""
                + ferramenta + "\",\"arguments\":" + args + "}}");
        Map<?, ?> result = (Map<?, ?>) Json.parseObject(resposta).get("result");
        return (String) ((Map<?, ?>) ((List<?>) result.get("content")).getFirst()).get("text");
    }
}
