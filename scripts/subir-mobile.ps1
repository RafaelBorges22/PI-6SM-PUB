<#
.SYNOPSIS
    Sobe o frontend Mobile (Android) no emulador: emulador -> install -> launch.

.DESCRIPTION
    Automatiza a sequencia manual de subir o app Android. Nao gerencia a infra
    nem o backend: apenas avisa se a API nao estiver respondendo, porque
    instalar o APK sem backend no ar e um caso legitimo.

    Antes de rodar, tenha o backend no ar (em outro terminal):
        docker compose up -d
        cd backend; .\gradlew bootRun

    E seguro reexecutar: se o emulador ja estiver aberto e saudavel, o script
    reaproveita; se estiver travado em 'offline', derruba e sobe de novo.

    O emulador sobe em boot frio por padrao. O quick boot do Android Studio
    restaura o snapshot 'default_boot', e um snapshot salvo de um emulador que
    morreu na marra volta com o adbd quebrado: o device fica 'offline' eterno.
    Use -ReaproveitarSnapshot se quiser o boot rapido mesmo assim.

.EXAMPLE
    .\scripts\subir-mobile.ps1
    Usa o primeiro AVD disponivel e checa a API na porta 8085.

.EXAMPLE
    .\scripts\subir-mobile.ps1 -Avd Pixel_7_API_34 -PortaBackend 8080
    Escolhe outro emulador e outra porta de backend.

.EXAMPLE
    .\scripts\subir-mobile.ps1 -PularBackendCheck
    Instala o APK sem reclamar que a API esta fora do ar.

.EXAMPLE
    .\scripts\subir-mobile.ps1 -ReaproveitarSnapshot
    Sobe pelo snapshot (quick boot). Mais rapido, mas volta a travar em
    'offline' se o snapshot estiver corrompido.
#>
param(
    [string]$Avd = '',

    [int]$PortaBackend = 8085,

    [switch]$PularBackendCheck,

    [switch]$ReaproveitarSnapshot,

    [int]$TimeoutBootSegundos = 180
)

$ErrorActionPreference = 'Stop'

function Escrever-Passo { param([string]$Texto) Write-Host "`n==> $Texto" -ForegroundColor Cyan }
function Escrever-Ok    { param([string]$Texto) Write-Host "    OK: $Texto" -ForegroundColor Green }
function Escrever-Aviso { param([string]$Texto) Write-Host "    AVISO: $Texto" -ForegroundColor Yellow }
function Abortar        { param([string]$Texto) Write-Host "`nERRO: $Texto" -ForegroundColor Red; exit 1 }

# --------------------------------------------------------------- 1. SDK ----

Escrever-Passo 'Localizando o Android SDK'

$sdk = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($sdk)) { $sdk = $env:ANDROID_SDK_ROOT }
if ([string]::IsNullOrWhiteSpace($sdk)) { $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }

if (-not (Test-Path $sdk)) {
    Abortar ("SDK do Android nao encontrado em '{0}'. Defina ANDROID_HOME ou instale o SDK pelo Android Studio." -f $sdk)
}

$adb      = Join-Path $sdk 'platform-tools\adb.exe'
$emulador = Join-Path $sdk 'emulator\emulator.exe'

if (-not (Test-Path $adb)) {
    Abortar ("adb.exe nao encontrado em '{0}'. Instale o pacote 'platform-tools' no SDK Manager." -f $adb)
}
if (-not (Test-Path $emulador)) {
    Abortar ("emulator.exe nao encontrado em '{0}'. Instale o pacote 'emulator' no SDK Manager." -f $emulador)
}

Escrever-Ok ("SDK em {0}" -f $sdk)

# --------------------------------------------------------------- 2. AVD ----

Escrever-Passo 'Verificando os emuladores configurados'

$avds = @(& $emulador -list-avds |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
    ForEach-Object { $_.Trim() })

if ($avds.Count -eq 0) {
    Abortar 'Nenhum AVD configurado. Crie um no Android Studio: Tools > Device Manager > Create Device.'
}

if ([string]::IsNullOrWhiteSpace($Avd)) {
    $Avd = $avds[0]
    if ($avds.Count -gt 1) {
        Escrever-Aviso ("{0} AVDs disponiveis; usando o primeiro. Use -Avd para escolher outro." -f $avds.Count)
    }
} elseif ($avds -notcontains $Avd) {
    Abortar ("AVD '{0}' nao existe. Disponiveis: {1}" -f $Avd, ($avds -join ', '))
}

