-- No PostgreSQL, ao contrário do MySQL, você não cria o banco de dados a
-- partir de dentro de outro banco com "CREATE DATABASE IF NOT EXISTS".
-- É preciso rodar esse comando conectado a um banco existente (geralmente
-- o banco padrão "postgres"), e o PostgreSQL não suporta "IF NOT EXISTS"
-- para CREATE DATABASE. Por isso, esse passo fica documentado no README
-- como um comando separado - veja as instruções de setup.

CREATE TABLE IF NOT EXISTS produtos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(500),
    preco DECIMAL(10, 2) NOT NULL,
    quantidade_em_estoque INT NOT NULL DEFAULT 0
);
