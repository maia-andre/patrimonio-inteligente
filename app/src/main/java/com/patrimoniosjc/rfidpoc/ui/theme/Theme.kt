package com.patrimoniosjc.rfidpoc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = AzulPlaca,
    onPrimary = Color.White,
    primaryContainer = AzulPlacaContainer,
    onPrimaryContainer = AzulPlacaEscuro,
    secondary = Ardosia,
    onSecondary = Color.White,
    secondaryContainer = ArdosiaContainer,
    onSecondaryContainer = ArdosiaEscura,
    background = PapelFrio,
    onBackground = TintaFria,
    surface = PapelFrio,
    onSurface = TintaFria,
    surfaceVariant = SuperficieVarianteClara,
    onSurfaceVariant = TintaSecundariaClara,
    outline = ContornoClaro
)

private val EsquemaEscuro = darkColorScheme(
    primary = AzulPlacaNoturno,
    onPrimary = AzulPlacaEscuro,
    primaryContainer = AzulPlacaContainerNoturno,
    onPrimaryContainer = AzulPlacaContainer,
    secondary = ArdosiaNoturna,
    onSecondary = ArdosiaEscura,
    secondaryContainer = ArdosiaContainerNoturna,
    onSecondaryContainer = ArdosiaContainer,
    background = FundoNoturno,
    onBackground = TintaNoturna,
    surface = FundoNoturno,
    onSurface = TintaNoturna,
    surfaceVariant = SuperficieVarianteNoturna,
    onSurfaceVariant = TintaSecundariaNoturna,
    outline = ContornoNoturno
)

/** Cores de estado (conectado, aviso de duplicata). Não são a cor de destaque. */
data class CoresDeEstado(
    val conectado: Color,
    val avisoFundo: Color,
    val avisoTexto: Color
)

private val CoresDeEstadoClaras = CoresDeEstado(
    conectado = VerdeConectado,
    avisoFundo = AmbarAvisoFundo,
    avisoTexto = AmbarAvisoTexto
)

private val CoresDeEstadoNoturnas = CoresDeEstado(
    conectado = VerdeConectadoNoturno,
    avisoFundo = AmbarAvisoFundoNoturno,
    avisoTexto = AmbarAvisoTextoNoturno
)

val LocalCoresDeEstado = staticCompositionLocalOf { CoresDeEstadoClaras }

/**
 * Tema do aplicativo. Sem cor dinâmica de propósito: a identidade visual
 * não muda com o papel de parede do aparelho.
 */
@Composable
fun RfidpocTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalCoresDeEstado provides if (darkTheme) CoresDeEstadoNoturnas else CoresDeEstadoClaras
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) EsquemaEscuro else EsquemaClaro,
            typography = Typography,
            content = content
        )
    }
}
