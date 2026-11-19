package br.com.temaqui;

import br.com.temaqui.Modelo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TemAquiController {
    private final TemAquiBo bo;

    public TemAquiController(TemAquiBo bo) {
        this.bo = bo;
    }

    @GetMapping("/ofertas")
    public List<Oferta> buscar(@RequestParam @NotBlank @Size(max = 120) String q,
                              @RequestParam @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
                              @RequestParam @DecimalMin("-180") @DecimalMax("180") BigDecimal lon) {
        return bo.buscar(q, lat.doubleValue(), lon.doubleValue());
    }

    @PostMapping("/moradores")
    @ResponseStatus(HttpStatus.CREATED)
    public Morador cadastrar(@Valid @RequestBody CadastroMorador dados) {
        return bo.cadastrar(dados);
    }

    @PostMapping("/lojas")
    @ResponseStatus(HttpStatus.CREATED)
    public Loja cadastrar(@Valid @RequestBody CadastroLoja dados) {
        return bo.cadastrar(dados);
    }

    @GetMapping("/lojas/{lojaId}")
    public Loja loja(@PathVariable @Positive long lojaId) {
        return bo.loja(lojaId);
    }

    @PostMapping("/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    public Produto cadastrar(@Valid @RequestBody CadastroProduto dados) {
        return bo.cadastrar(dados);
    }

    @GetMapping("/lojas/{lojaId}/estoques")
    public List<Estoque> listarEstoque(@PathVariable @Positive long lojaId) {
        return bo.estoqueDaLoja(lojaId);
    }

    @PutMapping("/lojas/{lojaId}/estoques/{produtoId}")
    public Estoque publicar(@PathVariable @Positive long lojaId, @PathVariable @Positive long produtoId,
                            @Valid @RequestBody PublicacaoEstoque dados) {
        if (produtoId != dados.produtoId()) {
            throw new RegraException(HttpStatus.BAD_REQUEST, "Produto do corpo difere da URL.");
        }
        return bo.publicar(lojaId, dados);
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public Reserva reservar(@Valid @RequestBody PedidoReserva dados) {
        return bo.reservar(dados);
    }

    @GetMapping("/reservas/{id}")
    public Reserva acompanhar(@PathVariable @Positive long id) {
        return bo.reserva(id);
    }

    @GetMapping("/lojas/{lojaId}/reservas")
    public List<Reserva> listarReservas(@PathVariable @Positive long lojaId) {
        return bo.reservasDaLoja(lojaId);
    }

    @PutMapping("/lojas/{lojaId}/reservas/{id}/status")
    public Reserva decidir(@PathVariable @Positive long lojaId, @PathVariable @Positive long id,
                          @Valid @RequestBody MudancaReserva dados) {
        return bo.decidir(lojaId, id, dados.status());
    }

    @DeleteMapping("/reservas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable @Positive long id, @RequestParam @Positive long moradorId) {
        bo.cancelar(id, moradorId);
    }
}
