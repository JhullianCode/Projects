# Tutorial de Estudo — Estoque System (Java + JDBC + PostgreSQL)

Projeto: sistema de estoque em **Java puro**, sem frameworks, com arquitetura em camadas. Este documento explica o "porquê" de cada decisão técnica, para você estudar e conseguir defender o projeto em entrevista.

---

## 1. Por que Java puro (sem Spring)?

O ponto deste projeto é demonstrar que você entende os **fundamentos** antes de depender de um framework para resolver tudo por você. Quando você disser numa entrevista "sei o que o Spring Data JPA faz por trás dos panos", este projeto é a prova disso: você escreveu manualmente o que o Hibernate/JPA automatiza.

---

## 2. Arquitetura em Camadas

```
┌─────────────┐
│    Main     │  ← interface com o usuário (aqui, um menu de console)
└──────┬──────┘
       │ usa
┌──────▼──────┐
│   Service   │  ← regras de negócio, validações
└──────┬──────┘
       │ depende de (interface)
┌──────▼──────┐
│  Repository │  ← acesso a dados (JDBC ou memória)
└──────┬──────┘
       │
┌──────▼──────┐
│    Model    │  ← representa os dados (Produto)
└─────────────┘
```

**Regra de ouro:** cada camada só conhece a camada logo abaixo dela, e sempre através de uma interface quando possível. O `Main` não sabe como o `Service` valida os dados; o `Service` não sabe se os dados estão num banco PostgreSQL ou numa lista em memória.

---

## 3. O Model (`Produto`)

Um **POJO** (Plain Old Java Object) — uma classe Java comum, sem anotações de framework, sem saber que existe banco de dados.

**Decisão técnica importante:** uso de `BigDecimal` para o campo `preco`, nunca `double` ou `float`. Números de ponto flutuante binário não representam a maioria dos valores decimais com exatidão — o clássico exemplo é `0.1 + 0.2 != 0.3` em `double`. Para dinheiro, esse erro de arredondamento é inaceitável. `BigDecimal` representa o valor exatamente. O espelho disso no banco é a coluna `DECIMAL(10, 2)` — nunca `FLOAT`/`DOUBLE` no SQL.

---

## 4. A Interface `ProdutoRepository` — o coração do projeto

```java
public interface ProdutoRepository {
    Produto salvar(Produto produto);
    Optional<Produto> buscarPorId(Long id);
    List<Produto> listarTodos();
    Produto atualizar(Produto produto);
    boolean excluir(Long id);
}
```

Essa interface define **o que** um repositório de produtos deve saber fazer — não **como**. É esse contrato que permite duas implementações completamente diferentes (`ProdutoRepositoryMemoria` e `ProdutoRepositoryPostgres`) coexistirem sem que o resto do sistema perceba diferença.

**Por que `Optional<Produto>` em vez de retornar `null`?**
Retornar `null` obriga quem chama a lembrar de checar `if (produto != null)` — fácil de esquecer, gerando `NullPointerException`. `Optional` torna a ausência de valor explícita no tipo de retorno, e o compilador força você a lidar com esse caso (via `.orElseThrow()`, `.ifPresentOrElse()`, etc).

Este é o princípio de design conhecido como **"programe para interfaces, não para implementações"** (também aparece como parte do princípio de Inversão de Dependência, o "D" do SOLID).

---

## 5. `ProdutoRepositoryMemoria` — implementação #1

Guarda os produtos numa `List` Java comum. Serve para testar a lógica de negócio sem precisar de um banco de dados rodando.

**Detalhes técnicos:**
- **`AtomicLong` para gerar IDs**: `++` não é uma operação atômica (são 3 passos: ler, incrementar, gravar). Em cenário concorrente (múltiplas threads chamando `salvar()` ao mesmo tempo), isso poderia gerar IDs duplicados. `AtomicLong.getAndIncrement()` evita essa condição de corrida.
- **Cópia defensiva em `listarTodos()`**: retorna `new ArrayList<>(produtos)` em vez da lista original, para que quem recebe o retorno não consiga alterar o estado interno do repositório diretamente (sem passar pelos métodos oficiais).

