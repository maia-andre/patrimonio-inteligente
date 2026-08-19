package com.patrimoniosjc.rfidpoc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrimoniosjc.rfidpoc.domain.LeituraPatrimonial
import com.patrimoniosjc.rfidpoc.domain.OrigemLeitura
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Rótulo legível de cada origem de leitura. */
fun rotuloDaOrigem(origem: OrigemLeitura): String = when (origem) {
    OrigemLeitura.CODIGO_BARRAS -> "Código de barras"
    OrigemLeitura.NFC -> "NFC"
    OrigemLeitura.RFID_UHF -> "RFID UHF"
}

private fun horarioDe(instante: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(instante))

@Composable
fun TelaScanner(
    estado: EstadoTelaScanner,
    aoAlternarConexao: () -> Unit,
    aoIniciarLeitura: () -> Unit,
    aoPararLeitura: () -> Unit,
    aoSelecionarModo: (OrigemLeitura) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Scanner Patrimonial",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Status: ${estado.statusTexto}",
            style = MaterialTheme.typography.titleMedium,
            color = if (estado.conectado) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
        Spacer(modifier = Modifier.height(12.dp))

        SeletorDeModos(
            modos = estado.modos,
            modoSelecionado = estado.modoSelecionado,
            aoSelecionarModo = aoSelecionarModo
        )
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = aoAlternarConexao,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (estado.conectado) Color(0xFFD32F2F) else Color(0xFF1976D2)
            )
        ) {
            Text(if (estado.conectado) "Desligar Scanner" else "Ligar Scanner")
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = aoIniciarLeitura,
                enabled = estado.conectado,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
            ) {
                Text("Escanear")
            }
            Button(
                onClick = aoPararLeitura,
                enabled = estado.conectado,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161))
            ) {
                Text("Desliga Scanner")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        estado.avisoJaConferido?.let { aviso ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Text(
                    text = aviso,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE65100)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = "Conferidos na sessão: ${estado.leituras.size}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (estado.leituras.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Text(
                    text = "Nenhum item conferido ainda.\nConecte o scanner e leia uma etiqueta para começar.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF616161)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(estado.leituras) { leitura ->
                    LinhaDeLeitura(leitura)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Logs:",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (estado.leituras.isEmpty()) 1f else 0.6f)
        ) {
            items(estado.logs) { log ->
                Text(text = log, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

/** REQ-03/REQ-11 — seletor com os três modos; indisponível aparece desabilitado com o motivo. */
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
            val selecionado = modo.origem == modoSelecionado
            Column(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { aoSelecionarModo(modo.origem) },
                    enabled = modo.disponivel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selecionado) Color(0xFF1976D2) else Color(0xFF90A4AE)
                    )
                ) {
                    Text(
                        text = rotuloDaOrigem(modo.origem),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                modo.motivo?.let { motivo ->
                    Text(
                        text = motivo,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF757575)
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDeLeitura(leitura: LeituraPatrimonial) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leitura.chave,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = rotuloDaOrigem(leitura.origem),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF757575)
            )
        }
        Text(
            text = horarioDe(leitura.instante),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF757575)
        )
    }
}
