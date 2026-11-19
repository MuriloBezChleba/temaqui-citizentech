package br.com.temaqui;

import br.com.temaqui.Modelo.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class TemAquiDao {
    private final JdbcTemplate jdbc;

    private static final RowMapper<Estoque> ESTOQUE = (r, n) -> new Estoque(
            r.getLong("id"), r.getLong("loja_id"), r.getLong("produto_id"), r.getInt("quantidade"),
            r.getBigDecimal("preco"), r.getObject("atualizado_em", OffsetDateTime.class));

    private static final RowMapper<Reserva> RESERVA = (r, n) -> new Reserva(
            r.getLong("id"), r.getLong("morador_id"), r.getLong("estoque_id"), r.getInt("quantidade"),
            r.getBigDecimal("preco_unitario"), Status.valueOf(r.getString("status")),
            r.getObject("criada_em", OffsetDateTime.class));

    public TemAquiDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Loja loja(ResultSet r, int linha) throws SQLException {
        return new Loja(r.getLong("id"), r.getString("nome"), r.getString("endereco"),
                r.getString("horario"), r.getString("categoria"), r.getBigDecimal("latitude"),
                r.getBigDecimal("longitude"));
    }

    private <T> T unico(String sql, RowMapper<T> mapper, String recurso, Object... parametros) {
        return jdbc.query(sql, mapper, parametros).stream().findFirst()
                .orElseThrow(() -> RegraException.ausente(recurso));
    }

    private long inserir(String sql, Object... valores) {
        var chave = new GeneratedKeyHolder();
        jdbc.update(conexao -> {
            PreparedStatement statement = conexao.prepareStatement(sql, new String[]{"ID"});
            for (int i = 0; i < valores.length; i++) {
                statement.setObject(i + 1, valores[i]);
            }
            return statement;
        }, chave);
        return Objects.requireNonNull(chave.getKey()).longValue();
    }

    public Morador cadastrarMorador(CadastroMorador dados) {
        long id = inserir("INSERT INTO TA_MORADOR (nome,celular,bairro) VALUES (?,?,?)",
                dados.nome().strip(), dados.celular(), dados.bairro().strip());
        return new Morador(id, dados.nome().strip(), dados.celular(), dados.bairro().strip());
    }

    public Loja cadastrarLoja(CadastroLoja dados) {
        long id = inserir("""
                INSERT INTO TA_LOJA (nome,endereco,horario,categoria,latitude,longitude)
                VALUES (?,?,?,?,?,?)
                """, dados.nome().strip(), dados.endereco().strip(), dados.horario().strip(),
                dados.categoria().strip(), dados.latitude(), dados.longitude());
        return buscarLoja(id);
    }

    public Loja buscarLoja(long id) {
        return unico("SELECT * FROM TA_LOJA WHERE id = ?", TemAquiDao::loja, "Loja", id);
    }

    public Produto cadastrarProduto(String nome) {
        long id = inserir("INSERT INTO TA_PRODUTO (nome) VALUES (?)", nome);
        return new Produto(id, nome);
    }

    public void bloquearLoja(long id) {
        unico("SELECT id FROM TA_LOJA WHERE id = ? FOR UPDATE", (r, n) -> r.getLong(1), "Loja", id);
    }

    public void verificarProduto(long id) {
        unico("SELECT id FROM TA_PRODUTO WHERE id = ?", (r, n) -> r.getLong(1), "Produto", id);
    }

    public Estoque estoque(long id, boolean bloquear) {
        return unico("SELECT * FROM TA_ESTOQUE WHERE id = ?" + (bloquear ? " FOR UPDATE" : ""),
                ESTOQUE, "Estoque", id);
    }

    public List<Estoque> estoqueDaLoja(long lojaId) {
        return jdbc.query("SELECT * FROM TA_ESTOQUE WHERE loja_id = ? ORDER BY id", ESTOQUE, lojaId);
    }

    public Optional<Estoque> estoqueDoProduto(long lojaId, long produtoId) {
        return jdbc.query("SELECT * FROM TA_ESTOQUE WHERE loja_id = ? AND produto_id = ? FOR UPDATE",
                ESTOQUE, lojaId, produtoId).stream().findFirst();
    }

    public long criarEstoque(long lojaId, PublicacaoEstoque dados) {
        return inserir("INSERT INTO TA_ESTOQUE (loja_id,produto_id,quantidade,preco) VALUES (?,?,?,?)",
                lojaId, dados.produtoId(), dados.quantidade(), dados.preco());
    }

    public void atualizarEstoque(long id, PublicacaoEstoque dados) {
        jdbc.update("UPDATE TA_ESTOQUE SET quantidade=?, preco=?, atualizado_em=SYSTIMESTAMP WHERE id=?",
                dados.quantidade(), dados.preco(), id);
    }

    public void baixarEstoque(long id, int quantidade) {
        jdbc.update("UPDATE TA_ESTOQUE SET quantidade=quantidade-?, atualizado_em=SYSTIMESTAMP WHERE id=?",
                quantidade, id);
    }

    public long unidadesComprometidas(long estoqueId) {
        return Objects.requireNonNull(jdbc.queryForObject("""
                SELECT COALESCE(SUM(quantidade),0) FROM TA_RESERVA
                WHERE estoque_id=? AND status='CONFIRMADA'
                """, Long.class, estoqueId));
    }

    public List<Oferta> buscarOfertas(String termo, double latitude, double longitude) {
        OffsetDateTime agora = OffsetDateTime.now();
        return jdbc.query("""
                SELECT l.*, p.id AS produto_id, p.nome AS produto_nome, e.id AS estoque_id,
                       e.preco, e.atualizado_em, e.quantidade - COALESCE(r.comprometida,0) AS disponivel
                FROM TA_ESTOQUE e
                JOIN TA_LOJA l ON l.id=e.loja_id
                JOIN TA_PRODUTO p ON p.id=e.produto_id
                LEFT JOIN (
                    SELECT estoque_id, SUM(quantidade) AS comprometida
                    FROM TA_RESERVA WHERE status='CONFIRMADA' GROUP BY estoque_id
                ) r ON r.estoque_id=e.id
                WHERE INSTR(LOWER(p.nome), LOWER(?)) > 0
                  AND e.quantidade > COALESCE(r.comprometida,0)
                """, (r, n) -> {
            Loja loja = loja(r, n);
            OffsetDateTime atualizacao = r.getObject("atualizado_em", OffsetDateTime.class);
            return new Oferta(loja, new Produto(r.getLong("produto_id"), r.getString("produto_nome")),
                    r.getLong("estoque_id"), r.getInt("disponivel"), r.getBigDecimal("preco"), atualizacao,
                    Math.max(0, Duration.between(atualizacao, agora).toMinutes()),
                    distanciaKm(latitude, longitude, loja.latitude().doubleValue(), loja.longitude().doubleValue()));
        }, termo);
    }

    static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 6371.0088 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, a))));
    }

    public void verificarMorador(long id) {
        unico("SELECT id FROM TA_MORADOR WHERE id=?", (r, n) -> r.getLong(1), "Morador", id);
    }

    public Reserva reservar(PedidoReserva dados, Estoque estoque) {
        long id = inserir("""
                INSERT INTO TA_RESERVA (morador_id, estoque_id, quantidade, preco_unitario)
                VALUES (?,?,?,?)
                """, dados.moradorId(), estoque.id(), dados.quantidade(), estoque.preco());
        return reserva(id, false);
    }

    public Reserva reserva(long id, boolean bloquear) {
        return unico("SELECT * FROM TA_RESERVA WHERE id=?" + (bloquear ? " FOR UPDATE" : ""),
                RESERVA, "Reserva", id);
    }

    public List<Reserva> reservasDaLoja(long lojaId) {
        return jdbc.query("""
                SELECT r.* FROM TA_RESERVA r JOIN TA_ESTOQUE e ON e.id=r.estoque_id
                WHERE e.loja_id=? ORDER BY r.criada_em DESC, r.id DESC
                """, RESERVA, lojaId);
    }

    public void mudarStatus(long id, Status status) {
        jdbc.update("UPDATE TA_RESERVA SET status=? WHERE id=?", status.name(), id);
    }
}