---

## 6. `ProdutoRepositoryPostgres` — implementação #2 (a de verdade)

Aqui é onde o JDBC "puro" acontece — sem nenhum ORM fazendo o trabalho por você.

### `PreparedStatement` e SQL Injection

```java
String sql = "INSERT INTO produtos (nome, ...) VALUES (?, ?, ?, ?)";
stmt.setString(1, produto.getNome());
```

Os `?` são *placeholders*; os valores são enviados **separadamente** do comando SQL, nunca concatenados como texto. Isso é o que previne **SQL Injection** — se você escrevesse `"...WHERE nome = '" + nome + "'"`, um valor malicioso de `nome` poderia alterar o significado inteiro da consulta.

### `try-with-resources`

```java
try (Connection conn = ConnectionFactory.criarConexao();
     PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
    ...
}
```

`Connection`, `PreparedStatement` e `ResultSet` implementam `AutoCloseable`. Declarados dentro do `try(...)`, o Java fecha esses recursos automaticamente ao sair do bloco — mesmo se uma exceção acontecer no meio. **Esquecer de fechar conexões é uma das causas mais comuns de aplicações Java "travarem" em produção** depois de rodar um tempo (o pool de conexões se esgota).

### O "mapper" manual

```java
private Produto mapearLinhaParaProduto(ResultSet rs) throws SQLException {
    return new Produto(rs.getLong("id"), rs.getString("nome"), ...);
}
```

Esse método converte uma linha "crua" do banco (`ResultSet`) num objeto Java. É exatamente o trabalho que o **Hibernate/JPA faz automaticamente** nos bastidores em projetos com Spring Data (se você tiver feito o projeto Kanban antes deste, essa comparação direta é ótima munição de entrevista).

### `RETURN_GENERATED_KEYS`

Como o campo `id` é `BIGSERIAL` no PostgreSQL (o equivalente a um `AUTO_INCREMENT`, mas implementado por trás dos panos com uma **sequence** — um contador independente no banco), o valor é gerado *pelo banco*, não pelo Java. `Statement.RETURN_GENERATED_KEYS` + `stmt.getGeneratedKeys()` é como perguntamos ao driver JDBC "qual id você acabou de gerar?", para devolver o objeto `Produto` já completo. O driver do PostgreSQL suporta esse mecanismo normalmente — o código Java é idêntico ao que seria escrito para MySQL.

---

## 7. `ProdutoService` — regras de negócio

```java
public ProdutoService(ProdutoRepository produtoRepository) {
    this.produtoRepository = produtoRepository;
}
```

**Injeção de dependência manual** (sem framework): o `Service` recebe a interface já pronta pelo construtor, em vez de criar a implementação ele mesmo com `new`. É literalmente o mesmo princípio que o Spring automatiza com `@Autowired` — só que aqui, você monta a "mão" o que o Spring faria por injeção automática.

**Onde mora a validação de negócio?**
No `Service`, nunca no `Repository`. O `Repository` só sabe ler/escrever dados; se a validação estivesse lá, cada nova implementação (`Memoria`, `JDBC`, um futuro `MongoDB`) teria que repetir a mesma lógica. Centralizada no `Service`, a regra existe uma única vez.

---

## 8. A Linha Mais Importante do Projeto

Em `Main.java`:

```java
private static final ProdutoRepository repository = new ProdutoRepositoryPostgres();
```

A variável é declarada com o **tipo da interface**, não da classe concreta. Trocar para:

```java
private static final ProdutoRepository repository = new ProdutoRepositoryMemoria();
```

...é a única mudança necessária no projeto inteiro para trocar completamente a fonte de dados. Nenhuma linha de `ProdutoService`, `Main` (fora essa) ou qualquer outra classe precisa mudar. **Este é o aprendizado central do projeto**, e também o que costuma mais impressionar em entrevista: você não está só dizendo que sabe o que é uma interface — está mostrando um caso de uso real e prático dela.

