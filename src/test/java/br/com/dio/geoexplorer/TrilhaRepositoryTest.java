package br.com.dio.geoexplorer;

import br.com.dio.geoexplorer.model.Nivel;
import br.com.dio.geoexplorer.model.Trilha;
import br.com.dio.geoexplorer.repository.TrilhaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TrilhaRepository: base fictícia de trilhas")
class TrilhaRepositoryTest {

    private final TrilhaRepository repo = TrilhaRepository.carregarPadrao();

    @Test
    @DisplayName("carrega as cinco trilhas da base")
    void carregaBase() {
        List<String> ids = repo.listar().stream().map(Trilha::id).sorted().toList();
        assertEquals(List.of("java", "javascript", "python", "react", "sql"), ids);
    }

    @Test
    @DisplayName("encontra por id, nome e alias sem diferenciar caixa e acento")
    void buscaFlexivel() {
        assertEquals("javascript", repo.buscar("JS").orElseThrow().id());
        assertEquals("javascript", repo.buscar("  node ").orElseThrow().id());
        assertEquals("python", repo.buscar("Py").orElseThrow().id());
        assertEquals("java", repo.buscar("JAVA").orElseThrow().id());
        assertFalse(repo.buscar("cobol").isPresent());
        assertFalse(repo.buscar("").isPresent());
        assertFalse(repo.buscar(null).isPresent());
    }

    @Test
    @DisplayName("toda trilha tem módulos, cards e desafio em todos os níveis")
    void integridadeDaBase() {
        for (Trilha t : repo.listar()) {
            assertFalse(t.modulos().isEmpty(), "sem módulos: " + t.id());
            assertFalse(t.cards().isEmpty(), "sem cards: " + t.id());
            for (Nivel n : Nivel.values()) {
                assertFalse(t.desafiosDoNivel(n).isEmpty(), t.id() + " sem desafio " + n);
            }
            int somaHoras = t.modulos().stream().mapToInt(m -> m.horas()).sum();
            assertEquals(t.cargaHoraria(), somaHoras, "carga horária inconsistente: " + t.id());
        }
    }

    @Test
    @DisplayName("rejeita base com trilha duplicada ou nível inválido")
    void rejeitaBaseInvalida() {
        String duplicada = """
                {"trilhas":[
                  {"id":"x","nome":"X","descricao":"d","cargaHoraria":1,"xp":1},
                  {"id":"x","nome":"Y","descricao":"d","cargaHoraria":1,"xp":1}]}""";
        assertThrows(IllegalStateException.class, () -> TrilhaRepository.deJson(duplicada));

        String nivelRuim = """
                {"trilhas":[{"id":"x","nome":"X","descricao":"d","cargaHoraria":1,"xp":1,
                  "desafios":[{"nivel":"expert","titulo":"t","descricao":"d"}]}]}""";
        assertThrows(IllegalStateException.class, () -> TrilhaRepository.deJson(nivelRuim));
    }

    @Test
    @DisplayName("Nivel aceita variações com acento e maiúsculas")
    void nivelFlexivel() {
        assertEquals(Nivel.INTERMEDIARIO, Nivel.de("Intermediário").orElseThrow());
        assertEquals(Nivel.AVANCADO, Nivel.de("AVANÇADO").orElseThrow());
        assertTrue(Nivel.de("expert").isEmpty());
    }
}
