param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($env:DEMO_KEY)) { throw 'Defina DEMO_KEY.' }
$headers = @{ 'X-Demo-Key' = $env:DEMO_KEY }
$BaseUrl = $BaseUrl.TrimEnd('/')

function Enviar($metodo, $caminho, $corpo) {
    $params = @{ Method = $metodo; Uri = "$BaseUrl$caminho"; Headers = $headers }
    if ($null -ne $corpo) {
        $params.Body = $corpo | ConvertTo-Json -Compress
        $params.ContentType = 'application/json'
    }
    Invoke-RestMethod @params
}

function Exigir($condicao, $mensagem) {
    if (-not $condicao) { throw $mensagem }
}

$health = Invoke-RestMethod "$BaseUrl/actuator/health"
Exigir ($health.status -eq 'UP') 'Health check falhou.'
$busca = '/api/ofertas?q=Parafuso%20M8%2040%20mm&lat=-23.5584&lon=-46.6353'
$ofertas = Invoke-RestMethod "$BaseUrl$busca"
$oferta = @($ofertas)[0]
Exigir ($oferta.disponivel -gt 1) 'A carga de teste precisa ter saldo disponivel.'
$lojaId = $oferta.loja.id
$reserva = Enviar POST '/api/reservas' @{ moradorId = 1; estoqueId = $oferta.estoqueId; quantidade = 1 }
$id = $reserva.id
try {
    Exigir ($reserva.status -eq 'PENDENTE') 'A reserva deve comecar pendente.'
    $confirmada = Enviar PUT "/api/lojas/$lojaId/reservas/$id/status" @{ status = 'CONFIRMADA' }
    Exigir ($confirmada.status -eq 'CONFIRMADA') 'A confirmacao falhou.'
    $consultada = Enviar GET "/api/reservas/$id" $null
    Exigir ($consultada.status -eq 'CONFIRMADA') 'A leitura da reserva falhou.'
    $ofertasApos = Invoke-RestMethod "$BaseUrl$busca"
    $apos = @($ofertasApos) | Where-Object estoqueId -eq $oferta.estoqueId
    Exigir ($apos.disponivel -eq ($oferta.disponivel - 1)) 'O saldo disponivel deve cair ao confirmar.'
} finally {
    Enviar DELETE "/api/reservas/${id}?moradorId=1" $null | Out-Null
}
$cancelada = Enviar GET "/api/reservas/$id" $null
Exigir ($cancelada.status -eq 'CANCELADA') 'O cancelamento falhou.'
$ofertasApos = Invoke-RestMethod "$BaseUrl$busca"
$saldo = @($ofertasApos) | Where-Object estoqueId -eq $oferta.estoqueId
Exigir ($saldo.disponivel -eq $oferta.disponivel) 'O cancelamento deve liberar o saldo.'
'Busca, reserva, confirmacao, consulta, cancelamento e saldo verificados.'

