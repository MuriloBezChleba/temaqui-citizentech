import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

class PrepararOracle {
    private static final List<String> TABELAS =
            List.of("TA_MORADOR", "TA_LOJA", "TA_PRODUTO", "TA_ESTOQUE", "TA_RESERVA");

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !List.of("instalar", "verificar").contains(args[0])) {
            throw new IllegalArgumentException("Informe instalar ou verificar.");
        }
        try (Connection banco = DriverManager.getConnection(
                ambiente("ORACLE_URL"), ambiente("ORACLE_USER"), ambiente("ORACLE_PASSWORD"))) {
            if (args[0].equals("instalar")) {
                instalar(banco);
            }
            verificar(banco);
        }
    }

    private static String ambiente(String nome) {
        String valor = System.getenv(nome);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Variavel obrigatoria: " + nome);
        }
        return valor;
    }

    private static List<String> comandos(String arquivo) throws Exception {
        String texto = Files.readString(Path.of("database", arquivo));
        return Arrays.stream(texto.replaceAll("(?m)^\\s*--[^\\r\\n]*", "").split(";"))
                .map(String::strip).filter(sql -> !sql.isEmpty()).toList();
    }

    private static void instalar(Connection banco) throws Exception {
        List<String> ddl = comandos("01_DDL.SQL");
        List<String> dml = comandos("02_DML.SQL");
        try (var consulta = banco.prepareStatement("SELECT 1 FROM USER_TABLES WHERE TABLE_NAME = ?")) {
            for (String tabela : TABELAS) {
                consulta.setString(1, tabela);
                try (var existentes = consulta.executeQuery()) {
                    if (existentes.next()) {
                        throw new IllegalStateException(tabela + " ja existe. Use verificar para consultar a carga.");
                    }
                }
            }
        }
        try (var comando = banco.createStatement()) {
            for (String sql : ddl) {
                comando.execute(sql);
            }
            banco.setAutoCommit(false);
            try {
                for (String sql : dml) {
                    if (!sql.equalsIgnoreCase("COMMIT")) {
                        comando.executeUpdate(sql);
                    }
                }
                banco.commit();
            } catch (SQLException erro) {
                banco.rollback();
                throw erro;
            }
        }
    }

    private static void verificar(Connection banco) throws Exception {
        try (var comando = banco.createStatement()) {
            for (String tabela : TABELAS) {
                try (var linhas = comando.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
                    linhas.next();
                    long total = linhas.getLong(1);
                    System.out.printf("%s -> %d linhas%n", tabela, total);
                    if (total < 5) {
                        throw new IllegalStateException(tabela + " tem menos de cinco linhas.");
                    }
                }
            }
            int numero = 0;
            for (String sql : comandos("03_DQL.SQL")) {
                System.out.printf("Consulta %d%n", ++numero);
                try (var linhas = comando.executeQuery(sql)) {
                    int colunas = linhas.getMetaData().getColumnCount();
                    while (linhas.next()) {
                        for (int i = 1; i <= colunas; i++) {
                            System.out.printf("%s%s", i == 1 ? "" : " | ", linhas.getString(i));
                        }
                        System.out.println();
                    }
                }
            }
        }
    }
}
