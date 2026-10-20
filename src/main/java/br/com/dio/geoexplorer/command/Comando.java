package br.com.dio.geoexplorer.command;

import java.util.List;

/** Um slash command do Geo-Explorer. */
public interface Comando {

    /** Nome sem a barra, ex.: {@code trilha}. */
    String nome();

    /** Exemplo de uso, ex.: {@code /trilha <tecnologia>}. */
    String uso();

    String descricao();

    /** Executa o comando e devolve a resposta em Markdown. */
    String executar(List<String> argumentos);
}
