package br.com.dio.geoexplorer.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor e escritor de JSON minimalista, sem dependências externas.
 *
 * <p>Mapeamento: objeto → {@code Map<String,Object>} (ordem preservada), array → {@code List<Object>},
 * string → {@code String}, número → {@code Long} ou {@code Double}, booleano → {@code Boolean},
 * null → {@code null}.</p>
 *
 * <p>Decisão de arquitetura: o projeto usa apenas a JDK para manter o CLI e o servidor MCP
 * leves e fáceis de rodar em qualquer máquina com Java 25.</p>
 */
public final class Json {

    private static final char PRIMEIRO_CARACTERE_IMPRIMIVEL = 0x20;
    private static final int DIGITOS_UNICODE = 4;

    private Json() {
    }

    // ------------------------------------------------------------------ leitura

    public static Object parse(String texto) {
        if (texto == null)
            throw new JsonException("Texto JSON nulo");

        var parser = new Parser(texto);
        parser.pularEspacos();
        Object valor = parser.lerValor();
        parser.pularEspacos();

        if (!parser.fim())
            throw parser.erro("Conteúdo inesperado após o fim do JSON");

        return valor;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String texto) {
        if (parse(texto) instanceof Map<?, ?> mapa)
            return (Map<String, Object>) mapa;

        throw new JsonException("Esperado um objeto JSON");
    }

    private static final class Parser {
        private final String texto;
        private int pos;

        Parser(String texto) {
            this.texto = texto;
        }

        boolean fim() {
            return pos >= texto.length();
        }

        JsonException erro(String mensagem) {
            return new JsonException(mensagem + " (posição " + pos + ")");
        }

        void pularEspacos() {
            while (!fim() && Character.isWhitespace(atual()))
                pos++;
        }

        Object lerValor() {
            if (fim())
                throw erro("Fim inesperado do JSON");

            char c = atual();
            return switch (c) {
                case '{' -> lerObjeto();
                case '[' -> lerArray();
                case '"' -> lerString();
                case 't' -> lerLiteral("true", Boolean.TRUE);
                case 'f' -> lerLiteral("false", Boolean.FALSE);
                case 'n' -> lerLiteral("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c))
                        yield lerNumero();

                    throw erro("Caractere inesperado '" + c + "'");
                }
            };
        }

        private char atual() {
            return texto.charAt(pos);
        }

        private char proximo() {
            return texto.charAt(pos++);
        }

        private boolean consumirSe(char esperado) {
            if (fim() || atual() != esperado)
                return false;

            pos++;
            return true;
        }

        private Object lerLiteral(String literal, Object valor) {
            if (!texto.startsWith(literal, pos))
                throw erro("Literal inválido");

            pos += literal.length();
            return valor;
        }

        private Map<String, Object> lerObjeto() {
            var mapa = new LinkedHashMap<String, Object>();
            pos++; // {
            pularEspacos();

            if (consumirSe('}'))
                return mapa;

            do {
                pularEspacos();
                String chave = lerChave();
                pularEspacos();
                esperar(':');
                pularEspacos();
                mapa.put(chave, lerValor());
                pularEspacos();
            } while (lerSeparador('}', "Objeto não fechado"));

            return mapa;
        }

        private String lerChave() {
            if (fim() || atual() != '"')
                throw erro("Esperada chave entre aspas");

            return lerString();
        }

        private List<Object> lerArray() {
            var lista = new ArrayList<Object>();
            pos++; // [
            pularEspacos();

            if (consumirSe(']'))
                return lista;

            do {
                pularEspacos();
                lista.add(lerValor());
                pularEspacos();
            } while (lerSeparador(']', "Array não fechado"));

            return lista;
        }

        /** Lê ',' (devolve true para continuar) ou o fechamento (devolve false). */
        private boolean lerSeparador(char fechamento, String mensagemNaoFechado) {
            if (fim())
                throw erro(mensagemNaoFechado);

            char c = proximo();

            if (c == fechamento)
                return false;

            if (c != ',')
                throw erro("Esperado ',' ou '" + fechamento + "'");

            return true;
        }

