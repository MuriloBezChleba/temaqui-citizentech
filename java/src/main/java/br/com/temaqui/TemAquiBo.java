package br.com.temaqui;

import br.com.temaqui.Modelo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class TemAquiBo {
    private final TemAquiDao dao;

    public TemAquiBo(TemAquiDao dao) {
        this.dao = dao;
    }

    public Morador cadastrar(CadastroMorador dados) {
        return dao.cadastrarMorador(dados);
    }

    @Transactional
    public Loja cadastrar(CadastroLoja dados) {
        return dao.cadastrarLoja(dados);
    }

    public Produto cadastrar(CadastroProduto dados) {
        return dao.cadastrarProduto(dados.nome().strip());
    }

    public Loja loja(long id) {
        return dao.buscarLoja(id);
    }

    public List<Estoque> estoqueDaLoja(long lojaId) {
        loja(lojaId);
        return dao.estoqueDaLoja(lojaId);
    }

    @Transactional
    public Estoque publicar(long lojaId, PublicacaoEstoque dados) {
        dao.bloquearLoja(lojaId);
        dao.verificarProduto(dados.produtoId());
        var estoque = dao.estoqueDoProduto(lojaId, dados.produtoId());
        if (estoque.isEmpty()) {
            return dao.estoque(dao.criarEstoque(lojaId, dados), false);
        }

        long id = estoque.get().id();
        if (dados.quantidade() < dao.unidadesComprometidas(id)) {
            throw RegraException.conflito("Quantidade abaixo das reservas confirmadas.");
        }
        dao.atualizarEstoque(id, dados);
        return dao.estoque(id, false);
    }

    public List<Oferta> buscar(String termo, double lat, double lon) {
        return dao.buscarOfertas(termo.strip(), lat, lon).stream()
                .sorted(Comparator.comparingDouble(Oferta::distanciaKm)
                        .thenComparing(Oferta::preco).thenComparingLong(Oferta::estoqueId))
                .toList();
    }

    @Transactional
    public Reserva reservar(PedidoReserva dados) {
        dao.verificarMorador(dados.moradorId());
        Estoque estoque = dao.estoque(dados.estoqueId(), true);
        verificarDisponibilidade(estoque, dados.quantidade());
        return dao.reservar(dados, estoque);
    }

    public Reserva reserva(long id) {
        return dao.reserva(id, false);
    }

    public List<Reserva> reservasDaLoja(long lojaId) {
        loja(lojaId);
        return dao.reservasDaLoja(lojaId);
    }

    @Transactional
    public Reserva decidir(long lojaId, long id, Status destino) {
        Reserva reserva = dao.reserva(id, true);
        Estoque estoque = dao.estoque(reserva.estoqueId(), true);
        if (estoque.lojaId() != lojaId) {
            throw RegraException.ausente("Reserva da loja");
        }
        if (destino != Status.CONFIRMADA && destino != Status.RECUSADA && destino != Status.RETIRADA) {
            throw RegraException.conflito("A loja pode confirmar, recusar ou registrar a retirada.");
        }
        if (destino == reserva.status()) {
            return reserva;
        }
        boolean permitida = destino == Status.RETIRADA
                ? reserva.status() == Status.CONFIRMADA : reserva.status() == Status.PENDENTE;
        if (!permitida) {
            throw RegraException.conflito("Transicao de reserva invalida.");
        }
        if (destino == Status.CONFIRMADA) {
            verificarDisponibilidade(estoque, reserva.quantidade());
        }
        if (destino == Status.RETIRADA) {
            dao.baixarEstoque(estoque.id(), reserva.quantidade());
        }
        dao.mudarStatus(id, destino);
        return dao.reserva(id, false);
    }

    @Transactional
    public void cancelar(long id, long moradorId) {
        Reserva reserva = dao.reserva(id, true);
        if (reserva.moradorId() != moradorId) {
            throw RegraException.ausente("Reserva do morador");
        }
        if (reserva.status() == Status.CANCELADA) {
            return;
        }
        if (reserva.status() != Status.PENDENTE && reserva.status() != Status.CONFIRMADA) {
            throw RegraException.conflito("Somente reservas pendentes ou confirmadas podem ser canceladas.");
        }
        dao.estoque(reserva.estoqueId(), true);
        dao.mudarStatus(id, Status.CANCELADA);
    }

    private void verificarDisponibilidade(Estoque estoque, int quantidade) {
        if (quantidade > estoque.quantidade() - dao.unidadesComprometidas(estoque.id())) {
            throw RegraException.conflito("Quantidade indisponivel.");
        }
    }
}
