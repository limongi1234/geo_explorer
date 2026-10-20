package br.com.dio.geoexplorer.command;

import br.com.dio.geoexplorer.service.ExplorerService;
import br.com.dio.geoexplorer.service.GeoExplorerException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Roteador central: recebe uma linha como {@code /desafio java avancado}, separa os argumentos
 * (respeitando aspas) e despacha para o comando correspondente.
 */
public final class CommandRouter {

    private final Map<String, Comando> comandos = new LinkedHashMap<>();

    public CommandRouter(ExplorerService servico, Path pastaSaida) {
        registrar(new Comandos.TrilhaComando(servico));
        registrar(new Comandos.DesafioComando(servico));
        registrar(new Comandos.CertificadoComando(servico, pastaSaida));
        registrar(new Comandos.CardsComando(servico));
        registrar(new Comandos.HelpComando(servico, this));
    }

    private void registrar(Comando c) {
        comandos.put(c.nome(), c);
    }

    public Collection<Comando> comandos() {
        return comandos.values();
    }

    /**
     * Executa uma linha de comando. Erros de uso viram mensagens amigáveis em vez de exceções,
     * para que o CLI nunca termine com stack trace na cara do usuário.
     */
    public Resultado executar(String linha) {
        try {
            List<String> partes = tokenizar(linha == null ? "" : linha.strip());
            if (partes.isEmpty()) {
                throw new GeoExplorerException("Digite um comando. Use /help para ver as opções.");
            }
            String primeiro = partes.getFirst();
            if (!primeiro.startsWith("/")) {
                throw new GeoExplorerException("Comandos começam com '/'. Use /help para ver as opções.");
            }
            String nome = primeiro.substring(1).toLowerCase(Locale.ROOT);
            Comando comando = comandos.get(nome);
            if (comando == null) {
                throw new GeoExplorerException("Comando desconhecido: /" + nome + ". Use /help para ver as opções.");
            }
            return Resultado.ok(comando.executar(partes.subList(1, partes.size())));
        } catch (GeoExplorerException e) {
            return Resultado.erro(e.getMessage());
        }
    }

    /** Separa por espaços, mantendo juntos os trechos entre aspas simples ou duplas. */
    static List<String> tokenizar(String linha) {
        List<String> tokens = new ArrayList<>();
        var atual = new StringBuilder();
        char aspas = 0;
        boolean temToken = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (aspas != 0) {
                if (c == aspas) {
                    aspas = 0;
                } else {
                    atual.append(c);
                }
            } else if (c == '"' || c == '\'') {
                aspas = c;
                temToken = true;
            } else if (Character.isWhitespace(c)) {
                if (temToken) {
                    tokens.add(atual.toString());
                    atual.setLength(0);
                    temToken = false;
                }
            } else {
                atual.append(c);
                temToken = true;
            }
        }
        if (aspas != 0) {
            throw new GeoExplorerException("Aspas não fechadas no comando.");
        }
        if (temToken) {
            tokens.add(atual.toString());
        }
        return tokens;
    }

    /** Resultado da execução de um comando. */
    public record Resultado(boolean sucesso, String texto) {
        static Resultado ok(String texto) {
            return new Resultado(true, texto);
        }

        static Resultado erro(String mensagem) {
            return new Resultado(false, "Erro: " + mensagem);
        }
    }
}
