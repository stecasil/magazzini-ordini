package com.epicode.magazzino.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProdottoTest {

    private Prodotto viti() {
        return new Prodotto("VIT", "Viti", new BigDecimal("6.90"), 10, 3);
    }

    @Test
    void prelevaTogliePezziDallaGiacenza() {
        Prodotto viti = viti();
        viti.preleva(4);
        assertEquals(6, viti.getDisponibili());
    }

    @Test
    void sipuoPrelevareTuttaLaGiacenza() {
        Prodotto viti = viti();
        viti.preleva(10);
        assertEquals(0, viti.getDisponibili());
    }

    @Test
    void unPezzoInPiuDellaGiacenzaVieneRifiutatoSenzaToccareNulla() {
        Prodotto viti = viti();
        assertThrows(ScortaInsufficienteException.class, () -> viti.preleva(11));
        assertEquals(10, viti.getDisponibili());
    }

    @Test
    void quantitaZeroONegativaNonValida() {
        Prodotto viti = viti();
        assertThrows(RichiestaNonValidaException.class, () -> viti.preleva(0));
        assertThrows(RichiestaNonValidaException.class, () -> viti.riassortisci(-1));
    }

    @Test
    void prezzoNegativoRifiutato() {
        assertThrows(RichiestaNonValidaException.class,
                () -> new Prodotto("X", "X", new BigDecimal("-1"), 1, 0));
    }

    @Test
    void riassortireAggiungePezziAllaGiacenza() {
        Prodotto viti = viti();
        viti.riassortisci(5);
        assertEquals(15, viti.getDisponibili());
    }

    @Test
    void riassortireZeroPezziNonValido() {
        Prodotto viti = viti();
        assertThrows(RichiestaNonValidaException.class, () -> viti.riassortisci(0));
        assertEquals(10, viti.getDisponibili());
    }

    @Test
    void prelevareUnaQuantitaNegativaNonValido() {
        Prodotto viti = viti();
        assertThrows(RichiestaNonValidaException.class, () -> viti.preleva(-3));
        assertEquals(10, viti.getDisponibili());
    }

    @Test
    void codiceENomeSonoObbligatori() {
        BigDecimal prezzo = new BigDecimal("1.00");
        assertThrows(RichiestaNonValidaException.class, () -> new Prodotto(null, "X", prezzo, 1, 0));
        assertThrows(RichiestaNonValidaException.class, () -> new Prodotto(" ", "X", prezzo, 1, 0));
        assertThrows(RichiestaNonValidaException.class, () -> new Prodotto("X", null, prezzo, 1, 0));
        assertThrows(RichiestaNonValidaException.class, () -> new Prodotto("X", "", prezzo, 1, 0));
    }

    @Test
    void prezzoNulloGiacenzaEScortaMinimaNegativeRifiutati() {
        assertThrows(RichiestaNonValidaException.class, () -> new Prodotto("X", "X", null, 1, 0));
        assertThrows(RichiestaNonValidaException.class,
                () -> new Prodotto("X", "X", new BigDecimal("1.00"), -1, 0));
        assertThrows(RichiestaNonValidaException.class,
                () -> new Prodotto("X", "X", new BigDecimal("1.00"), 1, -1));
    }

    @Test
    void unProdottoGratuitoEAmmesso() {
        Prodotto omaggio = new Prodotto("OMA", "Omaggio", new BigDecimal("0.00"), 1, 0);
        assertEquals(new BigDecimal("0.00"), omaggio.getPrezzo());
    }

    @Test
    void ilMessaggioDiScortaInsufficienteDiceCodiceRichiestiEDisponibili() {
        Prodotto viti = viti();
        ScortaInsufficienteException e = assertThrows(ScortaInsufficienteException.class,
                () -> viti.preleva(12));
        assertEquals("Scorta insufficiente per VIT: richiesti 12, disponibili 10", e.getMessage());
    }
}
