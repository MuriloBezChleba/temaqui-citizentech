# TemAqui · Ficha da CitizenTech

| Campo | TemAqui |
| --- | --- |
| Cidade e bairro | São Paulo, Liberdade |
| Cidadão beneficiado | Moradores em busca de produtos para o mesmo dia e pequenos lojistas do bairro |
| Problema | O estoque do comércio de rua é pouco visível online, então o morador anda de loja em loja para encontrar um item. |
| Como funciona | A loja publica seu estoque. O morador busca pelo nome do produto, compara disponibilidade, preço e distância, faz a reserva e retira no balcão. |
| Partes | Spring Boot fornece a API REST de catálogo, estoque e reservas. Oracle armazena catálogo, lojas, moradores, estoque e reservas. |
| Ecossistema | Moradores, ferragens locais e associação comercial do bairro |
| Impacto | Contar retiradas por loja ao mês e medir a conversão de buscas em retiradas quando o front-end registrar eventos de busca. Expandir para bairros vizinhos depois de validar o piloto. |
| ODS | ODS 9, inclusão digital de pequenos lojistas. ODS 10, visibilidade para negócios com menos recursos de divulgação. ODS 11, acesso a comércio próximo e incentivo a deslocamentos a pé. |
| Repositório | https://github.com/MuriloBezChleba/temaqui-citizentech |

## Entregas

- `docs/TemAqui-Java.pdf` reúne especificação, UML, DER, telas, endpoints e execução.
- `docs/TemAqui-Database.pdf` reúne a apresentação e o diagrama relacional exportado pelo Oracle SQL Developer Data Modeler.
- `database/` contém os três scripts editáveis para o Oracle.
- `java/` contém o código-fonte Spring Boot. O Blueprint de deploy fica em `render.yaml`.
- Os exemplos usam dados fictícios produzidos para esta GS.

## Executar com Oracle FIAP

1. Use Oracle SQL Developer, SQLcl ou SQL*Plus. Entre no seu esquema acadêmico e execute `01_DDL.SQL` e `02_DML.SQL` nessa ordem. O script `03_DQL.SQL` contém as consultas da entrega. Execute o DDL uma vez por esquema.
2. Instale JDK 17 e Maven 3.9 ou abra `java/pom.xml` no IntelliJ. Configure `ORACLE_URL`, `ORACLE_USER`, `ORACLE_PASSWORD` e `DEMO_KEY` no ambiente do processo. Use a URL JDBC informada pela FIAP. Os exemplos recebem a chave pelo cabeçalho `X-Demo-Key`.
3. A partir da raiz, rode `mvn -f java/pom.xml clean package` e `java -jar java/target/temaqui-1.0.0.jar`.
4. Consulte `http://localhost:8080/actuator/health` e rode `./java/verificar-api.ps1` com a mesma variável `DEMO_KEY`. O roteiro usa a carga fictícia e restaura as quantidades comprometidas com o cancelamento da reserva criada no teste.
5. No Render, configure o Blueprint do repositório com as mesmas variáveis e teste a conexão através de `/actuator/health`. Confirme a busca e as operações autenticadas pelo roteiro com `-BaseUrl` apontando para o serviço.

A chave compartilhada atende à demonstração acadêmica. Use dados fictícios. A distância é em linha reta. O protótipo Java cobre cadastro básico, publicação manual, busca, reserva e retirada. As telas de WhatsApp, OCR, planilha e relatório descrevem funcionalidades do produto no backlog Agile.

Também é possível preparar o esquema pelo terminal com Java 17. Depois do build Maven, a partir da raiz no PowerShell, use o driver resolvido pelo projeto:

```powershell
$jdbc = "$env:USERPROFILE/.m2/repository/com/oracle/database/jdbc/ojdbc11/23.7.0.25.01/ojdbc11-23.7.0.25.01.jar"
java --class-path "$jdbc" java/PrepararOracle.java instalar
```

Use o modo `verificar` se as tabelas já existirem. O utilitário conta as linhas e executa as três consultas da entrega. A instalação exige um esquema sem as tabelas do TemAqui e usa as variáveis `ORACLE_URL`, `ORACLE_USER` e `ORACLE_PASSWORD`.

## Modelo no Oracle Data Modeler

1. Abra o Oracle SQL Developer Data Modeler e selecione **File → Import → DDL File**. Importe `database/01_DDL.SQL` para um modelo relacional Oracle que suporte colunas identity.
2. Confira as cinco tabelas, as quatro FKs e as cardinalidades: cada filho referencia exatamente um pai, cada pai tem zero ou muitos filhos. O par loja e produto é único em TA_ESTOQUE.
3. Organize as tabelas, exiba atributos e indicadores PK/FK e salve o projeto. No modelo relacional, use **Print Diagram → To Image File** e exporte `docs/modelo-oracle.png`. O gerador `docs/gerar_pdfs.py` incorpora essa imagem ao relatório.
4. Gere os PDFs com Python e os pacotes `reportlab`, `pypdf` e `Pillow` usando `python docs/gerar_pdfs.py`. Configure `GS_EQUIPE`, `GS_INTEGRANTES` (lista separada por barra vertical), `GS_GITHUB` e `GS_DEPLOY` com os dados confirmados.

[Oracle Data Modeler](https://docs.oracle.com/en/database/oracle/sql-developer-data-modeler/22.2/dmdug/data-modeler-concepts-usage.html) · [Render Blueprint](https://render.com/docs/blueprint-spec) · [Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html)

