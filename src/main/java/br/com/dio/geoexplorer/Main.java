package br.com.dio.geoexplorer;

import br.com.dio.geoexplorer.command.CommandRouter;
import br.com.dio.geoexplorer.mcp.McpServer;
import br.com.dio.geoexplorer.service.ExplorerService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

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

    private static final String OPCAO_MCP = "--mcp";
    private static final String COMANDO_SAIR = "/sair";

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        ExplorerService servico = ExplorerService.padrao();

        if (args.length > 0 && args[0].equals(OPCAO_MCP)) {
            new McpServer(servico).executar(System.in, System.out);
            return;
        }

        Path pastaSaida = Path.of(System.getProperty("geo.output", "output"));
        CommandRouter roteador = new CommandRouter(servico, pastaSaida);

        if (args.length > 0)
            executarUmComando(roteador, args, out);
        else
            executarInterativo(roteador, out);
    }

    private static void executarUmComando(CommandRouter roteador, String[] args, PrintStream out) {
        var resultado = roteador.executar(juntar(args));
        out.println(resultado.texto());

        if (!resultado.sucesso())
            System.exit(1);
    }

    private static void executarInterativo(CommandRouter roteador, PrintStream out) throws IOException {
        out.println("Geo-Explorer · explore trilhas de aprendizagem. Digite /help ou " + COMANDO_SAIR + ".");
        var entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        while (true) {
            out.print("geo> ");
            out.flush();
            String linha = entrada.readLine();

            if (ehSaida(linha)) {
                out.println("Até a próxima!");
                return;
            }

            if (!linha.isBlank())
                out.println(roteador.executar(linha).texto());
        }
    }

    private static boolean ehSaida(String linha) {
        return linha == null || linha.strip().equalsIgnoreCase(COMANDO_SAIR);
    }

    /** Reconstrói a linha preservando argumentos com espaço (ex.: nomes no certificado). */
    static String juntar(String[] args) {
        return Arrays.stream(args)
                .map(arg -> arg.contains(" ") ? "\"" + arg + "\"" : arg)
                .collect(Collectors.joining(" "));
    }
}
