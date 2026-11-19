from pathlib import Path
from xml.sax.saxutils import escape
import os
import re

from PIL import Image
from pypdf import PdfReader
from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Paragraph

root = Path(__file__).resolve().parents[1]
docs = root / "docs"
W, H = A4
NAVY = HexColor("#12263A")
TEAL = HexColor("#106954")
GRAY = HexColor("#47566A")
LIGHT = HexColor("#EAF2F0")
FONT = "Helvetica"
BOLD = "Helvetica-Bold"

team = os.getenv("GS_EQUIPE", "Calçada LTDA")
roster = ("Bernardo Lozório Gomes Y Gomes — RM 564943|Caio M. Lins — RM 559805|"
          "Guilherme Augusto Ferreira Fernandes — RM 562107|Murilo Gonzalez Bez Chleba — RM 566199")
names = sorted(os.getenv("GS_INTEGRANTES", roster).split("|"),
               key=str.casefold)
github = os.getenv("GS_GITHUB", "https://github.com/MuriloBezChleba/temaqui-citizentech")
deploy = os.getenv("GS_DEPLOY", "Pendente de provisionamento no Render")
ddl = (root / "database" / "01_DDL.SQL").read_text()
tables = {}
for name, body in re.findall(r"CREATE TABLE (TA_\w+) \((.*?)\n\);", ddl, re.S):
    columns = re.findall(r"^    (\w+) (NUMBER\(\d+(?:,\d+)?\)|VARCHAR2\(\d+ CHAR\)|TIMESTAMP WITH TIME ZONE)", body, re.M)
    tables[name] = columns
foreign_keys = {}
for child, col, parent in re.findall(
        r"ALTER TABLE (TA_\w+) ADD CONSTRAINT \w+\s+FOREIGN KEY \((\w+)\) REFERENCES (TA_\w+)", ddl):
    foreign_keys[(child, col)] = parent

agile = PdfReader(root / "TemAqui - Agile - GS CitizenTech.pdf")


def paragraph(c, text, x, y, width=485, size=11, color=NAVY):
    p = Paragraph(text, ParagraphStyle("body", fontName=FONT, fontSize=size,
                                       leading=size * 1.4, textColor=color))
    _, h = p.wrap(width, H)
    p.drawOn(c, x, y - h)
    return y - h


def cover(c, discipline, subtitle):
    c.setFillColor(NAVY)
    c.rect(0, 0, W, H, fill=1, stroke=0)
    c.setFillColor(HexColor("#82DCC2"))
    c.setFont(BOLD, 12)
    c.drawString(55, 748, "FIAP  /  GLOBAL SOLUTION 2026.2")
    c.setFillColor(white)
    c.setFont(BOLD, 62)
    c.drawString(52, 581, "TemAqui")
    paragraph(c, subtitle, 55, 552, 472, 18, white)
    c.setStrokeColor(HexColor("#82DCC2"))
    c.line(55, 463, W - 55, 463)
    y = paragraph(c, f"<b>{discipline}</b><br/>Engenharia de Software · 2º ano<br/>"
                     f"Equipe {escape(team)}", 55, 425, 483, 14, white) - 45
    y = paragraph(c, "<b>Integrantes</b>", 55, y, 483, 13, white) - 10
    for name in names:
        y = paragraph(c, escape(name), 55, y, 483, 13, white) - 5
    paragraph(c, "São Paulo · Liberdade · Novembro de 2026", 55, 76, 483, 11, white)
    c.showPage()


def page(c, discipline, number, title, kicker=None):
    c.setFillColor(TEAL)
    c.setFont(BOLD, 9)
    c.drawString(55, 797, f"TEMAQUI   /   {discipline.upper()}")
    c.setFillColor(NAVY)
    c.setFont(BOLD, 29)
    c.drawString(55, 746, title)
    if kicker:
        paragraph(c, kicker, 56, 720, 483, 10, GRAY)
    c.setStrokeColor(LIGHT)
    c.line(55, 52, W - 55, 52)
    paragraph(c, "FIAP · Global Solution 2026.2 · CitizenTech", 55, 39, 380, 9, GRAY)
    c.setFont(BOLD, 9)
    c.drawRightString(W - 55, 29, str(number))


