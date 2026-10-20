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

    private static final int MAX_TEXTO_CURTO = 40;

    private Ferramentas() {
    }

    static Map<String, Ferramenta> criar(ExplorerService servico) {
        List<String> niveis = Arrays.stream(Nivel.values()).map(Nivel::codigo).toList();
        var mapa = new LinkedHashMap<String, Ferramenta>();

        adicionar(mapa, new Ferramenta(
                "buscar_trilha",
                "Retorna o plano de estudos (módulos, tópicos, carga horária) de uma tecnologia em Markdown.",
                schema(Map.of("tecnologia", campoTexto("Tecnologia ou alias, ex.: java, python, js")),
                        List.of("tecnologia")),
                args -> servico.buscarTrilha(argumento(args, "tecnologia"))));

        adicionar(mapa, new Ferramenta(
                "gerar_desafio",
                "Sorteia um desafio prático de uma tecnologia. Se o nível não for informado, é sorteado.",
                schema(Map.of(
                                "tecnologia", campoTexto("Tecnologia ou alias"),
                                "nivel", Map.of("type", "string", "enum", niveis,
                                        "description", "Nível de dificuldade")),
                        List.of("tecnologia")),
                args -> servico.gerarDesafio(argumento(args, "tecnologia"), argumento(args, "nivel"))));

        adicionar(mapa, new Ferramenta(
                "gerar_certificado",
                "Gera o certificado de conclusão de uma trilha em Markdown, com código de verificação.",
                schema(Map.of(
                                "nome", campoTexto("Nome completo do aluno", ExplorerService.MAX_NOME),
                                "tecnologia", campoTexto("Tecnologia ou alias")),
                        List.of("nome", "tecnologia")),
                args -> {
                    Certificado certificado = servico.gerarCertificado(
                            argumento(args, "nome"), argumento(args, "tecnologia"));
                    return certificado.markdown()
                            + "\n> Nome sugerido para o arquivo: `" + certificado.nomeArquivo() + "`\n";
                }));

        adicionar(mapa, new Ferramenta(
                "listar_tecnologias",
                "Lista as tecnologias com trilha disponível, com carga horária e número de desafios.",
                schema(Map.of(), List.of()),
                args -> servico.listarTecnologias()));

        adicionar(mapa, new Ferramenta(
                "gerar_cards",
                "Gera flash cards de revisão de uma tecnologia, opcionalmente filtrados por categoria.",
                schema(Map.of(
                                "tecnologia", campoTexto("Tecnologia ou alias"),
                                "quantidade", Map.of("type", "integer", "minimum", 1,
                                        "maximum", ExplorerService.MAX_CARDS,
                                        "description", "Quantidade de cards (padrão "
                                                + ExplorerService.QUANTIDADE_PADRAO_CARDS + ")"),
                                "categoria", campoTexto("Categoria, ex.: poo, streams, testes")),
                        List.of("tecnologia")),
                args -> servico.gerarCards(argumento(args, "tecnologia"), quantidade(args),
                        argumento(args, "categoria"))));

        return mapa;
    }

    private static void adicionar(Map<String, Ferramenta> mapa, Ferramenta ferramenta) {
        mapa.put(ferramenta.nome(), ferramenta);
    }

    private static Map<String, Object> schema(Map<String, Object> propriedades, List<String> obrigatorios) {
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", new LinkedHashMap<>(propriedades));
        schema.put("required", obrigatorios);
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> campoTexto(String descricao) {
        return campoTexto(descricao, MAX_TEXTO_CURTO);
    }

    private static Map<String, Object> campoTexto(String descricao, int tamanhoMaximo) {
        return Map.of("type", "string", "description", descricao, "minLength", 1, "maxLength", tamanhoMaximo);
    }

    private static String argumento(Map<String, Object> args, String campo) {
        Object valor = args.get(campo);
        return valor == null ? null : valor.toString();
    }

    private static int quantidade(Map<String, Object> args) {
        return args.get("quantidade") instanceof Number numero
                ? numero.intValue()
                : ExplorerService.QUANTIDADE_PADRAO_CARDS;
    }
}
