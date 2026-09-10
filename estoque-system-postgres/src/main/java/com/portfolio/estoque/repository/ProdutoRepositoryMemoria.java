package com.portfolio.estoque.repository;

import com.portfolio.estoque.model.Produto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ProdutoRepositoryMemoria implements ProdutoRepository {

    private final List<Produto> produtos = new ArrayList<>();
    private final AtomicLong proximoId = new AtomicLong(1);

    @Override
    public Produto salvar(Produto produto) {
        produto.setId(proximoId.getAndIncrement());
        produtos.add(produto);
        return produto;
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {
        return produtos.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Produto> listarTodos() {
        return new ArrayList<>(produtos);
    }

    @Override
    public Produto atualizar(Produto produtoAtualizado) {
        buscarPorId(produtoAtualizado.getId()).ifPresent(existente -> {
            existente.setNome(produtoAtualizado.getNome());
            existente.setDescricao(produtoAtualizado.getDescricao());
            existente.setPreco(produtoAtualizado.getPreco());
            existente.setQuantidadeEmEstoque(produtoAtualizado.getQuantidadeEmEstoque());
        });
        return produtoAtualizado;
    }

    @Override
    public boolean excluir(Long id) {
        return produtos.removeIf(p -> p.getId().equals(id));
    }
}