Escrever-Ok ("AVD: {0}" -f $Avd)

# ----------------------------------------------------------- 3. backend ----

if ($PularBackendCheck) {
    Escrever-Passo 'Checagem do backend pulada (-PularBackendCheck)'
} else {
    Escrever-Passo ("Checando o backend em 127.0.0.1:{0}" -f $PortaBackend)

    $backendNoAr = $false
    try {
        $cliente = New-Object System.Net.Sockets.TcpClient
        if ($cliente.ConnectAsync('127.0.0.1', $PortaBackend).Wait(3000)) {
            $backendNoAr = $true
        }
        $cliente.Close()
    } catch {
        $backendNoAr = $false
    }

    if ($backendNoAr) {
        Escrever-Ok 'Backend respondendo'
    } else {
        Escrever-Aviso ("Nada escutando na porta {0}. O app vai abrir, mas o login falha." -f $PortaBackend)
        Escrever-Aviso 'Suba o backend em outro terminal:  cd backend; .\gradlew bootRun'
    }
}

# ---------------------------------------------------------- 4. emulador ----

function Obter-Emulador {
    # Retorna o primeiro emulador de "adb devices" com seu estado (device/offline/
    # unauthorized), ou $null se nenhum estiver conectado.
    $linhas = @(& $adb devices | Select-Object -Skip 1)
    foreach ($linha in $linhas) {
        if ($linha -match '^(emulator-\S+)\s+(\S+)\s*$') {
            return [pscustomobject]@{ Serial = $Matches[1]; Estado = $Matches[2] }
        }
    }
    return $null
}

function Encerrar-Emulador {
    # Desliga o emulador pelo console (porta do serial). Funciona mesmo com o
    # device 'offline', porque o console e servido pelo processo do emulador e
    # nao depende do adbd do guest.
    param([string]$Serial)

    $porta = 5554
    if ($Serial -match '^emulator-(\d+)$') { $porta = [int]$Matches[1] }

    $token    = ''
    $arqToken = Join-Path $env:USERPROFILE '.emulator_console_auth_token'
    if (Test-Path $arqToken) { $token = (Get-Content $arqToken -Raw).Trim() }

    try {
        $cliente  = New-Object System.Net.Sockets.TcpClient('127.0.0.1', $porta)
        $escritor = New-Object System.IO.StreamWriter($cliente.GetStream())
        $escritor.AutoFlush = $true
        if (-not [string]::IsNullOrWhiteSpace($token)) {
            $escritor.WriteLine("auth $token")
            Start-Sleep -Milliseconds 300
        }
        $escritor.WriteLine('kill')
        Start-Sleep -Milliseconds 1500
        $cliente.Close()
    } catch {
        Escrever-Aviso 'Nao consegui falar com o console do emulador; feche a janela dele na mao.'
    }

    for ($i = 0; $i -lt 20; $i++) {
        if (-not (Get-Process qemu-system-x86_64 -ErrorAction SilentlyContinue)) { break }
        Start-Sleep -Seconds 1
    }
}

Escrever-Passo 'Preparando o emulador'

$emu = Obter-Emulador