def sections(c, entries, y=685):
    for title, body in entries:
        y = paragraph(c, f"<b>{title}</b>", 55, y, 485, 14, TEAL) - 7
        y = paragraph(c, body, 55, y, 485, 11) - 23
    assert y > 45, y


def index(c, discipline, items):
    page(c, discipline, 2, "Sumário")
    y = 687
    for name, number in items:
        c.setFillColor(LIGHT)
        c.roundRect(55, y - 38, 485, 38, 6, fill=1, stroke=0)
        paragraph(c, escape(name), 69, y - 10, 440, 12)
        c.setFillColor(TEAL)
        c.setFont(BOLD, 12)
        c.drawRightString(524, y - 23, str(number))
        y -= 52
    c.showPage()


def entity_box(c, name, x, y):
    width = 219
    height = 28 + 16 * len(tables[name]) + 8
    c.setFillColor(LIGHT)
    c.roundRect(x, y - height, width, height, 6, stroke=0, fill=1)
    c.setFillColor(TEAL)
    c.rect(x, y - 25, width, 25, fill=1, stroke=0)
    c.setFillColor(white)
    c.setFont(BOLD, 10)
    c.drawString(x + 9, y - 17, name)
    for idx, (col, typ) in enumerate(tables[name]):
        ycol = y - 40 - idx * 16
        mark = "PK" if col == "id" else "FK" if (name, col) in foreign_keys else ""
        c.setFillColor(NAVY)
        c.setFont(BOLD, 8)
        c.drawString(x + 8, ycol, mark)
        c.setFont(FONT, 8)
        c.drawString(x + 30, ycol, col)
        c.drawRightString(x + width - 8, ycol, typ)
    return (x, y - height, width, height)


def join(c, points, parent_label, child_label):
    c.setStrokeColor(TEAL)
    path = c.beginPath()
    path.moveTo(*points[0])
    for xy in points[1:]:
        path.lineTo(*xy)
    c.drawPath(path)
    c.setFillColor(TEAL)
    c.setFont(BOLD, 9)
    for label, pos in (("1", parent_label), ("0..*", child_label)):
        c.drawString(*pos, label)


def der(c):
    entity_box(c, "TA_LOJA", 55, 680)
    entity_box(c, "TA_PRODUTO", 321, 680)
    entity_box(c, "TA_ESTOQUE", 188, 473)
    entity_box(c, "TA_MORADOR", 55, 277)
    entity_box(c, "TA_RESERVA", 321, 277)
    join(c, [(163, 532), (163, 500), (227, 500), (227, 473)], (153, 519), (231, 477))
    join(c, [(431, 612), (431, 500), (367, 500), (367, 473)], (435, 598), (371, 477))
    join(c, [(340, 341), (340, 311), (430, 311), (430, 277)], (328, 326), (434, 280))
    join(c, [(274, 215), (321, 215)], (279, 221), (298, 221))
    paragraph(c, "PK · chave primária &nbsp;&nbsp; FK · chave estrangeira<br/>"
                 "Cada linha filha referencia um pai. Cada pai pode ter zero ou mais filhos. "
                 "O par (loja_id, produto_id) é único no estoque.", 55, 140, 258, 9, GRAY)


def project(c, label):
    page(c, label, 3, "Projeto TemAqui", "São Paulo · Liberdade · comércio de bairro")
    sections(c, [
        ("Problema e justificativa", "O morador encontra o endereço da ferragem, mas tem pouca visibilidade sobre "
         "o estoque. Para comprar uma peça no mesmo dia, pode visitar várias lojas ou comprar de longe. "
         "O pequeno lojista precisa aparecer na busca sem manter uma loja virtual."),
        ("Objetivo", "Dar visibilidade ao estoque local e permitir que o morador consulte disponibilidade, "
         "faça uma reserva e retire o item no balcão. A associação comercial pode acompanhar retiradas por loja."),
        ("Escopo e funcionalidades", "Cadastro de moradores, lojas e produtos, publicação de quantidades e preços, "
         "busca por item e distância, idade do estoque e reserva com confirmação, recusa, cancelamento e retirada. "
         "O pagamento é feito na loja. WhatsApp, OCR e importação de planilha ficam no backlog descrito em Agile."),
        ("Dados e impacto", "A carga acadêmica foi criada para a GS com dados fictícios. "
         "Acompanhar retiradas por loja e, com eventos do front-end, conversão de buscas em retiradas. "
         "Os ODS 9, 10 e 11 se conectam à inclusão digital, à visibilidade de pequenos negócios e ao comércio de proximidade."),
    ])
    c.showPage()


