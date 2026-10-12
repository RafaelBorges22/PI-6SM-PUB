<#
.SYNOPSIS
    Publica leituras MQTT como se fossem de um ESP32 (versao Windows/PowerShell).

.DESCRIPTION
    Serve para testar todo o caminho (broker -> backend -> banco -> frontends)
    antes de ter a placa em maos. Requer o container smartparking-mosquitto no ar.

.EXAMPLE
    .\scripts\simular-sensor.ps1
    Alterna LIVRE/OCUPADA a cada 5s na vaga 01.

.EXAMPLE
    .\scripts\simular-sensor.ps1 -Acao ocupada -Vaga 2
    Publica uma unica leitura OCUPADA para a vaga 02.
#>
param(
    [ValidateSet('ciclo', 'ocupada', 'livre')]
    [string]$Acao = 'ciclo',

    [int]$Vaga = 1,

    [string]$DeviceId = '',

    [string]$Container = 'smartparking-mosquitto'
)

if ([string]::IsNullOrWhiteSpace($DeviceId)) {
    $DeviceId = 'ESP32-VAGA-{0:D2}' -f $Vaga
}
$topico = 'smart-parking/vaga/{0:D2}' -f $Vaga

function Publicar {
    param([string]$Status, [string]$Distancia)

    $payload = '{{"deviceId":"{0}","vagaId":{1},"distancia":{2},"status":"{3}"}}' -f $DeviceId, $Vaga, $Distancia, $Status
    docker exec $Container mosquitto_pub -h localhost -t $topico -m $payload
    Write-Host ("[{0}] {1} <- {2}" -f (Get-Date -Format 'HH:mm:ss'), $topico, $payload)
}

switch ($Acao) {
    'ocupada' { Publicar -Status 'OCUPADA' -Distancia '80.5' }
    'livre' { Publicar -Status 'LIVRE' -Distancia '250.8' }
    'ciclo' {
        Write-Host "Alternando o status da vaga $Vaga a cada 5s. Ctrl+C para parar."
        while ($true) {
            Publicar -Status 'OCUPADA' -Distancia '80.5'
            Start-Sleep -Seconds 5
            Publicar -Status 'LIVRE' -Distancia '250.8'
            Start-Sleep -Seconds 5
        }
    }
}