        private String lerString() {
            pos++; // aspas de abertura
            var sb = new StringBuilder();

            while (true) {
                if (fim())
                    throw erro("String não fechada");

                char c = proximo();

                if (c == '"')
                    return sb.toString();

                if (c == '\\')
                    sb.append(lerEscape());
                else
                    sb.append(c);
            }
        }

        private char lerEscape() {
            if (fim())
                throw erro("Escape incompleto");

            char escape = proximo();
            return switch (escape) {
                case '"' -> '"';
                case '\\' -> '\\';
                case '/' -> '/';
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> lerUnicode();
                default -> throw erro("Escape inválido '\\" + escape + "'");
            };
        }

        private char lerUnicode() {
            if (pos + DIGITOS_UNICODE > texto.length())
                throw erro("Escape unicode incompleto");

            try {
                char c = (char) Integer.parseInt(texto.substring(pos, pos + DIGITOS_UNICODE), 16);
                pos += DIGITOS_UNICODE;
                return c;
            } catch (NumberFormatException e) {
                throw erro("Escape unicode inválido");
            }
        }

        private Object lerNumero() {
            int inicio = pos;
            consumirSe('-');
            boolean decimal = false;

            while (!fim() && ehParteDeNumero(atual()))
                decimal |= !Character.isDigit(proximo());

            String numero = texto.substring(inicio, pos);
            try {
                return decimal ? (Object) Double.parseDouble(numero) : (Object) Long.parseLong(numero);
            } catch (NumberFormatException e) {
                throw erro("Número inválido '" + numero + "'");
            }
        }

        private static boolean ehParteDeNumero(char c) {
            return Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-';
        }

        private void esperar(char esperado) {
            if (!consumirSe(esperado))
                throw erro("Esperado '" + esperado + "'");
        }
    }

    // ------------------------------------------------------------------ escrita

    public static String stringify(Object valor) {
        var sb = new StringBuilder();
        escrever(sb, valor);
        return sb.toString();
    }

    private static void escrever(StringBuilder sb, Object valor) {
        switch (valor) {
            case null -> sb.append("null");
            case String texto -> escreverString(sb, texto);
            case Boolean b -> sb.append(b);
            case Double d when d.isNaN() || d.isInfinite() -> sb.append("null");
            case Float f when f.isNaN() || f.isInfinite() -> sb.append("null");
            case Number n -> sb.append(n);
            case Map<?, ?> mapa -> escreverObjeto(sb, mapa);
            case Iterable<?> lista -> escreverArray(sb, lista);
            default -> escreverString(sb, valor.toString());
        }
    }

    private static void escreverObjeto(StringBuilder sb, Map<?, ?> mapa) {
        sb.append('{');
        String separador = "";

        for (var entrada : mapa.entrySet()) {
            sb.append(separador);
            escreverString(sb, String.valueOf(entrada.getKey()));
            sb.append(':');
            escrever(sb, entrada.getValue());
            separador = ",";
        }

        sb.append('}');
    }

    private static void escreverArray(StringBuilder sb, Iterable<?> lista) {
        sb.append('[');
        String separador = "";

        for (Object item : lista) {
            sb.append(separador);
            escrever(sb, item);
            separador = ",";
        }

        sb.append(']');
    }

    private static void escreverString(StringBuilder sb, String texto) {
        sb.append('"');

        for (char c : texto.toCharArray())
            sb.append(escapar(c));

        sb.append('"');
    }

    private static String escapar(char c) {
        return switch (c) {
            case '"' -> "\\\"";
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            case '\f' -> "\\f";
            default -> c < PRIMEIRO_CARACTERE_IMPRIMIVEL ? String.format("\\u%04x", (int) c) : String.valueOf(c);
        };
    }
}
