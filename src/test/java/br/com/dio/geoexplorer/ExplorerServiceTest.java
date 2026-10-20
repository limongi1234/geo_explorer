package br.com.dio.geoexplorer;

import br.com.dio.geoexplorer.service.Certificado;
import br.com.dio.geoexplorer.service.ExplorerService;
import br.com.dio.geoexplorer.service.GeoExplorerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ExplorerService: regras de negócio")
class ExplorerServiceTest {

    private final ExplorerService servico = TestSupport.servico();

    @Test
    @DisplayName("/trilha mostra cabeçalho, carga horária e todos os módulos")
    void buscarTrilha() {
        String md = servico.buscarTrilha("java");
        assertTrue(md.startsWith("# Trilha de Java"));
        assertTrue(md.contains("**Carga horária:** 60h"));
        assertTrue(md.contains("### 5. Qualidade e testes (12h)"));
        assertTrue(md.contains("- [ ] Virtual threads"));
    }

    @Test
    @DisplayName("tecnologia inexistente sugere a mais parecida")
    void sugereTecnologia() {
        var e = assertThrows(GeoExplorerException.class, () -> servico.buscarTrilha("pyton"));
        assertTrue(e.getMessage().contains("Você quis dizer 'python'?"));

        var semSugestao = assertThrows(GeoExplorerException.class, () -> servico.buscarTrilha("kubernetes"));
        assertFalse(semSugestao.getMessage().contains("Você quis dizer"));
        assertTrue(semSugestao.getMessage().contains("Disponíveis: java, javascript, python, react, sql"));
    }

    @Test
    @DisplayName("/desafio respeita o nível pedido")
    void desafioNivel() {
        for (int semente = 0; semente < 20; semente++)
            assertTrue(TestSupport.servico(semente).gerarDesafio("java", "avancado").contains("**Nível:** Avançado"));
    }

    @Test
    @DisplayName("/desafio sem nível sorteia e é determinístico com a mesma semente")
    void desafioSemNivel() {
        assertEquals(TestSupport.servico(7).gerarDesafio("sql", null),
                TestSupport.servico(7).gerarDesafio("sql", null));
        assertTrue(servico.gerarDesafio("sql", "").contains("**Nível:**"));
    }

    @Test
    @DisplayName("/desafio rejeita nível inválido listando as opções")
    void desafioNivelInvalido() {
        var e = assertThrows(GeoExplorerException.class, () -> servico.gerarDesafio("java", "expert"));
        assertTrue(e.getMessage().contains("iniciante, intermediario, avancado"));
    }

    @Test
    @DisplayName("/certificado gera Markdown, código estável e nome de arquivo seguro")
    void certificado() {
        Certificado c = servico.gerarCertificado("  Renato   Lemos ", "JS");
        assertEquals("Renato Lemos", c.nome());
        assertEquals("JavaScript", c.trilha());
        assertEquals(LocalDate.of(2026, 10, 17), c.data());
        assertEquals("certificado-renato-lemos-javascript.md", c.nomeArquivo());
        assertTrue(c.codigo().matches("GEO-[0-9A-F]{10}"));
        assertTrue(c.markdown().contains("**Renato Lemos** concluiu a trilha **JavaScript**"));
        assertTrue(c.markdown().contains("17/10/2026"));
        assertEquals(c.codigo(), servico.gerarCertificado("renato lemos", "javascript").codigo());
    }

    @Test
    @DisplayName("/certificado aceita acentos e apóstrofo, mas bloqueia caracteres perigosos")
    void certificadoValidacaoNome() {
        Certificado c = servico.gerarCertificado("João D'Ávila", "java");
        assertEquals("certificado-joao-d-avila-java.md", c.nomeArquivo());

        assertThrows(GeoExplorerException.class, () -> servico.gerarCertificado("", "java"));
        assertThrows(GeoExplorerException.class, () -> servico.gerarCertificado("../../etc/passwd", "java"));
        assertThrows(GeoExplorerException.class, () -> servico.gerarCertificado("<script>", "java"));
        assertThrows(GeoExplorerException.class, () -> servico.gerarCertificado("a".repeat(81), "java"));
    }

    @Test
    @DisplayName("/cards limita quantidade, filtra categoria e avisa quando faltam cards")
    void cards() {
        String tres = servico.gerarCards("java", 3, null);
        assertEquals(3, tres.split("### Card ", -1).length - 1);

        String filtrado = servico.gerarCards("java", 5, "Testes");
        assertTrue(filtrado.contains("· testes"));
        assertTrue(filtrado.contains("todos foram exibidos"));

        assertThrows(GeoExplorerException.class, () -> servico.gerarCards("java", 0, null));
        assertThrows(GeoExplorerException.class, () -> servico.gerarCards("java", 21, null));
        var e = assertThrows(GeoExplorerException.class, () -> servico.gerarCards("java", 2, "culinaria"));
        assertTrue(e.getMessage().contains("Disponíveis:"));
    }

    @Test
    @DisplayName("listarTecnologias monta a tabela em ordem alfabética")
    void listar() {
        String md = servico.listarTecnologias();
        assertTrue(md.indexOf("| Java |") < md.indexOf("| JavaScript |"));
        assertTrue(md.indexOf("| React |") < md.indexOf("| SQL |"));
    }
}
