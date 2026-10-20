package br.com.dio.geoexplorer.mcp;

import br.com.dio.geoexplorer.model.Nivel;
import br.com.dio.geoexplorer.service.Certificado;
import br.com.dio.geoexplorer.service.ExplorerService;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catálogo das ferramentas MCP. Cada uma é um adaptador fino sobre o {@link ExplorerService}. */
final class Ferramentas {

    private Ferramentas() {
    }

    static Map<String, Ferramenta> criar(ExplorerService servico) {
        List<String> niveis = Arrays.stream(Nivel.values()).map(Nivel::codigo).toList();
        var mapa = new LinkedHashMap<String, Ferramenta>();

        add(mapa, new Ferramenta(
                "buscar_trilha",
                "Retorna o plano de estudos (módulos, tópicos, carga horária) de uma tecnologia em Markdown.",
                schema(Map.of("tecnologia", texto("Tecnologia ou alias, ex.: java, python, js", 1, 40)),
                        List.of("tecnologia")),
                a -> servico.buscarTrilha(str(a, "tecnologia"))));

        add(mapa, new Ferramenta(
                "gerar_desafio",
                "Sorteia um desafio prático de uma tecnologia. Se o nível não for informado, é sorteado.",
                schema(Map.of(
                                "tecnologia", texto("Tecnologia ou alias", 1, 40),
                                "nivel", Map.of("type", "string", "enum", niveis,
                                        "description", "Nível de dificuldade")),
                        List.of("tecnologia")),
                a -> servico.gerarDesafio(str(a, "tecnologia"), str(a, "nivel"))));

        add(mapa, new Ferramenta(
                "gerar_certificado",
                "Gera o certificado de conclusão de uma trilha em Markdown, com código de verificação.",
                schema(Map.of(
                                "nome", texto("Nome completo do aluno", 1, ExplorerService.MAX_NOME),
                                "tecnologia", texto("Tecnologia ou alias", 1, 40)),
                        List.of("nome", "tecnologia")),
                a -> {
                    Certificado c = servico.gerarCertificado(str(a, "nome"), str(a, "tecnologia"));
                    return c.markdown() + "\n> Nome sugerido para o arquivo: `" + c.nomeArquivo() + "`\n";
                }));

        add(mapa, new Ferramenta(
                "listar_tecnologias",
                "Lista as tecnologias com trilha disponível, com carga horária e número de desafios.",
                schema(Map.of(), List.of()),
                a -> servico.listarTecnologias()));

        add(mapa, new Ferramenta(
                "gerar_cards",
                "Gera flash cards de revisão de uma tecnologia, opcionalmente filtrados por categoria.",
                schema(Map.of(
                                "tecnologia", texto("Tecnologia ou alias", 1, 40),
                                "quantidade", Map.of("type", "integer", "minimum", 1,
                                        "maximum", ExplorerService.MAX_CARDS,
                                        "description", "Quantidade de cards (padrão 3)"),
                                "categoria", texto("Categoria, ex.: poo, streams, testes", 1, 40)),
                        List.of("tecnologia")),
                a -> servico.gerarCards(str(a, "tecnologia"),
                        a.get("quantidade") instanceof Number n ? n.intValue() : 3,
                        str(a, "categoria"))));

        return mapa;
    }

    private static void add(Map<String, Ferramenta> mapa, Ferramenta f) {
        mapa.put(f.nome(), f);
    }

    private static Map<String, Object> schema(Map<String, Object> propriedades, List<String> obrigatorios) {
        var s = new LinkedHashMap<String, Object>();
        s.put("type", "object");
        s.put("properties", new LinkedHashMap<>(propriedades));
        s.put("required", obrigatorios);
        s.put("additionalProperties", false);
        return s;
    }

    private static Map<String, Object> texto(String descricao, int min, int max) {
        return Map.of("type", "string", "description", descricao, "minLength", min, "maxLength", max);
    }

    private static String str(Map<String, Object> args, String campo) {
        Object v = args.get(campo);
        return v == null ? null : v.toString();
    }
}
