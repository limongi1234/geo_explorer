package br.com.dio.geoexplorer;

import br.com.dio.geoexplorer.json.Json;
import br.com.dio.geoexplorer.mcp.McpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("McpServer: protocolo JSON-RPC via stdio")
class McpServerTest {

    private final McpServer servidor = new McpServer(TestSupport.servico());

    @Test
    @DisplayName("initialize devolve versão do protocolo e capacidade de tools")
    void initialize() {
        Map<String, Object> r = resultado(chamar(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\"}}"));
        assertEquals("2024-11-05", r.get("protocolVersion"));
        assertTrue(((Map<?, ?>) r.get("capabilities")).containsKey("tools"));
        assertEquals("geo-explorer", ((Map<?, ?>) r.get("serverInfo")).get("name"));
    }

    @Test
    @DisplayName("versão desconhecida cai na versão padrão do servidor")
    void initializeVersaoDesconhecida() {
        Map<String, Object> r = resultado(chamar(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"1999-01-01\"}}"));
        assertEquals(McpServer.VERSAO_PROTOCOLO_PADRAO, r.get("protocolVersion"));
    }

    @Test
    @DisplayName("notificações não geram resposta")
    void notificacao() {
        assertNull(servidor.processar("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));
    }

    @Test
    @DisplayName("tools/list expõe as cinco ferramentas com schema")
    void listaFerramentas() {
        Map<String, Object> r = resultado(chamar("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}"));
        List<?> tools = (List<?>) r.get("tools");
        List<String> nomes = tools.stream().map(t -> (String) ((Map<?, ?>) t).get("name")).toList();
        assertEquals(List.of("buscar_trilha", "gerar_desafio", "gerar_certificado",
                "listar_tecnologias", "gerar_cards"), nomes);
        tools.forEach(t -> assertTrue(((Map<?, ?>) t).containsKey("inputSchema")));
    }

    @Test
    @DisplayName("tools/call executa a ferramenta e devolve texto")
    void chamaFerramenta() {
        String texto = textoDe(chamar(chamada("buscar_trilha", "{\"tecnologia\":\"react\"}")));
        assertTrue(texto.startsWith("# Trilha de React"));
    }

    @Test
    @DisplayName("argumentos inválidos voltam como isError=true para o agente se corrigir")
    void argumentosInvalidos() {
        Map<String, Object> r = resultado(chamar(chamada("gerar_desafio",
                "{\"tecnologia\":\"java\",\"nivel\":\"expert\",\"extra\":1}")));
        assertEquals(Boolean.TRUE, r.get("isError"));
        String texto = textoDe(r);
        assertTrue(texto.contains("nivel deve ser um de"));
        assertTrue(texto.contains("Campo não permitido: extra"));

        Map<String, Object> semCampo = resultado(chamar(chamada("gerar_certificado", "{\"nome\":\"Ana\"}")));
        assertEquals(Boolean.TRUE, semCampo.get("isError"));
        assertTrue(textoDe(semCampo).contains("Campo obrigatório ausente: tecnologia"));

        Map<String, Object> tipoErrado = resultado(chamar(chamada("gerar_cards",
                "{\"tecnologia\":\"java\",\"quantidade\":\"dez\"}")));
        assertTrue(textoDe(tipoErrado).contains("quantidade deve ser um número inteiro"));
    }

    @Test
    @DisplayName("erros de negócio também voltam como isError=true")
    void erroNegocio() {
        Map<String, Object> r = resultado(chamar(chamada("buscar_trilha", "{\"tecnologia\":\"cobol\"}")));
        assertEquals(Boolean.TRUE, r.get("isError"));
        assertTrue(textoDe(r).contains("não encontrada"));
    }

    @Test
    @DisplayName("erros de protocolo usam os códigos JSON-RPC")
    void errosProtocolo() {
        assertEquals(-32700L, codigoErro(servidor.processar("isso não é json")));
        assertEquals(-32600L, codigoErro(servidor.processar("{\"id\":1,\"method\":\"ping\"}")));
        assertEquals(-32601L, codigoErro(servidor.processar("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"voar\"}")));
        assertEquals(-32602L, codigoErro(servidor.processar(chamada("nao_existe", "{}"))));
    }

    @Test
    @DisplayName("loop stdio responde uma linha por requisição e ignora notificações")
    void loopStdio() throws IOException {
        String entrada = String.join("\n",
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}",
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}",
                "",
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}") + "\n";
        var saida = new ByteArrayOutputStream();
        servidor.executar(new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)), saida);
        String[] linhas = saida.toString(StandardCharsets.UTF_8).strip().split("\n");
        assertEquals(2, linhas.length);
        assertEquals(2L, Json.parseObject(linhas[1]).get("id"));
    }

    // ------------------------------------------------------------------ apoio

    private static String chamada(String ferramenta, String argumentos) {
        return "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\",\"params\":{\"name\":\""
                + ferramenta + "\",\"arguments\":" + argumentos + "}}";
    }

    private Map<String, Object> chamar(String requisicao) {
        return Json.parseObject(servidor.processar(requisicao));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> resultado(Map<String, Object> resposta) {
        Object r = resposta.containsKey("result") ? resposta.get("result") : resposta;
        return (Map<String, Object>) r;
    }

    private static String textoDe(Map<String, Object> respostaOuResultado) {
        Map<String, Object> r = resultado(respostaOuResultado);
        Map<?, ?> primeiro = (Map<?, ?>) ((List<?>) r.get("content")).getFirst();
        return (String) primeiro.get("text");
    }

    private static Object codigoErro(String resposta) {
        return ((Map<?, ?>) Json.parseObject(resposta).get("error")).get("code");
    }
}
