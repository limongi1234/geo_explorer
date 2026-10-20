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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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

        for (Trilha trilha : trilhas) {
            if (ids.putIfAbsent(trilha.id(), trilha) != null)
                throw new IllegalStateException("Trilha duplicada na base: " + trilha.id());

            indexarChaves(chaves, trilha);
        }

        this.porId = Collections.unmodifiableMap(ids);
        this.porChave = Collections.unmodifiableMap(chaves);
    }

    private static void indexarChaves(Map<String, Trilha> chaves, Trilha trilha) {
        chaves.put(Nivel.normalizar(trilha.id()), trilha);
        chaves.put(Nivel.normalizar(trilha.nome()), trilha);

        for (String alias : trilha.aliases())
            chaves.put(Nivel.normalizar(alias), trilha);
    }

    /** Carrega a base embutida no JAR. */
    public static TrilhaRepository carregarPadrao() {
        try (InputStream in = TrilhaRepository.class.getResourceAsStream(RECURSO_PADRAO)) {
            if (in == null)
                throw new IllegalStateException("Base de trilhas não encontrada: " + RECURSO_PADRAO);

            return deJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler a base de trilhas", e);
        }
    }

    /** Monta o repositório a partir de um JSON (útil em testes). */
    public static TrilhaRepository deJson(String json) {
        Map<String, Object> raiz = Json.parseObject(json);
        return new TrilhaRepository(converter(raiz, "trilhas", TrilhaRepository::paraTrilha));
    }

    public Optional<Trilha> buscar(String tecnologia) {
        if (tecnologia == null || tecnologia.isBlank())
            return Optional.empty();

        return Optional.ofNullable(porChave.get(Nivel.normalizar(tecnologia)));
    }

    public List<Trilha> listar() {
        return List.copyOf(porId.values());
    }

    // ------------------------------------------------------------------ conversão

    private static Trilha paraTrilha(Map<String, Object> objeto) {
        String id = texto(objeto, "id");
        return new Trilha(
                id,
                texto(objeto, "nome"),
                textos(objeto, "aliases"),
                texto(objeto, "descricao"),
                inteiro(objeto, "cargaHoraria"),
                inteiro(objeto, "xp"),
                converter(objeto, "modulos", TrilhaRepository::paraModulo),
                converter(objeto, "desafios", desafio -> paraDesafio(desafio, id)),
                converter(objeto, "cards", TrilhaRepository::paraCard));
    }

    private static Modulo paraModulo(Map<String, Object> objeto) {
        return new Modulo(texto(objeto, "titulo"), inteiro(objeto, "horas"), textos(objeto, "topicos"));
    }

    private static Desafio paraDesafio(Map<String, Object> objeto, String trilhaId) {
        Nivel nivel = Nivel.de(texto(objeto, "nivel")).orElseThrow(() -> new IllegalStateException(
                "Nível inválido na trilha " + trilhaId + ": " + objeto.get("nivel")));
        return new Desafio(nivel, texto(objeto, "titulo"), texto(objeto, "descricao"), textos(objeto, "criterios"));
    }

    private static Card paraCard(Map<String, Object> objeto) {
        return new Card(texto(objeto, "categoria"), texto(objeto, "pergunta"), texto(objeto, "resposta"));
    }

    private static <T> List<T> converter(Map<String, Object> objeto, String campo,
                                         Function<Map<String, Object>, T> conversor) {
        return lista(objeto, campo).stream().map(TrilhaRepository::mapa).map(conversor).toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object valor) {
        if (valor instanceof Map<?, ?> mapa)
            return (Map<String, Object>) mapa;

        throw new IllegalStateException("Esperado objeto JSON na base de trilhas");
    }

    private static List<?> lista(Map<String, Object> objeto, String campo) {
        Object valor = objeto.get(campo);

        if (valor == null)
            return List.of();

        if (valor instanceof List<?> lista)
            return lista;

        throw new IllegalStateException("Campo '" + campo + "' deveria ser uma lista");
    }

    private static List<String> textos(Map<String, Object> objeto, String campo) {
        return lista(objeto, campo).stream().map(String::valueOf).toList();
    }

    private static String texto(Map<String, Object> objeto, String campo) {
        Object valor = objeto.get(campo);

        if (valor == null)
            throw new IllegalStateException("Campo obrigatório ausente: " + campo);

        return valor.toString();
    }

    private static int inteiro(Map<String, Object> objeto, String campo) {
        if (objeto.get(campo) instanceof Number numero)
            return numero.intValue();

        throw new IllegalStateException("Campo numérico ausente: " + campo);
    }
}
