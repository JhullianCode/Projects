package com.portfolio.estoque.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class ConnectionFactory {

    private static final String URL = System.getenv().getOrDefault(
            "DB_URL", "jdbc:postgresql://localhost:5433/estoque_db"
    );
    private static final String USUARIO = System.getenv().getOrDefault("DB_USER", "postgres");
    private static final String SENHA = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

    public static Connection criarConexao() {
        try {
            return DriverManager.getConnection(URL, USUARIO, SENHA);
        } catch (SQLException e) {
            // Encapsulamos a exceção checada (SQLException) numa RuntimeException.
            // Isso evita que o "throws SQLException" precise se propagar por
            // toda a cadeia de chamadas do sistema (service, controller, etc).
            throw new RuntimeException("Erro ao conectar ao banco de dados: " + e.getMessage(), e);
        }
    }
}
