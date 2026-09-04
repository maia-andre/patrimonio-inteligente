package com.patrimoniosjc.rfidpoc.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrimoniosjc.rfidpoc.R
import com.patrimoniosjc.rfidpoc.domain.LeituraPatrimonial
import com.patrimoniosjc.rfidpoc.domain.OrigemLeitura
import com.patrimoniosjc.rfidpoc.ui.theme.LocalCoresDeEstado
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Rótulo legível de cada origem de leitura. */
fun rotuloDaOrigem(origem: OrigemLeitura): String = when (origem) {
    OrigemLeitura.CODIGO_BARRAS -> "Código de barras"
    OrigemLeitura.NFC -> "NFC"
    OrigemLeitura.RFID_UHF -> "RFID UHF"
    OrigemLeitura.MANUAL -> "Manual"
}

/** Rótulo curto para o seletor, onde os quatro modos ficam lado a lado. */
private fun rotuloCurto(origem: OrigemLeitura): String = when (origem) {
    OrigemLeitura.CODIGO_BARRAS -> "Barras"
    OrigemLeitura.NFC -> "NFC"
    OrigemLeitura.RFID_UHF -> "UHF"
    OrigemLeitura.MANUAL -> "Manual"
}

@DrawableRes
private fun iconeDaOrigem(origem: OrigemLeitura): Int = when (origem) {
    OrigemLeitura.CODIGO_BARRAS -> R.drawable.ic_modo_barras
    OrigemLeitura.NFC -> R.drawable.ic_modo_nfc
    OrigemLeitura.RFID_UHF -> R.drawable.ic_modo_uhf
    OrigemLeitura.MANUAL -> R.drawable.ic_modo_manual
}

private fun horarioDe(instante: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(instante))

private val FormaPainel = RoundedCornerShape(16.dp)
private val FormaChip = RoundedCornerShape(12.dp)
private val FormaMenor = RoundedCornerShape(8.dp)

/**
 * Tela única do scanner. Um só sistema de controle: o seletor escolhe o modo
 * e o painel abaixo dele mostra o que aquele modo precisa — a prévia da
 * câmera, a instrução do NFC, a conexão do scanner BLE ou o campo manual.
 * A lista da sessão é o conteúdo principal; o registro técnico fica recolhido.
 */
@Composable
fun TelaScanner(
    estado: EstadoTelaScanner,
    aoAlternarConexao: () -> Unit,
    aoIniciarLeitura: () -> Unit,
    aoPararLeitura: () -> Unit,
    aoSelecionarModo: (OrigemLeitura) -> Unit = {},
    aoSolicitarPermissaoCamera: () -> Unit = {},
    aoRegistrarManual: (String) -> Boolean = { false },
    previaCamera: (@Composable (Modifier) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Cabecalho(total = estado.leituras.size)
        Spacer(modifier = Modifier.height(16.dp))

        SeletorDeModos(
            modos = estado.modos,
            modoSelecionado = estado.modoSelecionado,
            aoSelecionarModo = aoSelecionarModo
        )
        MotivosDeIndisponibilidade(
            modos = estado.modos,
            conectado = estado.conectado,
            aoAlternarConexao = aoAlternarConexao,
            aoSolicitarPermissaoCamera = aoSolicitarPermissaoCamera
        )
        Spacer(modifier = Modifier.height(12.dp))

        PainelDoModo(
            estado = estado,
            aoAlternarConexao = aoAlternarConexao,
            aoIniciarLeitura = aoIniciarLeitura,
            aoPararLeitura = aoPararLeitura,
            aoRegistrarManual = aoRegistrarManual,
            previaCamera = previaCamera
        )

        estado.avisoJaConferido?.let { aviso ->
            Spacer(modifier = Modifier.height(10.dp))
            AvisoDeDuplicata(aviso)
        }
        Spacer(modifier = Modifier.height(16.dp))

        SecaoDeLeituras(
            leituras = estado.leituras,
            modifier = Modifier.weight(1f)
        )

        RegistroTecnico(logs = estado.logs)
    }
}

