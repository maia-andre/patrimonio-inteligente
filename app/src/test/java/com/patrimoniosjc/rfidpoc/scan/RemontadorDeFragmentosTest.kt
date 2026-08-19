package com.patrimoniosjc.rfidpoc.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * REQ-08 — a remontagem de fragmentos pelo marcador `__END__` é preservada
 * fora da MainActivity, como lógica pura. Dados fictícios (RNF-03).
 */
class RemontadorDeFragmentosTest {

    @Test
    fun `acumula fragmentos e devolve o payload completo no marcador de fim`() {
        val remontador = RemontadorDeFragmentos()

        assertNull(remontador.receber("147258;Note"))
        assertNull(remontador.receber("book Positivo"))
        assertEquals("147258;Notebook Positivo", remontador.receber("__END__"))
    }

    @Test
    fun `marcador de fim sem fragmentos acumulados devolve nulo`() {
        val remontador = RemontadorDeFragmentos()

        assertNull(remontador.receber("__END__"))
    }

    @Test
    fun `buffer e limpo apos completar uma mensagem`() {
        val remontador = RemontadorDeFragmentos()

        remontador.receber("primeira")
        assertEquals("primeira", remontador.receber("__END__"))

        remontador.receber("segunda")
        assertEquals("segunda", remontador.receber("__END__"))
    }

    @Test
    fun `fragmento unico seguido do marcador devolve o proprio fragmento`() {
        val remontador = RemontadorDeFragmentos()

        assertNull(remontador.receber("369852;Projetor Epson"))
        assertEquals("369852;Projetor Epson", remontador.receber("__END__"))
    }
}
