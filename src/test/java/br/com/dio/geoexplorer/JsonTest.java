package br.com.dio.geoexplorer;

import br.com.dio.geoexplorer.json.Json;
import br.com.dio.geoexplorer.json.JsonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Json: leitura e escrita")
class JsonTest {

    @Test
    @DisplayName("lê objetos, arrays, números, booleanos e null")
    void leTiposBasicos() {
        Map<String, Object> m = Json.parseObject(
                "{\"a\":1,\"b\":-2.5,\"c\":true,\"d\":null,\"e\":[1,\"x\"],\"f\":{\"g\":false}}");
        assertEquals(1L, m.get("a"));
        assertEquals(-2.5, m.get("b"));
        assertEquals(Boolean.TRUE, m.get("c"));
        assertNull(m.get("d"));
        assertEquals(List.of(1L, "x"), m.get("e"));
        assertEquals(Map.of("g", false), m.get("f"));
    }

    @Test
    @DisplayName("trata escapes e acentos")
    void escapes() {
        assertEquals("linha\n\"aspas\" ç é ã", Json.parse("\"linha\\n\\\"aspas\\\" ç \\u00e9 ã\""));
    }

    @Test
    @DisplayName("ida e volta preserva o conteúdo")
    void idaEVolta() {
        var original = new LinkedHashMap<String, Object>();
        original.put("texto", "Olá\t\"mundo\"\n");
        original.put("lista", List.of(1L, 2L, Map.of("ok", true)));
        original.put("nulo", null);
        assertEquals(original, Json.parse(Json.stringify(original)));
    }

    @Test
    @DisplayName("rejeita JSON malformado")
    void rejeitaInvalido() {
        assertThrows(JsonException.class, () -> Json.parse("{\"a\":}"));
        assertThrows(JsonException.class, () -> Json.parse("[1,2"));
        assertThrows(JsonException.class, () -> Json.parse("{} lixo"));
        assertThrows(JsonException.class, () -> Json.parse("\"sem fim"));
    }

    @Test
    @DisplayName("escreve caracteres de controle escapados")
    void escreveControle() {
        String json = Json.stringify("a\u0001b");
        assertTrue(json.contains("\\u0001"));
    }
}
