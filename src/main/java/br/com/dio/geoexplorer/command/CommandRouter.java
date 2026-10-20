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

    private static final String PREFIXO = "/";
    private static final String DICA_HELP = " Use /help para ver as opções.";

    private final Map<String, Comando> comandos = new LinkedHashMap<>();

    public CommandRouter(ExplorerService servico, Path pastaSaida) {
        registrar(new Comandos.TrilhaComando(servico));
        registrar(new Comandos.DesafioComando(servico));
        registrar(new Comandos.CertificadoComando(servico, pastaSaida));
        registrar(new Comandos.CardsComando(servico));
        registrar(new Comandos.HelpComando(servico, this));
    }

    private void registrar(Comando comando) {
        comandos.put(comando.nome(), comando);
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
            Comando comando = localizarComando(partes);
            return Resultado.ok(comando.executar(partes.subList(1, partes.size())));
        } catch (GeoExplorerException e) {
            return Resultado.erro(e.getMessage());
        }
    }

    private Comando localizarComando(List<String> partes) {
        if (partes.isEmpty())
            throw new GeoExplorerException("Digite um comando." + DICA_HELP);

        String primeiro = partes.getFirst();

        if (!primeiro.startsWith(PREFIXO))
            throw new GeoExplorerException("Comandos começam com '" + PREFIXO + "'." + DICA_HELP);

        String nome = primeiro.substring(PREFIXO.length()).toLowerCase(Locale.ROOT);
        Comando comando = comandos.get(nome);

        if (comando == null)
            throw new GeoExplorerException("Comando desconhecido: " + PREFIXO + nome + "." + DICA_HELP);

        return comando;
    }

    /** Separa por espaços, mantendo juntos os trechos entre aspas simples ou duplas. */
    static List<String> tokenizar(String linha) {
        var tokenizador = new Tokenizador();

        for (char c : linha.toCharArray())
            tokenizador.consumir(c);

        return tokenizador.finalizar();
    }

    private static final class Tokenizador {
        private static final char SEM_ASPAS = 0;

        private final List<String> tokens = new ArrayList<>();
        private final StringBuilder atual = new StringBuilder();
        private char aspasAbertas = SEM_ASPAS;
        private boolean temToken;

        void consumir(char c) {
            if (aspasAbertas != SEM_ASPAS)
                consumirDentroDeAspas(c);
            else if (c == '"' || c == '\'')
                abrirAspas(c);
            else if (Character.isWhitespace(c))
                fecharToken();
            else
                acrescentar(c);
        }

        List<String> finalizar() {
            if (aspasAbertas != SEM_ASPAS)
                throw new GeoExplorerException("Aspas não fechadas no comando.");

            fecharToken();
            return tokens;
        }

        private void consumirDentroDeAspas(char c) {
            if (c == aspasAbertas)
                aspasAbertas = SEM_ASPAS;
            else
                atual.append(c);
        }

        private void abrirAspas(char aspas) {
            aspasAbertas = aspas;
            temToken = true;
        }

        private void acrescentar(char c) {
            atual.append(c);
            temToken = true;
        }

        private void fecharToken() {
            if (!temToken)
                return;

            tokens.add(atual.toString());
            atual.setLength(0);
            temToken = false;
        }
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