if ($emu -and $emu.Estado -eq 'device') {
    $serial = $emu.Serial
    Escrever-Ok ("Emulador '{0}' ja esta rodando; reaproveitando." -f $serial)
} else {
    if ($emu) {
        # Estado tipico: 'offline'. O snapshot de quick boot restaurou um guest
        # com o adbd morto -- o adb conecta na porta 5555 mas o handshake nunca
        # completa, e o device fica offline para sempre. Derruba e sobe de novo.
        Escrever-Aviso ("Emulador '{0}' esta em estado '{1}', nao utilizavel. Encerrando." -f $emu.Serial, $emu.Estado)
        Encerrar-Emulador $emu.Serial
    }

    $argumentos = @('-avd', $Avd)
    if ($ReaproveitarSnapshot) {
        Write-Host ("    Iniciando '{0}' pelo snapshot (quick boot)..." -f $Avd)
    } else {
        # Boot frio: ignora o snapshot 'default_boot'. Custa ~30s a mais, mas um
        # snapshot salvo de um emulador morto na marra sobe travado em 'offline'.
        $argumentos += '-no-snapshot-load'
        Write-Host ("    Iniciando '{0}' em boot frio, numa janela separada..." -f $Avd)
    }

    Start-Process -FilePath $emulador -ArgumentList $argumentos | Out-Null

    Write-Host ("    Aguardando o Android terminar de bootar (ate {0}s)..." -f $TimeoutBootSegundos)

    # Sem 'adb wait-for-device' aqui de proposito: ele bloqueia sem timeout ate o
    # device sair de 'offline', o que nunca acontece com snapshot corrompido.
    $relogio = [System.Diagnostics.Stopwatch]::StartNew()
    $bootou  = $false
    $serial  = ''
    $ultimoEstado = 'nenhum device'

    while ($relogio.Elapsed.TotalSeconds -lt $TimeoutBootSegundos) {
        $emu = Obter-Emulador
        if ($emu) {
            $ultimoEstado = $emu.Estado
            if ($emu.Estado -eq 'device') {
                $prop = ''
                try { $prop = (& $adb -s $emu.Serial shell getprop sys.boot_completed 2>$null) } catch { $prop = '' }
                if ("$prop".Trim() -eq '1') {
                    $bootou = $true
                    $serial = $emu.Serial
                    break
                }
            }
        }
        Start-Sleep -Seconds 3
    }

    if (-not $bootou) {
        if ($ultimoEstado -eq 'offline') {
            Abortar (@"
O emulador ficou '{0}' por {1:N0}s -- o Android nao subiu direito.
Quase sempre e o snapshot de quick boot corrompido. Tente:
  1. Feche a janela do emulador.
  2. Apague o snapshot:
       Remove-Item -Recurse -Force "$env:USERPROFILE\.android\avd\{2}.avd\snapshots"
  3. Rode este script de novo.
"@ -f $ultimoEstado, $relogio.Elapsed.TotalSeconds, $Avd)
        }
        Abortar ("O emulador nao terminou de bootar em {0}s (ultimo estado: {1}). Rode de novo com -TimeoutBootSegundos maior, ou verifique a janela do emulador." -f $TimeoutBootSegundos, $ultimoEstado)
    }

    Escrever-Ok ("Emulador '{0}' pronto em {1:N0}s" -f $serial, $relogio.Elapsed.TotalSeconds)
}

# ----------------------------------------------------------- 5. install ----

Escrever-Passo 'Compilando e instalando o APK (a primeira vez demora)'

$frontend = Join-Path (Split-Path -Parent $PSScriptRoot) 'frontend'
$gradlew  = Join-Path $frontend 'gradlew.bat'

if (-not (Test-Path $gradlew)) {
    Abortar ("gradlew.bat nao encontrado em '{0}'." -f $frontend)
}

Push-Location $frontend
try {
    # Garante que o Gradle instale neste emulador, e nao em outro device conectado.
    $env:ANDROID_SERIAL = $serial
    & $gradlew ':composeApp:installDebug' --console=plain
    if ($LASTEXITCODE -ne 0) {
        Abortar ("o build falhou (exit {0}). Veja o erro do Gradle acima." -f $LASTEXITCODE)
    }
} finally {
    Pop-Location
}

Escrever-Ok 'APK instalado'

# ------------------------------------------------------------ 6. launch ----

Escrever-Passo 'Abrindo o app'

& $adb -s $serial shell am start -n 'br.com.smartparking.app/br.com.smartparking.app.MainActivity' | Out-Null
if ($LASTEXITCODE -ne 0) {
    Escrever-Aviso 'Nao consegui abrir via adb. Toque no icone "Smart Parking" no emulador.'
} else {
    Escrever-Ok 'App aberto no emulador'
}

# ------------------------------------------------------------ lembrete ----

Write-Host "`n--------------------------------------------------------" -ForegroundColor DarkGray
Write-Host ' Na tela de login, confira a URL da API:' -ForegroundColor White
Write-Host ("   URL   : http://10.0.2.2:{0}" -f $PortaBackend) -ForegroundColor Yellow
Write-Host '   Email : admin@smartparking.com'
Write-Host '   Senha : 123456'
Write-Host ''
Write-Host ' O campo ja vem preenchido com a porta 8085; so ajuste se' -ForegroundColor DarkGray
Write-Host ' o backend subiu em outra. 10.0.2.2 e o host da sua maquina' -ForegroundColor DarkGray
Write-Host ' visto de dentro do emulador -- localhost apontaria para o' -ForegroundColor DarkGray
Write-Host ' proprio Android.' -ForegroundColor DarkGray
Write-Host "--------------------------------------------------------`n" -ForegroundColor DarkGray
