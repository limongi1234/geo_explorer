package br.com.dio.geoexplorer.service;

import br.com.dio.geoexplorer.model.Card;
import br.com.dio.geoexplorer.model.Desafio;
import br.com.dio.geoexplorer.model.Modulo;
import br.com.dio.geoexplorer.model.Nivel;
import br.com.dio.geoexplorer.model.Trilha;
import br.com.dio.geoexplorer.repository.TrilhaRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Regras de negócio do Geo-Explorer.
 *
 * <p>É a <strong>única</strong> fonte de lógica: tanto os slash commands do CLI quanto as
 * ferramentas do servidor MCP chamam esta classe. Assim os dois canais nunca divergem.</p>
 *
 * <p>Aleatoriedade e data são injetadas ({@link RandomGenerator} e {@link Clock}) para que os
 * testes sejam determinísticos.</p>
 */
public final class ExplorerService {

    public static final int MAX_CARDS = 20;
    public static final int QUANTIDADE_PADRAO_CARDS = 3;
    public static final int MAX_NOME = 80;
    private static final int DISTANCIA_MAXIMA_SUGESTAO = 2;
    private static final int BYTES_CODIGO_CERTIFICADO = 5;
    private static final String PADRAO_NOME_VALIDO = "[\\p{L}\\p{M}' .-]+";
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TrilhaRepository repositorio;
    private final RandomGenerator aleatorio;
    private final Clock relogio;

