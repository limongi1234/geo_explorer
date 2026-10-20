package br.com.dio.geoexplorer.mcp;

import br.com.dio.geoexplorer.json.Json;
import br.com.dio.geoexplorer.json.JsonException;
import br.com.dio.geoexplorer.service.ExplorerService;
import br.com.dio.geoexplorer.service.GeoExplorerException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor MCP (Model Context Protocol) via stdio, implementado só com a JDK.
 *
 * <p>Protocolo: JSON-RPC 2.0, uma mensagem por linha em stdin/stdout. Logs vão para stderr,
 * porque qualquer texto extra em stdout corromperia o canal com o cliente (IBM Bob).</p>
 *
 * <p>Métodos suportados: {@code initialize}, {@code notifications/initialized}, {@code ping},
 * {@code tools/list} e {@code tools/call}.</p>
 */
public final class McpServer {

    public static final String VERSAO_PROTOCOLO_PADRAO = "2025-06-18";
    static final List<String> VERSOES_SUPORTADAS = List.of("2025-06-18", "2025-03-26", "2024-11-05");

    static final int PARSE_ERROR = -32700;
    static final int INVALID_REQUEST = -32600;
    static final int METHOD_NOT_FOUND = -32601;
    static final int INVALID_PARAMS = -32602;
    static final int INTERNAL_ERROR = -32603;

    private static final String VERSAO_JSON_RPC = "2.0";

    private final Map<String, Ferramenta> ferramentas;

    public McpServer(ExplorerService servico) {
        this.ferramentas = Ferramentas.criar(servico);
    }

    /** Loop principal: lê linhas até o fim da entrada. */
    public void executar(InputStream entrada, OutputStream saida) throws IOException {
        var leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        var escritor = new PrintStream(saida, true, StandardCharsets.UTF_8);
        log("Geo-Explorer MCP iniciado (" + ferramentas.size() + " ferramentas)");

        String linha;
        while ((linha = leitor.readLine()) != null)
            responder(escritor, linha);

        log("Entrada encerrada, servidor finalizado");
    }

    private void responder(PrintStream escritor, String linha) {
        if (linha.isBlank())
            return;

        String resposta = processar(linha);

        if (resposta != null)
            escritor.println(resposta);
    }

    /**
     * Processa uma mensagem JSON-RPC e devolve a resposta serializada,
     * ou {@code null} quando a mensagem é uma notificação (sem id).
     */
    public String processar(String mensagem) {
        Map<String, Object> requisicao;
        try {
            requisicao = Json.parseObject(mensagem);
        } catch (JsonException e) {
            return Json.stringify(erro(null, PARSE_ERROR, "JSON inválido: " + e.getMessage()));
        }

        Object id = requisicao.get("id");
        boolean notificacao = !requisicao.containsKey("id");

        if (!VERSAO_JSON_RPC.equals(requisicao.get("jsonrpc")) || !(requisicao.get("method") instanceof String metodo))
            return responderErro(notificacao, id, INVALID_REQUEST, "Requisição JSON-RPC inválida");

        try {
            Object resultado = despachar(metodo, parametros(requisicao));
            return notificacao ? null : Json.stringify(sucesso(id, resultado == null ? Map.of() : resultado));
        } catch (ErroRpc e) {
            return responderErro(notificacao, id, e.codigo, e.getMessage());
        } catch (RuntimeException e) {
            log("Erro interno: " + e);
            return responderErro(notificacao, id, INTERNAL_ERROR, "Erro interno do servidor");
        }
    }

    private Object despachar(String metodo, Map<String, Object> params) {
        return switch (metodo) {
            case "initialize" -> inicializar(params);
            case "ping" -> Map.of();
            case "tools/list" -> listarFerramentas();
            case "tools/call" -> chamarFerramenta(params);
            case "notifications/initialized", "notifications/cancelled" -> null;
            default -> throw new ErroRpc(METHOD_NOT_FOUND, "Método não suportado: " + metodo);
        };
    }

    // ------------------------------------------------------------------ métodos