def text_box(c, x, top, width, title, body, height=79):
    c.setFillColor(LIGHT)
    c.roundRect(x, top - height, width, height, 5, fill=1, stroke=0)
    paragraph(c, f"<b>{title}</b>", x + 9, top - 8, width - 18, 10, TEAL)
    y = paragraph(c, body, x + 9, top - 27, width - 18, 8.8)
    assert y > top - height, (title, y, top)


def uml(c):
    x, w = 55, 290
    text_box(c, x, 684, w, "TemAquiController",
             "bo: TemAquiBo<br/>buscar(), cadastrar(), loja(), listarEstoque(), publicar(), reservar(), "
             "acompanhar(), listarReservas(), decidir(), cancelar()", 83)
    text_box(c, x, 565, w, "TemAquiBo",
             "dao: TemAquiDao<br/>buscar(), cadastrar(), loja(), estoqueDaLoja(), publicar(), reservar(), "
             "reserva(), reservasDaLoja(), decidir(), cancelar()", 83)
    text_box(c, x, 446, w, "TemAquiDao",
             "jdbc: JdbcTemplate<br/>cadastrarMorador(), cadastrarLoja(), cadastrarProduto(), "
             "buscarOfertas(), estoque(), reserva(), mudarStatus(), unidadesComprometidas()", 83)
    text_box(c, x, 327, w, "Modelo · tipos aninhados",
             "Morador, Loja, Produto, Estoque, Reserva, Oferta e Status<br/>"
             "CadastroMorador, CadastroLoja, CadastroProduto, PublicacaoEstoque, PedidoReserva e MudancaReserva", 96)
    text_box(c, 364, 684, 176, "TemAquiApplication", "main(String[] args)", 58)
    text_box(c, 364, 608, 176, "AcessoDemonstracao", "chave: byte[]<br/>addInterceptors(registry)", 66)
    text_box(c, 364, 524, 176, "TratamentoErros", "regra(), validacao(), integridade(), indisponivel(), persistencia()", 76)
    text_box(c, 364, 430, 176, "RegraException", "status: HttpStatus<br/>status(), ausente(), conflito()", 70)
    c.setStrokeColor(TEAL)
    c.setFillColor(TEAL)
    for y1, y2 in [(601, 565), (482, 446), (363, 327)]:
        c.line(195, y1, 195, y2 + 2)
        c.line(195, y2 + 2, 191, y2 + 8)
        c.line(195, y2 + 2, 199, y2 + 8)
    c.line(515, 448, 515, 430)
    c.line(515, 430, 511, 436)
    c.line(515, 430, 519, 436)
    paragraph(c, "Setas indicam dependência. A persistência usa JDBC. Os registros do domínio "
                 "guardam as referências com IDs, conforme o DER. Reserva usa Status e calcula o total "
                 "com o preço unitário registrado na criação.", 55, 195, 485, 11, GRAY)


def images(c, data, top=650):
    col = 485 / len(data)
    for i, (path, title) in enumerate(data):
        with Image.open(path) as img:
            iw, ih = img.size
        height = min(400, (col - 14) * ih / iw)
        width = height * iw / ih
        x = 55 + i * col
        c.drawImage(str(path), x + (col - width) / 2, top - height, width, height, mask="auto")
        paragraph(c, f"<b>{title}</b>", x + 6, top - height - 10, col - 12, 10, TEAL)


