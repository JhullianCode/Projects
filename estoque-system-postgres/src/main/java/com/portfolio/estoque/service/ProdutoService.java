package com.portfolio.estoque.service;

import com.portfolio.estoque.model.Produto;
import com.portfolio.estoque.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public Produto cadastrar(String nome, String descricao, BigDecimal preco, Integer quantidade) {
        validarDadosBasicos(nome, preco, quantidade);
        Produto produto = new Produto(nome, descricao, preco, quantidade);
        return produtoRepository.salvar(produto);
    }

    public List<Produto> listarTodos() {
        return produtoRepository.listarTodos();
    }

    public Optional<Produto> buscarPorId(Long id) {
        return produtoRepository.buscarPorId(id);
    }

    public Produto atualizar(Long id, String nome, String descricao, BigDecimal preco, Integer quantidade) {
        Produto existente = produtoRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto com id " + id + " não encontrado"));

        validarDadosBasicos(nome, preco, quantidade);

        existente.setNome(nome);
        existente.setDescricao(descricao);
        existente.setPreco(preco);
        existente.setQuantidadeEmEstoque(quantidade);

        return produtoRepository.atualizar(existente);
    }

    public boolean excluir(Long id) {
        return produtoRepository.excluir(id);
    }

    private void validarDadosBasicos(String nome, BigDecimal preco, Integer quantidade) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do produto não pode ser vazio");
        }
        if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço não pode ser negativo");
        }
        if (quantidade == null || quantidade < 0) {
            throw new IllegalArgumentException("Quantidade em estoque não pode ser negativa");
        }
    }
}
