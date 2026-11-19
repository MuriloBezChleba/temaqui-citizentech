-- 1. Onde ha estoque fisico de parafuso M8? Participam produto e estoque.
SELECT p.nome AS produto, e.loja_id, e.quantidade, e.preco, e.atualizado_em
FROM TA_PRODUTO p
INNER JOIN TA_ESTOQUE e ON e.produto_id = p.id
WHERE p.nome = 'Parafuso M8 40 mm'
  AND e.quantidade > 0;

-- 2. Quantas unidades de cada item foram retiradas?
SELECT e.produto_id, COUNT(*) AS retiradas, SUM(r.quantidade) AS unidades
FROM TA_ESTOQUE e
INNER JOIN TA_RESERVA r ON r.estoque_id = e.id
WHERE r.status = 'RETIRADA'
GROUP BY e.produto_id;

-- 3. Quais ferragens ainda nao publicaram nenhum item?
SELECT l.id, l.nome, l.endereco
FROM TA_LOJA l
LEFT OUTER JOIN TA_ESTOQUE e ON e.loja_id = l.id
WHERE l.categoria = 'Ferragens'
  AND e.id IS NULL
ORDER BY l.nome, l.id;