def main():
    work = root / "tmp" / "pdfs"
    work.mkdir(parents=True, exist_ok=True)
    prototypes = []
    for idx in (11, 12, 13):
        for source in agile.pages[idx].images:
            path = work / source.name
            path.write_bytes(source.data)
            prototypes.append(path)

    c = canvas.Canvas(str(docs / "TemAqui-Database.pdf"), pagesize=A4)
    c.setTitle("TemAqui | Database Design")
    cover(c, "Database Design", "Modelo de dados do estoque de bairro e das reservas.")
    index(c, "Database Design", [("Projeto TemAqui", 3), ("Modelo lógico-relacional", 4), ("Implementação e consultas", 5)])
    project(c, "Database Design")
    oracle_image = docs / "modelo-oracle.png"
    note = ("Exportado do Oracle SQL Developer Data Modeler." if oracle_image.exists()
            else "Diagrama de referência gerado do DDL. Exportação pelo Oracle Data Modeler pendente.")
    page(c, "Database Design", 4, "Modelo lógico-relacional", note)
    if oracle_image.exists():
        c.drawImage(str(oracle_image), 55, 125, 485, 540, preserveAspectRatio=True, anchor="c", mask="auto")
        paragraph(c, "Cardinalidade das quatro FKs no esquema: cada registro filho tem um único pai. "
                     "Cada pai pode ter zero ou muitos registros filhos.", 55, 113, 485, 9, GRAY)
    else:
        der(c)
    c.showPage()
    page(c, "Database Design", 5, "Implementação e consultas")
    sections(c, [
        ("01_DDL.SQL · Estrutura", "Cria cinco tabelas, quatro chaves estrangeiras, índices para as FKs, "
         "unicidade de celular, nome de produto e par loja/produto. Restrições validam saldo, preço, "
         "coordenadas e estado da reserva. Colunas identity geram IDs acima da faixa da carga."),
        ("02_DML.SQL · Carga", "Cinco moradores, cinco lojas, cinco produtos, seis posições de estoque e cinco "
         "reservas. Lojas, pessoas, endereços de teste e telefones são fictícios. A carga inclui "
         "estoque recente e antigo e os cinco estados de reserva."),
        ("03_DQL.SQL · Consultas", "1. Produto e estoque com INNER JOIN retornam as duas ofertas de parafuso M8.<br/>"
         "2. Estoque e reserva com INNER JOIN e GROUP BY somam as unidades retiradas por produto. "
         "A carga retorna uma retirada e duas unidades do produto 2.<br/>"
         "3. Loja e estoque com LEFT OUTER JOIN, WHERE e ORDER BY listam ferragens sem item publicado. "
         "A carga retorna a Ferragens Demo E."),
        ("Execução", "Abra uma conexão do esquema Oracle FIAP no SQL Developer. Execute os três arquivos na ordem "
         "numérica com Run Script (F5). Use um esquema sem as tabelas TA_ criadas ao aplicar o DDL."),
    ])
    c.showPage()
    c.save()

    label = "Domain Driven Design · Java"
    c = canvas.Canvas(str(docs / "TemAqui-Java.pdf"), pagesize=A4)
    c.setTitle("TemAqui | Domain Driven Design - Java")
    cover(c, label, "API REST para encontrar produtos, reservar e retirar na loja.")
    index(c, label, [("Projeto TemAqui", 3), ("Diagrama de classes", 4), ("DER", 5),
                     ("Protótipos", 6), ("Endpoints", 8), ("Execução e deploy", 10)])
    project(c, label)
    page(c, label, 4, "Diagrama de classes", "Camadas HTTP, negócio e persistência")
    uml(c)
    c.showPage()
    page(c, label, 5, "DER", "A API usa as cinco tabelas do esquema Oracle.")
    der(c)
    c.showPage()
    page(c, label, 6, "Protótipos do morador", "TemAqui Agile · páginas 12 e 13")
    images(c, list(zip(prototypes[:3], ["01 · Busca", "02 · Ofertas", "03 · Reserva"])))
    paragraph(c, "A busca é pública. A lista mostra disponibilidade, distância e idade do estoque. "
                 "O resumo da reserva usa o preço unitário gravado e orienta o pagamento no balcão.", 55, 298, 485)
    c.showPage()
    page(c, label, 7, "Protótipos do lojista", "TemAqui Agile · página 14")
    images(c, list(zip(prototypes[3:], ["04 · Painel do lojista", "05 · WhatsApp"])))
    paragraph(c, "O painel se conecta à publicação manual, à lista de reservas e às decisões da loja. "
                 "O fluxo de WhatsApp ilustra a evolução prevista na proposta Agile.", 55, 212, 485)
    c.showPage()

    tables_http = [
        [
            ("GET", "/api/ofertas", "q, lat, lon na URL. Público. 200 com ofertas por distância."),
            ("GET", "/api/lojas/{lojaId}", "200 com nome, endereço, horário, categoria e coordenadas."),
            ("POST", "/api/moradores", '{"nome":"Demo","celular":"11000000088","bairro":"Liberdade"} · 201'),
            ("POST", "/api/lojas", '{"nome":"Ferragens Demo","endereco":"Rua Demo","horario":"09–18h",'
             '"categoria":"Ferragens","latitude":-23.5584,"longitude":-46.6353} · 201'),
            ("POST", "/api/produtos", '{"nome":"Parafuso M8 45 mm"} · 201'),
            ("GET", "/api/lojas/{lojaId}/estoques", "200 com posições de estoque da loja."),
            ("PUT", "/api/lojas/{lojaId}/estoques/{produtoId}", '{"produtoId":1,"quantidade":20,"preco":2.50} · 200'),
        ],
        [
            ("POST", "/api/reservas", '{"moradorId":1,"estoqueId":1,"quantidade":2} · 201 PENDENTE'),
            ("GET", "/api/reservas/{id}", "200 com quantidade, preço unitário, total, estado e data."),
            ("GET", "/api/lojas/{lojaId}/reservas", "200 com as reservas da loja em ordem decrescente de criação."),
            ("PUT", "/api/lojas/{lojaId}/reservas/{id}/status",
             '{"status":"CONFIRMADA"} · 200. Também aceita RECUSADA e RETIRADA.'),
            ("DELETE", "/api/reservas/{id}", "moradorId na URL. 204. Cancela e libera o saldo comprometido."),
            ("GET", "/actuator/health", "Público. 200 com UP quando a aplicação alcança o banco."),
        ],
    ]
    for number, table in enumerate(tables_http, start=8):
        page(c, label, number, "Contrato HTTP", "Catálogo e estoque" if number == 8 else "Reservas e saúde")
        y = 683
        for method, route, body in table:
            c.setFillColor(LIGHT)
            c.roundRect(55, y - 67, 485, 67, 5, fill=1, stroke=0)
            paragraph(c, f"<b>{method}</b>", 65, y - 8, 60, 11, TEAL)
            paragraph(c, f"<b>{escape(route)}</b>", 126, y - 8, 407, 10)
            paragraph(c, escape(body), 126, y - 28, 400, 9, GRAY)
            y -= 72
        paragraph(c, "Envie Content-Type: application/json ao usar corpo. Rotas protegidas recebem X-Demo-Key. "
                     "Erros em application/problem+json incluem 400, 401, 404, 409 e 503.",
                  55, y - 9, 485, 10, GRAY)
        c.showPage()

    page(c, label, 10, "Execução e deploy")
    sections(c, [
        ("Ferramentas", "JDK 17, Maven 3.9, Spring Boot 3.5.8, driver Oracle ojdbc11 e Oracle FIAP "
         "com suporte a identity e TIMESTAMP WITH TIME ZONE. Use IntelliJ ou terminal e Oracle SQL Developer."),
        ("1 · Preparar o banco", "Execute database/01_DDL.SQL e database/02_DML.SQL em um esquema acadêmico. "
         "Defina ORACLE_URL (JDBC Thin da FIAP), ORACLE_USER, ORACLE_PASSWORD e DEMO_KEY."),
        ("2 · Compilar e iniciar", "<font face='Courier'>mvn -f java/pom.xml clean package</font><br/>"
         "<font face='Courier'>java -jar java/target/temaqui-1.0.0.jar</font><br/>"
         "Verifique http://localhost:8080/actuator/health e rode java/verificar-api.ps1 no PowerShell."),
        ("3 · Publicar no Render", "Vincule o repositório ao Blueprint render.yaml. Informe as variáveis no painel "
         "do serviço e aguarde o health check. Valide o fluxo apontando o roteiro PowerShell para a URL do Render."),
        ("Endereços do projeto", f"<b>GitHub</b> · {escape(github)}<br/><b>Deploy</b> · {escape(deploy)}"),
    ])
    c.showPage()
    c.save()


if __name__ == "__main__":
    main()

