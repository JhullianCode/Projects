package com.portfolio.estoque.repository;

import com.portfolio.estoque.config.ConnectionFactory;
import com.portfolio.estoque.model.Produto;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ProdutoRepositoryPostgres implements ProdutoRepository {

    private static final String SQL_INSERT =
            "INSERT INTO produtos (nome, descricao, preco, quantidade_em_estoque) VALUES (?, ?, ?, ?)";
    private static final String SQL_BUSCAR_POR_ID =
            "SELECT * FROM produtos WHERE id = ?";
    private static final String SQL_LISTAR_TODOS =
            "SELECT * FROM produtos ORDER BY id";
    private static final String SQL_ATUALIZAR =
            "UPDATE produtos SET nome = ?, descricao = ?, preco = ?, quantidade_em_estoque = ? WHERE id = ?";
    private static final String SQL_EXCLUIR =
            "DELETE FROM produtos WHERE id = ?";

    @Override
    public Produto salvar(Produto produto) {

        try (Connection conn = ConnectionFactory.criarConexao();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setBigDecimal(3, produto.getPreco());
            stmt.setInt(4, produto.getQuantidadeEmEstoque());

            stmt.executeUpdate();

            try (ResultSet chavesGeradas = stmt.getGeneratedKeys()) {
                if (chavesGeradas.next()) {
                    produto.setId(chavesGeradas.getLong(1));
                }
            }

            return produto;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar produto: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {
        try (Connection conn = ConnectionFactory.criarConexao();
             PreparedStatement stmt = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearLinhaParaProduto(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar produto por id: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Produto> listarTodos() {
        List<Produto> produtos = new ArrayList<>();

        try (Connection conn = ConnectionFactory.criarConexao();
             PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_TODOS);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapearLinhaParaProduto(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar produtos: " + e.getMessage(), e);
        }

        return produtos;
    }

    @Override
    public Produto atualizar(Produto produto) {
        try (Connection conn = ConnectionFactory.criarConexao();
             PreparedStatement stmt = conn.prepareStatement(SQL_ATUALIZAR)) {

            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setBigDecimal(3, produto.getPreco());
            stmt.setInt(4, produto.getQuantidadeEmEstoque());
            stmt.setLong(5, produto.getId());

            stmt.executeUpdate();
            return produto;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar produto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean excluir(Long id) {
        try (Connection conn = ConnectionFactory.criarConexao();
             PreparedStatement stmt = conn.prepareStatement(SQL_EXCLUIR)) {

            stmt.setLong(1, id);
            int linhasAfetadas = stmt.executeUpdate();
            return linhasAfetadas > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir produto: " + e.getMessage(), e);
        }
    }

    private Produto mapearLinhaParaProduto(ResultSet rs) throws SQLException {
        return new Produto(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("descricao"),
                rs.getBigDecimal("preco"),
                rs.getInt("quantidade_em_estoque")
        );
    }
}
