-- Dados ficticios para demonstracao academica.
INSERT INTO TA_MORADOR (id,nome,celular,bairro) VALUES (1,'Morador Demo A','11000000001','Liberdade');
INSERT INTO TA_MORADOR (id,nome,celular,bairro) VALUES (2,'Morador Demo B','11000000002','Liberdade');
INSERT INTO TA_MORADOR (id,nome,celular,bairro) VALUES (3,'Morador Demo C','11000000003','Liberdade');
INSERT INTO TA_MORADOR (id,nome,celular,bairro) VALUES (4,'Morador Demo D','11000000004','Liberdade');
INSERT INTO TA_MORADOR (id,nome,celular,bairro) VALUES (5,'Morador Demo E','11000000005','Liberdade');

INSERT INTO TA_LOJA (id,nome,endereco,horario,categoria,latitude,longitude) VALUES (1,'Ferragens Demo A','Rua Galvao Bueno, ponto A','Seg-Sab 09:00-18:00','Ferragens',-23.55840,-46.63530);
INSERT INTO TA_LOJA (id,nome,endereco,horario,categoria,latitude,longitude) VALUES (2,'Ferragens Demo B','Rua dos Estudantes, ponto B','Seg-Sab 08:00-17:00','Ferragens',-23.55640,-46.63380);
INSERT INTO TA_LOJA (id,nome,endereco,horario,categoria,latitude,longitude) VALUES (3,'Ferragens Demo C','Rua da Gloria, ponto C','Seg-Sex 09:00-18:00','Ferragens',-23.55680,-46.63240);
INSERT INTO TA_LOJA (id,nome,endereco,horario,categoria,latitude,longitude) VALUES (4,'Ferragens Demo D','Rua Sao Joaquim, ponto D','Seg-Sab 08:00-18:00','Ferragens',-23.56210,-46.63810);
INSERT INTO TA_LOJA (id,nome,endereco,horario,categoria,latitude,longitude) VALUES (5,'Ferragens Demo E','Rua Vergueiro, ponto E','Seg-Sex 09:00-17:00','Ferragens',-23.56570,-46.63800);

INSERT INTO TA_PRODUTO (id,nome) VALUES (1,'Parafuso M8 40 mm');
INSERT INTO TA_PRODUTO (id,nome) VALUES (2,'Bucha 8 mm');
INSERT INTO TA_PRODUTO (id,nome) VALUES (3,'Arruela M8');
INSERT INTO TA_PRODUTO (id,nome) VALUES (4,'Chave Allen 5 mm');
INSERT INTO TA_PRODUTO (id,nome) VALUES (5,'Fita veda rosca');

INSERT INTO TA_ESTOQUE (id,loja_id,produto_id,quantidade,preco,atualizado_em) VALUES (1,1,1,20,2.50,SYSTIMESTAMP - INTERVAL '2' HOUR);
INSERT INTO TA_ESTOQUE (id,loja_id,produto_id,quantidade,preco,atualizado_em) VALUES (2,2,1,15,2.30,SYSTIMESTAMP - INTERVAL '6' DAY);
INSERT INTO TA_ESTOQUE (id,loja_id,produto_id,quantidade,preco) VALUES (3,3,2,30,0.70);
INSERT INTO TA_ESTOQUE (id,loja_id,produto_id,quantidade,preco) VALUES (4,4,3,10,1.20);
INSERT INTO TA_ESTOQUE (id,loja_id,produto_id,quantidade,preco) VALUES (5,1,4,5,9.90);
INSERT INTO TA_ESTOQUE (id,loja_id,produto_id,quantidade,preco) VALUES (6,2,5,8,4.50);

INSERT INTO TA_RESERVA (id,morador_id,estoque_id,quantidade,preco_unitario,status) VALUES (1,1,1,2,2.50,'PENDENTE');
INSERT INTO TA_RESERVA (id,morador_id,estoque_id,quantidade,preco_unitario,status) VALUES (2,2,1,3,2.50,'CONFIRMADA');
INSERT INTO TA_RESERVA (id,morador_id,estoque_id,quantidade,preco_unitario,status) VALUES (3,3,2,1,2.30,'RECUSADA');
INSERT INTO TA_RESERVA (id,morador_id,estoque_id,quantidade,preco_unitario,status) VALUES (4,4,3,2,0.70,'RETIRADA');
INSERT INTO TA_RESERVA (id,morador_id,estoque_id,quantidade,preco_unitario,status) VALUES (5,5,3,1,0.70,'CANCELADA');
COMMIT;
