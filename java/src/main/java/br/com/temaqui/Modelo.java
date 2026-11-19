package br.com.temaqui;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class Modelo {
    private Modelo() {}

    public record Morador(long id, String nome, String celular, String bairro) {}

    public record Loja(long id, String nome, String endereco, String horario, String categoria,
                       BigDecimal latitude, BigDecimal longitude) {}

    public record Produto(long id, String nome) {}

    public record Estoque(long id, long lojaId, long produtoId, int quantidade, BigDecimal preco,
                          OffsetDateTime atualizadoEm) {}

    public enum Status { PENDENTE, CONFIRMADA, RECUSADA, CANCELADA, RETIRADA }

    public record Reserva(long id, long moradorId, long estoqueId, int quantidade,
                          BigDecimal precoUnitario, Status status, OffsetDateTime criadaEm) {
        public BigDecimal getTotal() {
            return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
    }

    public record Oferta(Loja loja, Produto produto, long estoqueId, int disponivel, BigDecimal preco,
                         OffsetDateTime atualizadoEm, long idadeMinutos, double distanciaKm) {}

    public record CadastroMorador(@NotBlank @Size(max = 120) String nome,
                                  @NotBlank @Pattern(regexp = "[0-9]{10,15}") String celular,
                                  @NotBlank @Size(max = 80) String bairro) {}

    public record CadastroLoja(@NotBlank @Size(max = 120) String nome,
                               @NotBlank @Size(max = 180) String endereco,
                               @NotBlank @Size(max = 100) String horario,
                               @NotBlank @Size(max = 80) String categoria,
                               @NotNull @DecimalMin("-90") @DecimalMax("90")
                               @Digits(integer = 3, fraction = 6) BigDecimal latitude,
                               @NotNull @DecimalMin("-180") @DecimalMax("180")
                               @Digits(integer = 3, fraction = 6) BigDecimal longitude) {}

    public record CadastroProduto(@NotBlank @Size(max = 120) String nome) {}

    public record PublicacaoEstoque(@NotNull @Positive Long produtoId,
                                    @NotNull @Min(0) @Max(999999999) Integer quantidade,
                                    @NotNull @DecimalMin("0.01")
                                    @Digits(integer = 8, fraction = 2) BigDecimal preco) {}

    public record PedidoReserva(@NotNull @Positive Long moradorId, @NotNull @Positive Long estoqueId,
                                @NotNull @Min(1) @Max(999999999) Integer quantidade) {}

    public record MudancaReserva(@NotNull Status status) {}
}
