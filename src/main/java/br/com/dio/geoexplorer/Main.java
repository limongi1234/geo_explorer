package br.com.dio.geoexplorer;

import br.com.dio.geoexplorer.command.CommandRouter;
import br.com.dio.geoexplorer.mcp.McpServer;
import br.com.dio.geoexplorer.service.ExplorerService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Ponto de entrada do Geo-Explorer.
 *
 * <ul>
 *   <li>{@code java -jar geo-explorer.jar} → modo interativo (REPL)</li>
 *   <li>{@code java -jar geo-explorer.jar /trilha java} → executa um comando e sai</li>
 *   <li>{@code java -jar geo-explorer.jar --mcp} → servidor MCP via stdio (para o IBM Bob)</li>
 * </ul>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        ExplorerService servico = ExplorerService.padrao();

        if (args.length > 0 && args[0].equals("--mcp")) {
            new McpServer(servico).executar(System.in, System.out);
            return;
        }

        Path pastaSaida = Path.of(System.getProperty("geo.output", "output"));
        CommandRouter roteador = new CommandRouter(servico, pastaSaida);

        if (args.length > 0) {
            var resultado = roteador.executar(juntar(args));
            out.println(resultado.texto());
            if (!resultado.sucesso()) {
                System.exit(1);
            }
            return;
        }

        out.println("Geo-Explorer · explore trilhas de aprendizagem. Digite /help ou /sair.");
        var entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            out.print("geo> ");
            out.flush();
            String linha = entrada.readLine();
            if (linha == null || linha.strip().equalsIgnoreCase("/sair")) {
                out.println("Até a próxima!");
                return;
            }
            if (linha.isBlank()) {
                continue;
            }
            out.println(roteador.executar(linha).texto());
        }
    }

    /** Reconstrói a linha preservando argumentos com espaço (ex.: nomes no certificado). */
    static String juntar(String[] args) {
        var sb = new StringBuilder();
        for (String a : args) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(a.contains(" ") ? "\"" + a + "\"" : a);
        }
        return sb.toString();
    }
}
