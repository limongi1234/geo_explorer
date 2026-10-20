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

    private Json() {
    }

    // ------------------------------------------------------------------ leitura

    public static Object parse(String texto) {
        if (texto == null) {
            throw new JsonException("Texto JSON nulo");
        }
        var parser = new Parser(texto);
        parser.pularEspacos();
        Object valor = parser.lerValor();
        parser.pularEspacos();
        if (!parser.fim()) {
            throw parser.erro("Conteúdo inesperado após o fim do JSON");
        }
        return valor;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String texto) {
        Object valor = parse(texto);
        if (valor instanceof Map<?, ?> mapa) {
            return (Map<String, Object>) mapa;
        }
        throw new JsonException("Esperado um objeto JSON");
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        boolean fim() {
            return pos >= s.length();
        }

        JsonException erro(String msg) {
            return new JsonException(msg + " (posição " + pos + ")");
        }

        void pularEspacos() {
            while (!fim() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        Object lerValor() {
            if (fim()) {
                throw erro("Fim inesperado do JSON");
            }
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> lerObjeto();
                case '[' -> lerArray();
                case '"' -> lerString();
                case 't' -> lerLiteral("true", Boolean.TRUE);
                case 'f' -> lerLiteral("false", Boolean.FALSE);
                case 'n' -> lerLiteral("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) {
                        yield lerNumero();
                    }
                    throw erro("Caractere inesperado '" + c + "'");
                }
            };
        }

        private Object lerLiteral(String literal, Object valor) {
            if (!s.startsWith(literal, pos)) {
                throw erro("Literal inválido");
            }
            pos += literal.length();
            return valor;
        }

        private Map<String, Object> lerObjeto() {
            var mapa = new LinkedHashMap<String, Object>();
            pos++; // {
            pularEspacos();
            if (!fim() && s.charAt(pos) == '}') {
                pos++;
                return mapa;
            }
            while (true) {
                pularEspacos();
                if (fim() || s.charAt(pos) != '"') {
                    throw erro("Esperada chave entre aspas");
                }
                String chave = lerString();
                pularEspacos();
                esperar(':');
                pularEspacos();
                mapa.put(chave, lerValor());
                pularEspacos();
                if (fim()) {
                    throw erro("Objeto não fechado");
                }
                char c = s.charAt(pos++);
                if (c == '}') {
                    return mapa;
                }
                if (c != ',') {
                    throw erro("Esperado ',' ou '}'");
                }
            }
        }

        private List<Object> lerArray() {
            var lista = new ArrayList<Object>();
            pos++; // [
            pularEspacos();
            if (!fim() && s.charAt(pos) == ']') {
                pos++;
                return lista;
            }
            while (true) {
                pularEspacos();
                lista.add(lerValor());
                pularEspacos();
                if (fim()) {
                    throw erro("Array não fechado");
                }
                char c = s.charAt(pos++);
                if (c == ']') {
                    return lista;
                }
                if (c != ',') {
                    throw erro("Esperado ',' ou ']'");
                }
            }
        }

        private String lerString() {
            pos++; // aspas de abertura
            var sb = new StringBuilder();
            while (true) {
                if (fim()) {
                    throw erro("String não fechada");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (fim()) {
                    throw erro("Escape incompleto");
                }
                char e = s.charAt(pos++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > s.length()) {
                            throw erro("Escape unicode incompleto");
                        }
                        try {
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw erro("Escape unicode inválido");
                        }
                        pos += 4;
                    }
                    default -> throw erro("Escape inválido '\\" + e + "'");
                }
            }
        }

        private Object lerNumero() {
            int inicio = pos;
            if (s.charAt(pos) == '-') {
                pos++;
            }
            boolean decimal = false;
            while (!fim()) {
                char c = s.charAt(pos);
                if (Character.isDigit(c)) {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    decimal = true;
                    pos++;
                } else {
                    break;
                }
            }
            String num = s.substring(inicio, pos);
            try {
                return decimal ? (Object) Double.parseDouble(num) : (Object) Long.parseLong(num);
            } catch (NumberFormatException ex) {
                throw erro("Número inválido '" + num + "'");
            }
        }

        private void esperar(char c) {
            if (fim() || s.charAt(pos) != c) {
                throw erro("Esperado '" + c + "'");
            }
            pos++;
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
            case Map<?, ?> mapa -> {
                sb.append('{');
                boolean primeiro = true;
                for (var entrada : mapa.entrySet()) {
                    if (!primeiro) {
                        sb.append(',');
                    }
                    primeiro = false;
                    escreverString(sb, String.valueOf(entrada.getKey()));
                    sb.append(':');
                    escrever(sb, entrada.getValue());
                }
                sb.append('}');
            }
            case Iterable<?> lista -> {
                sb.append('[');
                boolean primeiro = true;
                for (Object item : lista) {
                    if (!primeiro) {
                        sb.append(',');
                    }
                    primeiro = false;
                    escrever(sb, item);
                }
                sb.append(']');
            }
            default -> escreverString(sb, valor.toString());
        }
    }

    private static void escreverString(StringBuilder sb, String texto) {
        sb.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