@Composable
private fun Cabecalho(total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Scanner Patrimonial",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Conferência de bens da sessão",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (total == 1) "conferido" else "conferidos",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/** REQ-03/REQ-11 — quatro modos lado a lado; indisponível aparece desabilitado. */
@Composable
private fun SeletorDeModos(
    modos: List<ModoDaTela>,
    modoSelecionado: OrigemLeitura,
    aoSelecionarModo: (OrigemLeitura) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modos.forEach { modo ->
            ChipDeModo(
                modo = modo,
                selecionado = modo.origem == modoSelecionado,
                aoSelecionar = { aoSelecionarModo(modo.origem) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChipDeModo(
    modo: ModoDaTela,
    selecionado: Boolean,
    aoSelecionar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val esquema = MaterialTheme.colorScheme
    val fundo = when {
        selecionado -> esquema.primary
        else -> esquema.surfaceVariant
    }
    val conteudo = when {
        selecionado -> esquema.onPrimary
        modo.disponivel -> esquema.onSurfaceVariant
        else -> esquema.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Surface(
        onClick = aoSelecionar,
        enabled = modo.disponivel,
        shape = FormaChip,
        color = fundo,
        contentColor = conteudo,
        border = if (selecionado && !modo.disponivel) {
            BorderStroke(1.dp, esquema.outline)
        } else {
            null
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(iconeDaOrigem(modo.origem)),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rotuloCurto(modo.origem),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

/** REQ-11/CE-03 — o motivo de cada modo indisponível, com a ação que o resolve quando existe. */
@Composable
private fun MotivosDeIndisponibilidade(
    modos: List<ModoDaTela>,
    conectado: Boolean,
    aoAlternarConexao: () -> Unit,
    aoSolicitarPermissaoCamera: () -> Unit
) {
    val indisponiveis = modos.filter { it.motivo != null }
    if (indisponiveis.isEmpty()) return

    Column(modifier = Modifier.padding(top = 6.dp)) {
        indisponiveis.forEach { modo ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(iconeDaOrigem(modo.origem)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${rotuloDaOrigem(modo.origem)}: ${modo.motivo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                when {
                    modo.origem == OrigemLeitura.RFID_UHF && !conectado ->
                        TextButton(onClick = aoAlternarConexao) { Text("Conectar") }

                    modo.podeReabrirPermissao ->
                        TextButton(onClick = aoSolicitarPermissaoCamera) { Text("Permitir câmera") }
                }
            }
        }
    }
}

@Composable
private fun PainelDoModo(
    estado: EstadoTelaScanner,
    aoAlternarConexao: () -> Unit,
    aoIniciarLeitura: () -> Unit,
    aoPararLeitura: () -> Unit,
    aoRegistrarManual: (String) -> Boolean,
    previaCamera: (@Composable (Modifier) -> Unit)?
) {
    Surface(
        shape = FormaPainel,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            when (estado.modoSelecionado) {
                OrigemLeitura.CODIGO_BARRAS -> PainelCodigoBarras(
                    capturando = estado.capturando,
                    aoIniciarLeitura = aoIniciarLeitura,
                    aoPararLeitura = aoPararLeitura,
                    previaCamera = previaCamera
                )

                OrigemLeitura.NFC -> PainelNfc(capturando = estado.capturando, aoIniciarLeitura = aoIniciarLeitura)

                OrigemLeitura.RFID_UHF -> PainelUhf(
                    conectado = estado.conectado,
                    statusTexto = estado.statusTexto,
                    aoAlternarConexao = aoAlternarConexao,
                    aoIniciarLeitura = aoIniciarLeitura,
                    aoPararLeitura = aoPararLeitura
                )

                OrigemLeitura.MANUAL -> PainelManual(aoRegistrarManual = aoRegistrarManual)
            }
        }
    }
}

/** REQ-06 — a prévia da câmera vive dentro do painel, com o botão de fechar no canto. */
@Composable
private fun PainelCodigoBarras(
    capturando: Boolean,
    aoIniciarLeitura: () -> Unit,
    aoPararLeitura: () -> Unit,
    previaCamera: (@Composable (Modifier) -> Unit)?
) {
    if (capturando && previaCamera != null) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                previaCamera(Modifier.fillMaxSize())
                FilledTonalIconButton(
                    onClick = aoPararLeitura,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_fechar), contentDescription = "Fechar câmera")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aponte para o código do bem. Aceita Code 128, Code 39 e QR Code.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        PainelParado(
            icone = R.drawable.ic_modo_barras,
            titulo = "Câmera fechada",
            rotuloBotao = "Abrir câmera",
            aoAcionar = aoIniciarLeitura
        )
    }
}

/** REQ-07 — o NFC é passivo: a tela diz onde encostar e mostra que está esperando. */
@Composable
private fun PainelNfc(capturando: Boolean, aoIniciarLeitura: () -> Unit) {
    if (!capturando) {
        PainelParado(
            icone = R.drawable.ic_modo_nfc,
            titulo = "Leitura NFC pausada",
            rotuloBotao = "Retomar leitura",
            aoAcionar = aoIniciarLeitura
        )
        return
    }
    val pulso = rememberInfiniteTransition(label = "pulsoNfc")
    val alfa by pulso.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alfaNfc"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_modo_nfc),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(48.dp)
                .alpha(alfa)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Encoste a etiqueta nas costas do aparelho",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "A leitura é automática. Etiquetas NfcA, NfcB, NfcF e NfcV.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** REQ-08 — a conexão BLE e os comandos do scanner moram só aqui, dentro do modo UHF. */
@Composable
private fun PainelUhf(
    conectado: Boolean,
    statusTexto: String,
    aoAlternarConexao: () -> Unit,
    aoIniciarLeitura: () -> Unit,
    aoPararLeitura: () -> Unit
) {
    val cores = LocalCoresDeEstado.current
    val corStatus = if (conectado) cores.conectado else MaterialTheme.colorScheme.error
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(corStatus)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusTexto,
                style = MaterialTheme.typography.titleMedium,
                color = corStatus
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (conectado) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = aoIniciarLeitura, modifier = Modifier.weight(1f)) {
                    Text("Escanear")
                }
                FilledTonalButton(onClick = aoPararLeitura, modifier = Modifier.weight(1f)) {
                    Text("Parar")
                }
            }
            TextButton(onClick = aoAlternarConexao) { Text("Desconectar scanner") }
        } else {
            Text(
                text = "Ligue o scanner ESP32 e conecte por Bluetooth para ler etiquetas UHF.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = aoAlternarConexao, modifier = Modifier.fillMaxWidth()) {
                Text("Conectar scanner")
            }
        }
    }
}

/** Lançamento manual — para o bem sem etiqueta legível. Entra na lista pela mesma porta. */
@Composable
private fun PainelManual(aoRegistrarManual: (String) -> Boolean) {
    var texto by rememberSaveable { mutableStateOf("") }
    val enviar = {
        if (aoRegistrarManual(texto)) texto = ""
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Código do bem") },
            placeholder = { Text("Ex.: PATR-147258") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { enviar() }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = enviar,
            enabled = texto.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(painterResource(R.drawable.ic_adicionar), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Adicionar à lista")
        }
    }
}

@Composable
private fun PainelParado(
    @DrawableRes icone: Int,
    titulo: String,
    rotuloBotao: String,
    aoAcionar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icone),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Button(onClick = aoAcionar) { Text(rotuloBotao) }
    }
}

/** REQ-05/CE-01 — aviso de "já conferido", em âmbar, separado da cor de destaque. */
@Composable
private fun AvisoDeDuplicata(aviso: String) {
    val cores = LocalCoresDeEstado.current
    Surface(
        shape = FormaMenor,
        color = cores.avisoFundo,
        contentColor = cores.avisoTexto,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = aviso,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

/** REQ-04/RN-06/CE-14 — a lista da sessão, mais recente no topo, com estado vazio explicativo. */
@Composable
private fun SecaoDeLeituras(
    leituras: List<LeituraPatrimonial>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Conferidos na sessão",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (leituras.isEmpty()) {
            Surface(
                shape = FormaMenor,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nenhum item conferido ainda.\nEscolha um modo acima e leia a primeira etiqueta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(leituras) { indice, leitura ->
                    LinhaDeLeitura(leitura = leitura, destaque = indice == 0)
                    if (indice < leituras.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaDeLeitura(leitura: LeituraPatrimonial, destaque: Boolean) {
    val fundo = if (destaque) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaMenor)
            .background(fundo)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconeDaOrigem(leitura.origem)),
            contentDescription = rotuloDaOrigem(leitura.origem),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leitura.chave,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            val detalhe = leitura.descricao?.let { "${rotuloDaOrigem(leitura.origem)} · $it" }
                ?: rotuloDaOrigem(leitura.origem)
            Text(
                text = detalhe,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = horarioDe(leitura.instante),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** O registro técnico continua existindo, mas recolhido: é ferramenta de diagnóstico, não conteúdo. */
@Composable
private fun RegistroTecnico(logs: List<String>) {
    var aberto by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { aberto = !aberto }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Registro técnico",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = logs.size.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(if (aberto) R.drawable.ic_seta_baixo else R.drawable.ic_seta_cima),
                contentDescription = if (aberto) "Recolher registro" else "Abrir registro",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (aberto) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .padding(bottom = 8.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
