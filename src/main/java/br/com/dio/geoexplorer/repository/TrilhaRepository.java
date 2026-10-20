package br.com.dio.geoexplorer.repository;

import br.com.dio.geoexplorer.json.Json;
import br.com.dio.geoexplorer.model.Card;
import br.com.dio.geoexplorer.model.Desafio;
import br.com.dio.geoexplorer.model.Modulo;
import br.com.dio.geoexplorer.model.Nivel;
import br.com.dio.geoexplorer.model.Trilha;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Acesso à base fictícia de trilhas (arquivo {@code trilhas.json}).
 *
 * <p>A base é carregada uma única vez e mantida em memória. A busca aceita o id da trilha
 * ou qualquer alias, sem diferenciar maiúsculas nem acentos.</p>
 */
public final class TrilhaRepository {

    public static final String RECURSO_PADRAO = "/trilhas.json";

    private final Map<String, Trilha> porId;
    private final Map<String, Trilha> porChave;

    private TrilhaRepository(List<Trilha> trilhas) {
        var ids = new LinkedHashMap<String, Trilha>();
        var chaves = new LinkedHashMap<String, Trilha>();
        for (Trilha t : trilhas) {
            if (ids.putIfAbsent(t.id(), t) != null) {
                throw new IllegalStateException("Trilha duplicada na base: " + t.id());
            }
            chaves.put(Nivel.normalizar(t.id()), t);
            chaves.put(Nivel.normalizar(t.nome()), t);
            t.aliases().forEach(a -> chaves.put(Nivel.normalizar(a), t));
        }
        this.porId = Collections.unmodifiableMap(ids);
        this.porChave = Collections.unmodifiableMap(chaves);
    }

    /** Carrega a base embutida no JAR. */
    public static TrilhaRepository carregarPadrao() {
        try (InputStream in = TrilhaRepository.class.getResourceAsStream(RECURSO_PADRAO)) {
            if (in == null) {
                throw new IllegalStateException("Base de trilhas não encontrada: " + RECURSO_PADRAO);
            }
            return deJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler a base de trilhas", e);
        }
    }

    /** Monta o repositório a partir de um JSON (útil em testes). */
    public static TrilhaRepository deJson(String json) {
        Map<String, Object> raiz = Json.parseObject(json);
        List<Trilha> trilhas = new ArrayList<>();
        for (Object item : lista(raiz, "trilhas")) {
            trilhas.add(paraTrilha(mapa(item)));
        }
        return new TrilhaRepository(trilhas);
    }

    public Optional<Trilha> buscar(String tecnologia) {
        if (tecnologia == null || tecnologia.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(porChave.get(Nivel.normalizar(tecnologia)));
    }

    public List<Trilha> listar() {
        return List.copyOf(porId.values());
    }

    // ------------------------------------------------------------------ conversão

    private static Trilha paraTrilha(Map<String, Object> m) {
        String id = texto(m, "id");
        List<Modulo> modulos = lista(m, "modulos").stream()
                .map(TrilhaRepository::mapa)
                .map(x -> new Modulo(texto(x, "titulo"), inteiro(x, "horas"), textos(x, "topicos")))
                .toList();
        List<Desafio> desafios = lista(m, "desafios").stream()
                .map(TrilhaRepository::mapa)
                .map(x -> new Desafio(
                        Nivel.de(texto(x, "nivel")).orElseThrow(() ->
                                new IllegalStateException("Nível inválido na trilha " + id + ": " + x.get("nivel"))),
                        texto(x, "titulo"),
                        texto(x, "descricao"),
                        textos(x, "criterios")))
                .toList();
        List<Card> cards = lista(m, "cards").stream()
                .map(TrilhaRepository::mapa)
                .map(x -> new Card(texto(x, "categoria"), texto(x, "pergunta"), texto(x, "resposta")))
                .toList();
        return new Trilha(id, texto(m, "nome"), textos(m, "aliases"), texto(m, "descricao"),
                inteiro(m, "cargaHoraria"), inteiro(m, "xp"), modulos, desafios, cards);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object o) {
        if (o instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new IllegalStateException("Esperado objeto JSON na base de trilhas");
    }

    private static List<?> lista(Map<String, Object> m, String campo) {
        Object v = m.get(campo);
        if (v == null) {
            return List.of();
        }
        if (v instanceof List<?> l) {
            return l;
        }
        throw new IllegalStateException("Campo '" + campo + "' deveria ser uma lista");
    }

    private static List<String> textos(Map<String, Object> m, String campo) {
        return lista(m, campo).stream().map(String::valueOf).toList();
    }

    private static String texto(Map<String, Object> m, String campo) {
        Object v = m.get(campo);
        if (v == null) {
            throw new IllegalStateException("Campo obrigatório ausente: " + campo);
        }
        return v.toString();
    }

    private static int inteiro(Map<String, Object> m, String campo) {
        if (m.get(campo) instanceof Number n) {
            return n.intValue();
        }
        throw new IllegalStateException("Campo numérico ausente: " + campo);
    }
}