---

## 9. Perguntas de Entrevista que Este Projeto Responde

- **"O que é injeção de dependência, na prática?"** → Mostre o construtor de `ProdutoService` recebendo `ProdutoRepository` em vez de instanciá-lo com `new`.
- **"Por que usar interface em vez de só uma classe?"** → A troca de `ProdutoRepositoryMemoria` por `ProdutoRepositoryPostgres` sem alterar mais nada.
- **"Como você previne SQL Injection?"** → `PreparedStatement` com parâmetros via `?`, nunca concatenação de string.
- **"Por que `BigDecimal` e não `double` para dinheiro?"** → Imprecisão de ponto flutuante binário.
- **"O que acontece se você esquecer de fechar uma `Connection`?"** → Vazamento de conexão, esgotamento do pool, aplicação trava em produção. `try-with-resources` resolve isso automaticamente.
- **"Qual a diferença entre checked e unchecked exception?"** → `SQLException` é checked; o projeto a envelopa em `RuntimeException` na camada de repositório para não poluir a assinatura de métodos das camadas acima.
- **"Onde fica a validação de negócio, e por quê?"** → No `Service`, nunca no `Repository` ou no `Main`.
- **"O que o Hibernate faz que você não precisa fazer manualmente?"** → Aponte o método `mapearLinhaParaProduto` — é exatamente esse mapeamento objeto-relacional que um ORM automatiza.

---

## 10. MySQL x PostgreSQL: o que muda do ponto de vista Java/JDBC

Se você comparar este projeto com a versão em MySQL, a lição mais importante é: **quase nada muda no código Java**. Isso acontece porque o JDBC é uma especificação padrão do próprio Java — `Connection`, `PreparedStatement`, `ResultSet` são interfaces; cada driver (MySQL, PostgreSQL, Oracle...) fornece sua implementação por trás. Seu código só depende das interfaces.

| Aspecto | MySQL | PostgreSQL |
|---|---|---|
| Dependência Maven | `com.mysql:mysql-connector-j` | `org.postgresql:postgresql` |
| Prefixo da URL JDBC | `jdbc:mysql://` | `jdbc:postgresql://` |
| Porta padrão | 3306 | 5432 |
| Usuário superadmin padrão | `root` | `postgres` |
| Chave autoincremento | `BIGINT AUTO_INCREMENT` | `BIGSERIAL` (usa uma *sequence* por trás) |
| Criar banco dentro de script | `CREATE DATABASE IF NOT EXISTS` funciona | Não suporta `IF NOT EXISTS`; precisa ser um comando separado, rodado fora de uma transação |
| Código Java (`PreparedStatement`, `ResultSet`, etc.) | Idêntico | Idêntico |

**Por que isso importa para uma entrevista:** mostra que você entende a diferença entre a **linguagem/API** (JDBC, que é portável) e o **banco de dados específico** (que tem suas particularidades de SQL e configuração). Muita gente júnior mistura os dois conceitos e acha que "aprendeu MySQL" quando na verdade aprendeu JDBC + um pouco de sintaxe SQL do MySQL.

---

## 11. Como Isso se Conecta com o Próximo Passo (Spring Boot)

Quando você migrar este projeto (ou construir um novo) com Spring Boot:
- `ProdutoRepository` vira uma interface que estende `JpaRepository` — o Spring gera a implementação sozinho.
- O `mapearLinhaParaProduto` manual desaparece — vira anotações `@Entity` na classe `Produto`.
- A injeção de dependência manual no construtor continua **exatamente igual** — é o mesmo padrão, só que o Spring cria e "injeta" os objetos automaticamente via `@Autowired`/injeção por construtor com `@Service`/`@Repository`.

Ou seja: este projeto não é "código descartável" antes do Spring — é a base conceitual que faz você entender profundamente o que o Spring Boot vai automatizar depois.