    private Map<String, Object> inicializar(Map<String, Object> params) {
        var resultado = new LinkedHashMap<String, Object>();
        resultado.put("protocolVersion", negociarVersao(params.get("protocolVersion")));
        resultado.put("capabilities", Map.of("tools", Map.of("listChanged", false)));
        resultado.put("serverInfo", Map.of("name", "geo-explorer", "version", "1.0.0"));
        resultado.put("instructions", "Use as ferramentas do Geo-Explorer para consultar trilhas, "
                + "sortear desafios, gerar flash cards e certificados. Comece por listar_tecnologias.");
        return resultado;
    }

    private static String negociarVersao(Object pedida) {
        return pedida instanceof String versao && VERSOES_SUPORTADAS.contains(versao)
                ? versao
                : VERSAO_PROTOCOLO_PADRAO;
    }

    private Map<String, Object> listarFerramentas() {
        return Map.of("tools", ferramentas.values().stream().map(Ferramenta::definicao).toList());
    }

    private Map<String, Object> chamarFerramenta(Map<String, Object> params) {
        Ferramenta ferramenta = localizarFerramenta(params.get("name"));
        Map<String, Object> args = argumentos(params.getOrDefault("arguments", Map.of()));

        // Erros de validação e de negócio voltam como resultado com isError=true (padrão MCP),
        // para que o agente leia a mensagem e corrija a chamada sozinho.
        List<String> erros = ValidadorArgumentos.validar(ferramenta.inputSchema(), args);

        if (!erros.isEmpty())
            return conteudo("Argumentos inválidos: " + String.join("; ", erros), true);

        try {
            return conteudo(ferramenta.handler().apply(args), false);
        } catch (GeoExplorerException e) {
            return conteudo(e.getMessage(), true);
        }
    }

    private Ferramenta localizarFerramenta(Object nome) {
        if (!(nome instanceof String nomeTexto))
            throw new ErroRpc(INVALID_PARAMS, "Parâmetro 'name' é obrigatório");

        Ferramenta ferramenta = ferramentas.get(nomeTexto);

        if (ferramenta == null)
            throw new ErroRpc(INVALID_PARAMS, "Ferramenta desconhecida: " + nomeTexto);

        return ferramenta;
    }

    private static Map<String, Object> argumentos(Object brutos) {
        if (!(brutos instanceof Map<?, ?> mapa))
            throw new ErroRpc(INVALID_PARAMS, "'arguments' deve ser um objeto");

        return cast(mapa);
    }

    // ------------------------------------------------------------------ apoio

    private static Map<String, Object> parametros(Map<String, Object> requisicao) {
        return requisicao.get("params") instanceof Map<?, ?> params ? cast(params) : Map.of();
    }

    private static String responderErro(boolean notificacao, Object id, int codigo, String mensagem) {
        return notificacao ? null : Json.stringify(erro(id, codigo, mensagem));
    }

    private static Map<String, Object> conteudo(String texto, boolean erro) {
        var conteudo = new LinkedHashMap<String, Object>();
        conteudo.put("content", List.of(Map.of("type", "text", "text", texto)));
        conteudo.put("isError", erro);
        return conteudo;
    }

    private static Map<String, Object> sucesso(Object id, Object resultado) {
        var resposta = envelope(id);
        resposta.put("result", resultado);
        return resposta;
    }

    private static Map<String, Object> erro(Object id, int codigo, String mensagem) {
        var resposta = envelope(id);
        resposta.put("error", Map.of("code", codigo, "message", mensagem));
        return resposta;
    }

    private static Map<String, Object> envelope(Object id) {
        var resposta = new LinkedHashMap<String, Object>();
        resposta.put("jsonrpc", VERSAO_JSON_RPC);
        resposta.put("id", id);
        return resposta;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> mapa) {
        return (Map<String, Object>) mapa;
    }

    private static void log(String mensagem) {
        System.err.println("[geo-explorer-mcp] " + mensagem);
    }

    private static final class ErroRpc extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final int codigo;

        ErroRpc(int codigo, String mensagem) {
            super(mensagem);
            this.codigo = codigo;
        }
    }
}
