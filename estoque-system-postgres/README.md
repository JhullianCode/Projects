# Estoque System (PostgreSQL)

Sistema de gerenciamento de estoque em Java puro, com foco em boas práticas de arquitetura back-end.

📖 **Leia `TUTORIAL.md`** para o guia de estudo completo, explicando cada decisão técnica — incluindo as diferenças entre MySQL e PostgreSQL do ponto de vista de um desenvolvedor Java/JDBC.

## Stack

- ☕ Java 17+
- 🐘 PostgreSQL
- 📦 Maven
- 🔗 JDBC puro (sem framework/ORM)

## Arquitetura

```
Model       -> Produto (POJO puro)
Repository  -> interface ProdutoRepository + 2 implementações (Memória e Postgres)
Service     -> ProdutoService (regras de negócio, validações)
Main        -> menu interativo no console
```

## Como rodar

1. Tenha um PostgreSQL rodando localmente (ou ajuste as variáveis de ambiente `DB_URL`, `DB_USER`, `DB_PASSWORD`)

2. Crie o banco de dados (o PostgreSQL não permite `CREATE DATABASE IF NOT EXISTS` dentro de um script comum — rode este comando separadamente, conectado ao banco padrão `postgres`):
   ```bash
   psql -U postgres -c "CREATE DATABASE estoque_db;"
   ```

3. Crie a tabela:
   ```bash
   psql -U postgres -d estoque_db -f src/main/resources/schema.sql
   ```

4. Compile e execute:
   ```bash
   mvn clean package
   java -jar target/estoque-system-1.0.0.jar
   ```

## O aprendizado central deste projeto

Trocar a fonte de dados de "lista em memória" para "PostgreSQL via JDBC" exige mudar **uma única linha** de código, em `Main.java`:

```java
private static final ProdutoRepository repository = new ProdutoRepositoryPostgres();
// ou, para testar sem banco de dados:
private static final ProdutoRepository repository = new ProdutoRepositoryMemoria();
```

Isso é possível porque `ProdutoService` depende apenas da interface `ProdutoRepository`, nunca de uma implementação concreta.

## Próximos passos

- API REST com Spring Boot (+ Spring Data JPA, que abstrai o PostgreSQL ainda mais)
- Migrations com Flyway
- Testes unitários (JUnit + Mockito)
