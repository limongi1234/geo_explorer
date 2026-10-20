package br.com.dio.geoexplorer;

import br.com.dio.geoexplorer.repository.TrilhaRepository;
import br.com.dio.geoexplorer.service.ExplorerService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Random;

/** Fábrica de objetos determinísticos para os testes. */
public final class TestSupport {

    public static final Clock RELOGIO_FIXO =
            Clock.fixed(Instant.parse("2026-10-17T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private TestSupport() {
    }

    public static ExplorerService servico() {
        return servico(42L);
    }

    public static ExplorerService servico(long semente) {
        return new ExplorerService(TrilhaRepository.carregarPadrao(), new Random(semente), RELOGIO_FIXO);
    }
}
