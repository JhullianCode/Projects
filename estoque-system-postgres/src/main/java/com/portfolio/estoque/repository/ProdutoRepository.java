package com.portfolio.estoque.repository;

import com.portfolio.estoque.model.Produto;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository {

    Produto salvar(Produto produto);

    Optional<Produto> buscarPorId(Long id);

    List<Produto> listarTodos();

    Produto atualizar(Produto produto);

    boolean excluir(Long id);
}
