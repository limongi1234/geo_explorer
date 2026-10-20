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
    public static final int MAX_NOME = 80;
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
        Trilha t = trilhaObrigatoria(tecnologia);
        var md = new StringBuilder();
        md.append("# Trilha de ").append(t.nome()).append("\n\n");
        md.append(t.descricao()).append("\n\n");
        md.append("- **Carga horária:** ").append(t.cargaHoraria()).append("h\n");
        md.append("- **XP total:** ").append(t.xp()).append("\n");
        md.append("- **Módulos:** ").append(t.modulos().size()).append("\n\n");
        md.append("## Plano de estudos\n\n");
        int i = 1;
        for (Modulo m : t.modulos()) {
            md.append("### ").append(i++).append(". ").append(m.titulo())
                    .append(" (").append(m.horas()).append("h)\n\n");
            m.topicos().forEach(topico -> md.append("- [ ] ").append(topico).append("\n"));
            md.append("\n");
        }
        md.append("> Próximo passo: `/desafio ").append(t.id()).append(" iniciante`\n");
        return md.toString();
    }

    // ------------------------------------------------------------------ /desafio

    public String gerarDesafio(String tecnologia, String nivelTexto) {
        Trilha t = trilhaObrigatoria(tecnologia);
        Nivel nivel;
        if (nivelTexto == null || nivelTexto.isBlank()) {
            nivel = Nivel.values()[aleatorio.nextInt(Nivel.values().length)];
        } else {
            nivel = Nivel.de(nivelTexto).orElseThrow(() -> new GeoExplorerException(
                    "Nível inválido: '" + nivelTexto + "'. Use: " + Nivel.opcoes() + "."));
        }
        List<Desafio> opcoes = t.desafiosDoNivel(nivel);
        if (opcoes.isEmpty()) {
            throw new GeoExplorerException("Ainda não há desafios de nível " + nivel.rotulo()
                    + " para " + t.nome() + ".");
        }
        Desafio d = opcoes.get(aleatorio.nextInt(opcoes.size()));

        var md = new StringBuilder();
        md.append("# Desafio de ").append(t.nome()).append(": ").append(d.titulo()).append("\n\n");
        md.append("**Nível:** ").append(nivel.rotulo()).append("\n\n");
        md.append("## Enunciado\n\n").append(d.descricao()).append("\n\n");
        md.append("## Critérios de aceite\n\n");
        d.criterios().forEach(c -> md.append("- [ ] ").append(c).append("\n"));
        md.append("\n> Ao concluir a trilha: `/certificado \"Seu Nome\" ").append(t.id()).append("`\n");
        return md.toString();
    }

    // ------------------------------------------------------------------ /certificado

    public Certificado gerarCertificado(String nome, String tecnologia) {
        String nomeLimpo = validarNome(nome);
        Trilha t = trilhaObrigatoria(tecnologia);
        LocalDate hoje = LocalDate.now(relogio);
        String codigo = codigoCertificado(nomeLimpo, t.id(), hoje);

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
                """.formatted(nomeLimpo, t.nome(), t.cargaHoraria(), t.xp(), hoje.format(DATA_BR), codigo);

        String arquivo = "certificado-" + slug(nomeLimpo) + "-" + t.id() + ".md";
        return new Certificado(nomeLimpo, t.nome(), t.cargaHoraria(), hoje, codigo, arquivo, markdown);
    }

    // ------------------------------------------------------------------ /cards

    public String gerarCards(String tecnologia, int quantidade, String categoria) {
        Trilha t = trilhaObrigatoria(tecnologia);
        if (quantidade < 1 || quantidade > MAX_CARDS) {
            throw new GeoExplorerException("Quantidade deve estar entre 1 e " + MAX_CARDS + ".");
        }
        List<Card> base = t.cards();
        if (categoria != null && !categoria.isBlank()) {
            String cat = Nivel.normalizar(categoria);
            base = base.stream().filter(c -> Nivel.normalizar(c.categoria()).equals(cat)).toList();
            if (base.isEmpty()) {
                List<String> categorias = t.cards().stream().map(Card::categoria).distinct().sorted().toList();
                throw new GeoExplorerException("Categoria '" + categoria + "' não existe em " + t.nome()
                        + ". Disponíveis: " + String.join(", ", categorias) + ".");
            }
        }
        List<Card> embaralhados = new ArrayList<>(base);
        Collections.shuffle(embaralhados, Random.from(aleatorio));
        List<Card> escolhidos = embaralhados.subList(0, Math.min(quantidade, embaralhados.size()));

        var md = new StringBuilder();
        md.append("# Flash cards de ").append(t.nome()).append("\n\n");
        int i = 1;
        for (Card c : escolhidos) {
            md.append("### Card ").append(i++).append(" · ").append(c.categoria()).append("\n\n");
            md.append("**Pergunta:** ").append(c.pergunta()).append("\n\n");
            md.append("<details><summary>Ver resposta</summary>\n\n")
                    .append(c.resposta()).append("\n\n</details>\n\n");
        }
        if (escolhidos.size() < quantidade) {
            md.append("> A base tem ").append(escolhidos.size())
                    .append(" card(s) para esse filtro; todos foram exibidos.\n");
        }
        return md.toString();
    }

    // ------------------------------------------------------------------ /help

    public String listarTecnologias() {
        var md = new StringBuilder("# Tecnologias disponíveis\n\n");
        md.append("| Tecnologia | Id | Carga horária | Módulos | Desafios |\n");
        md.append("|---|---|---|---|---|\n");
        repositorio.listar().stream()
                .sorted(Comparator.comparing(Trilha::nome, String.CASE_INSENSITIVE_ORDER))
                .forEach(t -> md.append("| ").append(t.nome())
                        .append(" | `").append(t.id()).append("`")
                        .append(" | ").append(t.cargaHoraria()).append("h")
                        .append(" | ").append(t.modulos().size())
                        .append(" | ").append(t.desafios().size()).append(" |\n"));
        return md.toString();
    }

    public List<String> idsDisponiveis() {
        return repositorio.listar().stream().map(Trilha::id).sorted().toList();
    }

    // ------------------------------------------------------------------ apoio

    private Trilha trilhaObrigatoria(String tecnologia) {
        if (tecnologia == null || tecnologia.isBlank()) {
            throw new GeoExplorerException("Informe uma tecnologia. Disponíveis: "
                    + String.join(", ", idsDisponiveis()) + ".");
        }
        return repositorio.buscar(tecnologia).orElseThrow(() -> {
            String sugestao = sugerir(tecnologia);
            return new GeoExplorerException("Tecnologia '" + tecnologia + "' não encontrada."
                    + (sugestao == null ? "" : " Você quis dizer '" + sugestao + "'?")
                    + " Disponíveis: " + String.join(", ", idsDisponiveis()) + ".");
        });
    }

    /** Sugere o id mais parecido (distância de Levenshtein até 2). */
    String sugerir(String digitado) {
        String alvo = Nivel.normalizar(digitado);
        String melhor = null;
        int menor = Integer.MAX_VALUE;
        for (String id : idsDisponiveis()) {
            int d = levenshtein(alvo, id);
            if (d < menor) {
                menor = d;
                melhor = id;
            }
        }
        return menor <= 2 ? melhor : null;
    }

    static int levenshtein(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] atual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            atual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int custo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                atual[j] = Math.min(Math.min(atual[j - 1] + 1, anterior[j] + 1), anterior[j - 1] + custo);
            }
            int[] tmp = anterior;
            anterior = atual;
            atual = tmp;
        }
        return anterior[b.length()];
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new GeoExplorerException("Informe o nome para o certificado.");
        }
        String limpo = nome.strip().replaceAll("\\s+", " ");
        if (limpo.length() > MAX_NOME) {
            throw new GeoExplorerException("O nome deve ter no máximo " + MAX_NOME + " caracteres.");
        }
        if (!limpo.matches("[\\p{L}\\p{M}' .-]+")) {
            throw new GeoExplorerException("O nome deve conter apenas letras, espaços, ponto, hífen ou apóstrofo.");
        }
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
            return "GEO-" + HexFormat.of().withUpperCase().formatHex(hash, 0, 5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
