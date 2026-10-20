package br.com.dio.geoexplorer;

import br.com.dio.geoexplorer.command.CommandRouter;
import br.com.dio.geoexplorer.command.CommandRouter.Resultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CommandRouter: slash commands")
class CommandRouterTest {

    private Path saida;
    private CommandRouter roteador;

    @BeforeEach
    void preparar() throws IOException {
        saida = Files.createTempDirectory("geo-explorer-teste");
        roteador = new CommandRouter(TestSupport.servico(), saida);
    }

    @Test
    @DisplayName("/help lista todos os comandos e tecnologias")
    void help() {
        Resultado r = roteador.executar("/help");
        assertTrue(r.sucesso());
        for (String comando : new String[]{"/trilha", "/desafio", "/certificado", "/cards", "/help"})
            assertTrue(r.texto().contains(comando), "faltou " + comando);

        assertTrue(r.texto().contains("| Python |"));
    }

    @Test
    @DisplayName("nome do comando não diferencia maiúsculas")
    void caixa() {
        assertTrue(roteador.executar("/TRILHA python").sucesso());
    }

    @Test
    @DisplayName("/certificado com nome entre aspas salva o arquivo em output/")
    void certificadoSalvaArquivo() throws IOException {
        Resultado r = roteador.executar("/certificado \"Ana Maria Souza\" react");
        assertTrue(r.sucesso(), r.texto());
        Path arquivo = saida.resolve("certificado-ana-maria-souza-react.md");
        assertTrue(Files.exists(arquivo));
        assertTrue(Files.readString(arquivo).contains("**Ana Maria Souza**"));
    }

    @Test
    @DisplayName("aceita aspas simples")
    void aspasSimples() {
        assertTrue(roteador.executar("/certificado 'Ana Souza' sql").sucesso());
    }

    @Test
    @DisplayName("/cards valida quantidade numérica")
    void cardsQuantidade() {
        assertTrue(roteador.executar("/cards python 2").sucesso());
        Resultado r = roteador.executar("/cards python muitos");
        assertFalse(r.sucesso());
        assertTrue(r.texto().contains("número inteiro"));
    }

    @Test
    @DisplayName("erros de uso viram mensagens amigáveis, nunca exceções")
    void errosAmigaveis() {
        assertErro("", "Digite um comando");
        assertErro("trilha java", "começam com '/'");
        assertErro("/voar", "Comando desconhecido: /voar");
        assertErro("/trilha", "Uso: /trilha <tecnologia>");
        assertErro("/trilha java python", "Uso: /trilha <tecnologia>");
        assertErro("/certificado \"Ana java", "Aspas não fechadas");
        assertErro("/desafio java expert", "Nível inválido");
    }

    private void assertErro(String linha, String trecho) {
        Resultado r = roteador.executar(linha);
        assertFalse(r.sucesso(), "deveria falhar: " + linha);
        assertTrue(r.texto().startsWith("Erro: "));
        assertTrue(r.texto().contains(trecho), "esperava '" + trecho + "' em: " + r.texto());
    }

    @Test
    @DisplayName("comandos aparecem na ordem de registro")
    void ordem() {
        assertEquals("trilha", roteador.comandos().iterator().next().nome());
    }
}