    public ExplorerService(TrilhaRepository repositorio, RandomGenerator aleatorio, Clock relogio) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.aleatorio = Objects.requireNonNull(aleatorio);
        this.relogio = Objects.requireNonNull(relogio);
    }

    public static ExplorerService padrao() {
        return new ExplorerService(TrilhaRepository.carregarPadrao(), new Random(), Clock.systemDefaultZone());
    }

    // ------------------------------------------------------------------ /trilha

    public String buscarTrilha(String tecnologia) {
        Trilha trilha = trilhaObrigatoria(tecnologia);
        List<Modulo> modulos = trilha.modulos();

        var md = new StringBuilder();
        md.append("# Trilha de ").append(trilha.nome()).append("\n\n");
        md.append(trilha.descricao()).append("\n\n");
        md.append("- **Carga horária:** ").append(trilha.cargaHoraria()).append("h\n");
        md.append("- **XP total:** ").append(trilha.xp()).append("\n");
        md.append("- **Módulos:** ").append(modulos.size()).append("\n\n");
        md.append("## Plano de estudos\n\n");

        for (int i = 0; i < modulos.size(); i++)
            appendModulo(md, i + 1, modulos.get(i));

        md.append("> Próximo passo: `/desafio ").append(trilha.id()).append(" iniciante`\n");
        return md.toString();
    }

    private static void appendModulo(StringBuilder md, int numero, Modulo modulo) {
        md.append("### ").append(numero).append(". ").append(modulo.titulo())
                .append(" (").append(modulo.horas()).append("h)\n\n");

        for (String topico : modulo.topicos())
            md.append("- [ ] ").append(topico).append("\n");

        md.append("\n");
    }

    // ------------------------------------------------------------------ /desafio

    public String gerarDesafio(String tecnologia, String nivelTexto) {
        Trilha trilha = trilhaObrigatoria(tecnologia);
        Nivel nivel = resolverNivel(nivelTexto);
        Desafio desafio = sortearDesafio(trilha, nivel);

        var md = new StringBuilder();
        md.append("# Desafio de ").append(trilha.nome()).append(": ").append(desafio.titulo()).append("\n\n");
        md.append("**Nível:** ").append(nivel.rotulo()).append("\n\n");
        md.append("## Enunciado\n\n").append(desafio.descricao()).append("\n\n");
        md.append("## Critérios de aceite\n\n");

        for (String criterio : desafio.criterios())
            md.append("- [ ] ").append(criterio).append("\n");

        md.append("\n> Ao concluir a trilha: `/certificado \"Seu Nome\" ").append(trilha.id()).append("`\n");
        return md.toString();
    }

    private Nivel resolverNivel(String nivelTexto) {
        if (estaVazio(nivelTexto))
            return sortear(List.of(Nivel.values()));

        return Nivel.de(nivelTexto).orElseThrow(() -> new GeoExplorerException(
                "Nível inválido: '" + nivelTexto + "'. Use: " + Nivel.opcoes() + "."));
    }

    private Desafio sortearDesafio(Trilha trilha, Nivel nivel) {
        List<Desafio> opcoes = trilha.desafiosDoNivel(nivel);

        if (opcoes.isEmpty())
            throw new GeoExplorerException("Ainda não há desafios de nível " + nivel.rotulo()
                    + " para " + trilha.nome() + ".");

        return sortear(opcoes);
    }

    // ------------------------------------------------------------------ /certificado

    public Certificado gerarCertificado(String nome, String tecnologia) {
        String nomeLimpo = validarNome(nome);
        Trilha trilha = trilhaObrigatoria(tecnologia);
        LocalDate hoje = LocalDate.now(relogio);
        String codigo = codigoCertificado(nomeLimpo, trilha.id(), hoje);

        String markdown = """
                # Certificado de Conclusão

                Certificamos que **%s** concluiu a trilha **%s** no Geo-Explorer,
                com carga horária de **%dh** e **%d XP** conquistados.

                | Campo | Valor |
                |---|---|
                | Data de emissão | %s |
                | Código de verificação | `%s` |

                ---
                *Emitido pelo Geo-Explorer. Base de trilhas fictícia, criada para fins educacionais.*
                """.formatted(nomeLimpo, trilha.nome(), trilha.cargaHoraria(), trilha.xp(),
                hoje.format(DATA_BR), codigo);

        String arquivo = "certificado-" + slug(nomeLimpo) + "-" + trilha.id() + ".md";
        return new Certificado(nomeLimpo, trilha.nome(), trilha.cargaHoraria(), hoje, codigo, arquivo, markdown);
    }

    // ------------------------------------------------------------------ /cards

    public String gerarCards(String tecnologia, int quantidade, String categoria) {
        Trilha trilha = trilhaObrigatoria(tecnologia);

        if (quantidade < 1 || quantidade > MAX_CARDS)
            throw new GeoExplorerException("Quantidade deve estar entre 1 e " + MAX_CARDS + ".");

        List<Card> escolhidos = sortearCards(filtrarPorCategoria(trilha, categoria), quantidade);

        var md = new StringBuilder();
        md.append("# Flash cards de ").append(trilha.nome()).append("\n\n");

        for (int i = 0; i < escolhidos.size(); i++)
            appendCard(md, i + 1, escolhidos.get(i));

        if (escolhidos.size() < quantidade)
            md.append("> A base tem ").append(escolhidos.size())
                    .append(" card(s) para esse filtro; todos foram exibidos.\n");

        return md.toString();
    }

    private static List<Card> filtrarPorCategoria(Trilha trilha, String categoria) {
        if (estaVazio(categoria))
            return trilha.cards();

        String categoriaNormalizada = Nivel.normalizar(categoria);
        List<Card> filtrados = trilha.cards().stream()
                .filter(card -> Nivel.normalizar(card.categoria()).equals(categoriaNormalizada))
                .toList();

        if (filtrados.isEmpty())
            throw new GeoExplorerException("Categoria '" + categoria + "' não existe em " + trilha.nome()
                    + ". Disponíveis: " + String.join(", ", categoriasDisponiveis(trilha)) + ".");

        return filtrados;
    }

    private static List<String> categoriasDisponiveis(Trilha trilha) {
        return trilha.cards().stream().map(Card::categoria).distinct().sorted().toList();
    }

    private List<Card> sortearCards(List<Card> base, int quantidade) {
        List<Card> embaralhados = new ArrayList<>(base);
        Collections.shuffle(embaralhados, Random.from(aleatorio));
        return embaralhados.subList(0, Math.min(quantidade, embaralhados.size()));
    }

    private static void appendCard(StringBuilder md, int numero, Card card) {
        md.append("### Card ").append(numero).append(" · ").append(card.categoria()).append("\n\n");
        md.append("**Pergunta:** ").append(card.pergunta()).append("\n\n");
        md.append("<details><summary>Ver resposta</summary>\n\n")
                .append(card.resposta()).append("\n\n</details>\n\n");
    }

    // ------------------------------------------------------------------ /help

    public String listarTecnologias() {
        var md = new StringBuilder("# Tecnologias disponíveis\n\n");
        md.append("| Tecnologia | Id | Carga horária | Módulos | Desafios |\n");
        md.append("|---|---|---|---|---|\n");
        repositorio.listar().stream()
                .sorted(Comparator.comparing(Trilha::nome, String.CASE_INSENSITIVE_ORDER))
                .forEach(trilha -> md.append("| ").append(trilha.nome())
                        .append(" | `").append(trilha.id()).append("`")
                        .append(" | ").append(trilha.cargaHoraria()).append("h")
                        .append(" | ").append(trilha.modulos().size())
                        .append(" | ").append(trilha.desafios().size()).append(" |\n"));
        return md.toString();
    }

    public List<String> idsDisponiveis() {
        return repositorio.listar().stream().map(Trilha::id).sorted().toList();
    }

    // ------------------------------------------------------------------ apoio

    private Trilha trilhaObrigatoria(String tecnologia) {
        if (estaVazio(tecnologia))
            throw new GeoExplorerException("Informe uma tecnologia. Disponíveis: " + listaDeIds() + ".");

        return repositorio.buscar(tecnologia)
                .orElseThrow(() -> tecnologiaNaoEncontrada(tecnologia));
    }

    private GeoExplorerException tecnologiaNaoEncontrada(String tecnologia) {
        String sugestao = sugerir(tecnologia);
        String dica = sugestao == null ? "" : " Você quis dizer '" + sugestao + "'?";
        return new GeoExplorerException("Tecnologia '" + tecnologia + "' não encontrada."
                + dica + " Disponíveis: " + listaDeIds() + ".");
    }

    private String listaDeIds() {
        return String.join(", ", idsDisponiveis());
    }

    /** Sugere o id mais parecido (distância de Levenshtein até {@value #DISTANCIA_MAXIMA_SUGESTAO}). */
    String sugerir(String digitado) {
        String alvo = Nivel.normalizar(digitado);
        return idsDisponiveis().stream()
                .min(Comparator.comparingInt(id -> levenshtein(alvo, id)))
                .filter(id -> levenshtein(alvo, id) <= DISTANCIA_MAXIMA_SUGESTAO)
                .orElse(null);
    }

    static int levenshtein(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] atual = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++)
            anterior[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            atual[0] = i;

            for (int j = 1; j <= b.length(); j++) {
                int custo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                atual[j] = Math.min(Math.min(atual[j - 1] + 1, anterior[j] + 1), anterior[j - 1] + custo);
            }

            int[] linhaReaproveitada = anterior;
            anterior = atual;
            atual = linhaReaproveitada;
        }
        return anterior[b.length()];
    }

    private static String validarNome(String nome) {
        if (estaVazio(nome))
            throw new GeoExplorerException("Informe o nome para o certificado.");

        String limpo = nome.strip().replaceAll("\\s+", " ");

        if (limpo.length() > MAX_NOME)
            throw new GeoExplorerException("O nome deve ter no máximo " + MAX_NOME + " caracteres.");

        if (!limpo.matches(PADRAO_NOME_VALIDO))
            throw new GeoExplorerException("O nome deve conter apenas letras, espaços, ponto, hífen ou apóstrofo.");

        return limpo;
    }

    /** Gera um nome de arquivo seguro: sem acentos, minúsculo, só letras, números e hífen. */
    static String slug(String texto) {
        String s = Nivel.normalizar(texto).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "aluno" : s;
    }

    static String codigoCertificado(String nome, String trilhaId, LocalDate data) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest((nome.toLowerCase(Locale.ROOT) + "|" + trilhaId + "|" + data)
                    .getBytes(StandardCharsets.UTF_8));
            return "GEO-" + HexFormat.of().withUpperCase().formatHex(hash, 0, BYTES_CODIGO_CERTIFICADO);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static boolean estaVazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private <T> T sortear(List<T> opcoes) {
        return opcoes.get(aleatorio.nextInt(opcoes.size()));
    }
}
