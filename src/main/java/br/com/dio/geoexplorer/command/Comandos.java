package br.com.dio.geoexplorer.command;

import br.com.dio.geoexplorer.service.Certificado;
import br.com.dio.geoexplorer.service.ExplorerService;
import br.com.dio.geoexplorer.service.GeoExplorerException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Implementações dos slash commands. Cada comando só valida argumentos e delega ao serviço. */
final class Comandos {

    private Comandos() {
    }

    record TrilhaComando(ExplorerService servico) implements Comando {
        public String nome() { return "trilha"; }
        public String uso() { return "/trilha <tecnologia>"; }
        public String descricao() { return "Mostra o plano de estudos da trilha em Markdown."; }

        public String executar(List<String> args) {
            exigirEntre(args, 1, 1, uso());
            return servico.buscarTrilha(args.getFirst());
        }
    }

    record DesafioComando(ExplorerService servico) implements Comando {
        public String nome() { return "desafio"; }
        public String uso() { return "/desafio <tecnologia> [iniciante|intermediario|avancado]"; }
        public String descricao() { return "Sorteia um desafio prático (nível aleatório se omitido)."; }

        public String executar(List<String> args) {
            exigirEntre(args, 1, 2, uso());
            return servico.gerarDesafio(args.getFirst(), argumentoOpcional(args, 1));
        }
    }

    record CertificadoComando(ExplorerService servico, Path pastaSaida) implements Comando {
        public String nome() { return "certificado"; }
        public String uso() { return "/certificado \"<nome>\" <tecnologia>"; }
        public String descricao() { return "Gera um certificado em Markdown e salva em output/."; }

        public String executar(List<String> args) {
            exigirEntre(args, 2, 2, uso());
            Certificado certificado = servico.gerarCertificado(args.get(0), args.get(1));
            Path destino = salvar(certificado);
            return certificado.markdown() + "\n> Arquivo salvo em `" + destino + "`\n";
        }

        private Path salvar(Certificado certificado) {
            Path destino = pastaSaida.resolve(certificado.nomeArquivo()).normalize();

            if (!destino.startsWith(pastaSaida.normalize()))
                throw new GeoExplorerException("Nome de arquivo inválido.");

            try {
                Files.createDirectories(pastaSaida);
                Files.writeString(destino, certificado.markdown(), StandardCharsets.UTF_8);
                return destino;
            } catch (IOException e) {
                throw new UncheckedIOException("Não foi possível salvar o certificado", e);
            }
        }
    }

    record CardsComando(ExplorerService servico) implements Comando {
        public String nome() { return "cards"; }
        public String uso() { return "/cards <tecnologia> [quantidade] [categoria]"; }

        public String descricao() {
            return "Gera flash cards de revisão (padrão: " + ExplorerService.QUANTIDADE_PADRAO_CARDS + ").";
        }

        public String executar(List<String> args) {
            exigirEntre(args, 1, 3, uso());
            return servico.gerarCards(args.getFirst(), lerQuantidade(argumentoOpcional(args, 1)),
                    argumentoOpcional(args, 2));
        }

        private int lerQuantidade(String texto) {
            if (texto == null)
                return ExplorerService.QUANTIDADE_PADRAO_CARDS;

            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                throw new GeoExplorerException("Quantidade deve ser um número inteiro. Uso: " + uso());
            }
        }
    }

    record HelpComando(ExplorerService servico, CommandRouter roteador) implements Comando {
        public String nome() { return "help"; }
        public String uso() { return "/help"; }
        public String descricao() { return "Lista os comandos e as tecnologias disponíveis."; }

        public String executar(List<String> args) {
            var md = new StringBuilder("# Geo-Explorer: comandos\n\n");
            md.append("| Comando | Descrição |\n|---|---|\n");

            for (Comando comando : roteador.comandos())
                md.append("| `").append(comando.uso()).append("` | ").append(comando.descricao()).append(" |\n");

            md.append("\n").append(servico.listarTecnologias());
            return md.toString();
        }
    }

    static void exigirEntre(List<String> args, int min, int max, String uso) {
        if (args.size() < min || args.size() > max)
            throw new GeoExplorerException("Argumentos inválidos. Uso: " + uso);
    }

    private static String argumentoOpcional(List<String> args, int indice) {
        return args.size() > indice ? args.get(indice) : null;
    }
}
