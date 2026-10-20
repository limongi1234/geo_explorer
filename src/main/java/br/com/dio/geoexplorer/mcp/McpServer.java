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
        while ((linha = leitor.readLine()) != null) {
            if (linha.isBlank()) {
                continue;
            }
            String resposta = processar(linha);
            if (resposta != null) {
                escritor.println(resposta);
                escritor.flush();
            }
        }
        log("Entrada encerrada, servidor finalizado");
    }

    /**
     * Processa uma mensagem JSON-RPC e devolve a resposta serializada,
     * ou {@code null} quando a mensagem é uma notificação (sem id).
     */
    public String processar(String mensagem) {
        Map<String, Object> req;
        try {
            req = Json.parseObject(mensagem);
        } catch (JsonException e) {
            return Json.stringify(erro(null, PARSE_ERROR, "JSON inválido: " + e.getMessage()));
        }

        Object id = req.get("id");
        boolean notificacao = !req.containsKey("id");
        if (!"2.0".equals(req.get("jsonrpc")) || !(req.get("method") instanceof String metodo)) {
            return notificacao ? null : Json.stringify(erro(id, INVALID_REQUEST, "Requisição JSON-RPC inválida"));
        }

        try {
            Map<String, Object> params = req.get("params") instanceof Map<?, ?> p ? cast(p) : Map.of();
            Object resultado = switch (metodo) {
                case "initialize" -> inicializar(params);
                case "ping" -> Map.of();
                case "tools/list" -> listarFerramentas();
                case "tools/call" -> chamarFerramenta(params);
                case "notifications/initialized", "notifications/cancelled" -> null;
                default -> throw new ErroRpc(METHOD_NOT_FOUND, "Método não suportado: " + metodo);
            };
            if (notificacao) {
                return null;
            }
            return Json.stringify(sucesso(id, resultado == null ? Map.of() : resultado));
        } catch (ErroRpc e) {
            return notificacao ? null : Json.stringify(erro(id, e.codigo, e.getMessage()));
        } catch (RuntimeException e) {
            log("Erro interno: " + e);
            return notificacao ? null : Json.stringify(erro(id, INTERNAL_ERROR, "Erro interno do servidor"));
        }
    }

    // ------------------------------------------------------------------ métodos

    private Map<String, Object> inicializar(Map<String, Object> params) {
        Object pedida = params.get("protocolVersion");
        String versao = pedida instanceof String v && VERSOES_SUPORTADAS.contains(v) ? v : VERSAO_PROTOCOLO_PADRAO;
        var r = new LinkedHashMap<String, Object>();
        r.put("protocolVersion", versao);
        r.put("capabilities", Map.of("tools", Map.of("listChanged", false)));
        r.put("serverInfo", Map.of("name", "geo-explorer", "version", "1.0.0"));
        r.put("instructions", "Use as ferramentas do Geo-Explorer para consultar trilhas, "
                + "sortear desafios, gerar flash cards e certificados. Comece por listar_tecnologias.");
        return r;
    }

    private Map<String, Object> listarFerramentas() {
        return Map.of("tools", ferramentas.values().stream().map(Ferramenta::definicao).toList());
    }

    private Map<String, Object> chamarFerramenta(Map<String, Object> params) {
        if (!(params.get("name") instanceof String nome)) {
            throw new ErroRpc(INVALID_PARAMS, "Parâmetro 'name' é obrigatório");
        }
        Ferramenta f = ferramentas.get(nome);
        if (f == null) {
            throw new ErroRpc(INVALID_PARAMS, "Ferramenta desconhecida: " + nome);
        }
        Object brutos = params.getOrDefault("arguments", Map.of());
        if (!(brutos instanceof Map<?, ?> m)) {
            throw new ErroRpc(INVALID_PARAMS, "'arguments' deve ser um objeto");
        }
        Map<String, Object> args = cast(m);

        // Erros de validação e de negócio voltam como resultado com isError=true (padrão MCP),
        // para que o agente leia a mensagem e corrija a chamada sozinho.
        List<String> erros = ValidadorArgumentos.validar(f.inputSchema(), args);
        if (!erros.isEmpty()) {
            return conteudo("Argumentos inválidos: " + String.join("; ", erros), true);
        }
        try {
            return conteudo(f.handler().apply(args), false);
        } catch (GeoExplorerException e) {
            return conteudo(e.getMessage(), true);
        }
    }

    // ------------------------------------------------------------------ apoio

    private static Map<String, Object> conteudo(String texto, boolean erro) {
        var r = new LinkedHashMap<String, Object>();
        r.put("content", List.of(Map.of("type", "text", "text", texto)));
        r.put("isError", erro);
        return r;
    }

    private static Map<String, Object> sucesso(Object id, Object resultado) {
        var r = new LinkedHashMap<String, Object>();
        r.put("jsonrpc", "2.0");
        r.put("id", id);
        r.put("result", resultado);
        return r;
    }

    private static Map<String, Object> erro(Object id, int codigo, String mensagem) {
        var r = new LinkedHashMap<String, Object>();
        r.put("jsonrpc", "2.0");
        r.put("id", id);
        r.put("error", Map.of("code", codigo, "message", mensagem));
        return r;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }

    private static void log(String msg) {
        System.err.println("[geo-explorer-mcp] " + msg);
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
