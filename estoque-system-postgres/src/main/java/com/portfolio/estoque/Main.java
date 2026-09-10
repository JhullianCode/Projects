package com.portfolio.estoque;

import com.portfolio.estoque.model.Produto;
import com.portfolio.estoque.repository.ProdutoRepository;
import com.portfolio.estoque.repository.ProdutoRepositoryPostgres;
import com.portfolio.estoque.service.ProdutoService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final ProdutoRepository repository = new ProdutoRepositoryPostgres();
    private static final ProdutoService service = new ProdutoService(repository);
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        int opcao;
        do {
            exibirMenu();
            opcao = lerInteiro("Escolha uma opção: ");

            switch (opcao) {
                case 1 -> cadastrarProduto();
                case 2 -> listarProdutos();
                case 3 -> buscarProdutoPorId();
                case 4 -> atualizarProduto();
                case 5 -> excluirProduto();
                case 0 -> System.out.println("Encerrando... até mais!");
                default -> System.out.println("Opção inválida.");
            }
            System.out.println();
        } while (opcao != 0);

        scanner.close();
    }

    private static void exibirMenu() {
        System.out.println("=== ESTOQUE SYSTEM ===");
        System.out.println("1. Cadastrar produto");
        System.out.println("2. Listar produtos");
        System.out.println("3. Buscar produto por id");
        System.out.println("4. Atualizar produto");
        System.out.println("5. Excluir produto");
        System.out.println("0. Sair");
    }

    private static void cadastrarProduto() {
        try {
            System.out.print("Nome: ");
            String nome = scanner.nextLine();
            System.out.print("Descrição: ");
            String descricao = scanner.nextLine();
            BigDecimal preco = lerBigDecimal("Preço: ");
            int quantidade = lerInteiro("Quantidade em estoque: ");

            Produto produto = service.cadastrar(nome, descricao, preco, quantidade);
            System.out.println("Produto cadastrado com sucesso! " + produto);

        } catch (IllegalArgumentException e) {
            System.out.println("Erro de validação: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Erro ao cadastrar: " + e.getMessage());
        }
    }

    private static void listarProdutos() {
        List<Produto> produtos = service.listarTodos();
        if (produtos.isEmpty()) {
            System.out.println("Nenhum produto cadastrado.");
            return;
        }
        produtos.forEach(System.out::println);
    }

    private static void buscarProdutoPorId() {
        Long id = (long) lerInteiro("Id do produto: ");
        service.buscarPorId(id)
                .ifPresentOrElse(
                        System.out::println,
                        () -> System.out.println("Produto não encontrado.")
                );
    }

    private static void atualizarProduto() {
        try {
            Long id = (long) lerInteiro("Id do produto a atualizar: ");
            System.out.print("Novo nome: ");
            String nome = scanner.nextLine();
            System.out.print("Nova descrição: ");
            String descricao = scanner.nextLine();
            BigDecimal preco = lerBigDecimal("Novo preço: ");
            int quantidade = lerInteiro("Nova quantidade: ");

            Produto atualizado = service.atualizar(id, nome, descricao, preco, quantidade);
            System.out.println("Produto atualizado: " + atualizado);

        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void excluirProduto() {
        Long id = (long) lerInteiro("Id do produto a excluir: ");
        boolean excluido = service.excluir(id);
        System.out.println(excluido ? "Produto excluído." : "Produto não encontrado.");
    }

    private static int lerInteiro(String mensagem) {
        System.out.print(mensagem);
        while (!scanner.hasNextInt()) {
            System.out.println("Digite um número válido.");
            scanner.next();
        }
        int valor = scanner.nextInt();
        scanner.nextLine(); // consome a quebra de linha pendente
        return valor;
    }

    private static BigDecimal lerBigDecimal(String mensagem) {
        System.out.print(mensagem);
        while (!scanner.hasNextBigDecimal()) {
            System.out.println("Digite um valor numérico válido (ex: 19.90).");
            scanner.next();
        }
        BigDecimal valor = scanner.nextBigDecimal();
        scanner.nextLine();
        return valor;
    }
}
