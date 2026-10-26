package com.epicode.magazzino.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrdineTest {

    private static final LocalDateTime ADESSO = LocalDateTime.of(2026, 10, 26, 9, 0);

    private Prodotto viti;
    private Prodotto trapano;
    private Prodotto guanti;

    @BeforeEach
    void catalogo() {
        viti = new Prodotto("VIT", "Viti", new BigDecimal("6.90"), 100, 10);
        trapano = new Prodotto("TRA", "Trapano", new BigDecimal("89.00"), 5, 1);
        guanti = new Prodotto("GUA", "Guanti", new BigDecimal("3.20"), 100, 10);
    }

    @Test
    void ilTotaleMerceESommaDeiSubtotali() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(viti, 3);      // 20,70
        ordine.aggiungiRiga(trapano, 2);   // 178,00
        assertEquals(new BigDecimal("198.70"), ordine.totaleMerce());
    }

    // R1 — sconto per quantità sulla riga

    @Test
    void novePezziNonHannoSconto() {
        assertEquals(0, rigaDi(guanti, 9).percentualeSconto());
    }

    @Test
    void dieciPezziHannoIlCinquePerCento() {
        assertEquals(5, rigaDi(guanti, 10).percentualeSconto());
    }

    @Test
    void quarantanovePezziHannoAncoraIlCinquePerCento() {
        assertEquals(5, rigaDi(guanti, 49).percentualeSconto());
    }

    @Test
    void cinquantaPezziHannoIlDieciPerCento() {
        assertEquals(10, rigaDi(guanti, 50).percentualeSconto());
    }

    // R2 — subtotale scontato e arrotondato HALF_UP

    @Test
    void ilSubtotaleSenzaScontoEPrezzoPerQuantita() {
        assertEquals(new BigDecimal("28.80"), rigaDi(guanti, 9).subtotale());
    }

    @Test
    void ilSubtotaleTogliLoScontoDalLordo() {
        RigaOrdine riga = rigaDi(viti, 10);              // 69,00 − 5%
        assertEquals(new BigDecimal("69.00"), riga.importoLordo());
        assertEquals(new BigDecimal("65.55"), riga.subtotale());
    }

    @Test
    void ilSubtotaleConDieciPerCento() {
        assertEquals(new BigDecimal("144.00"), rigaDi(guanti, 50).subtotale()); // 160,00 − 10%
    }

    @Test
    void ilSubtotaleSiArrotondaHalfUpAlCentesimo() {
        Prodotto rondella = new Prodotto("RON", "Rondella", new BigDecimal("0.25"), 100, 10);
        assertEquals(new BigDecimal("2.38"), rigaDi(rondella, 10).subtotale()); // 2,375 → 2,38
    }

    // R3 — conta la quantità totale della riga

    @Test
    void cinquePiuCinqueStessoProdottoPrendeIlCinquePerCento() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(viti, 5);
        ordine.aggiungiRiga(viti, 5);
        RigaOrdine riga = ordine.getRighe().get(0);
        assertEquals(5, riga.percentualeSconto());
        assertEquals(new BigDecimal("65.55"), riga.subtotale());
    }

    // R4 — totale merce = somma dei subtotali scontati

    @Test
    void ilTotaleMerceUsaISubtotaliScontati() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(viti, 10);     // 65,55
        ordine.aggiungiRiga(trapano, 1);   // 89,00
        assertEquals(new BigDecimal("154.55"), ordine.totaleMerce());
    }

    // R5 — spedizione e totale da pagare

    @Test
    void sottoCentocinquantaEuroLaSpedizioneCosta990() {
        Ordine ordine = ordineDaUnPezzoA("149.99");
        assertEquals(new BigDecimal("9.90"), ordine.speseSpedizione());
        assertEquals(new BigDecimal("159.89"), ordine.totaleDaPagare());
    }

    @Test
    void daCentocinquantaEuroLaSpedizioneEGratuita() {
        Ordine ordine = ordineDaUnPezzoA("150.00");
        assertEquals(new BigDecimal("0.00"), ordine.speseSpedizione());
        assertEquals(new BigDecimal("150.00"), ordine.totaleDaPagare());
    }

    @Test
    void laSogliaDiSpedizioneSiConfrontaConLaMerceScontata() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        Prodotto cassetta = new Prodotto("CAS", "Cassetta", new BigDecimal("15.00"), 20, 1);
        ordine.aggiungiRiga(cassetta, 10);   // lordo 150,00 → scontato 142,50
        assertEquals(new BigDecimal("142.50"), ordine.totaleMerce());
        assertEquals(new BigDecimal("9.90"), ordine.speseSpedizione());
        assertEquals(new BigDecimal("152.40"), ordine.totaleDaPagare());
    }

    private RigaOrdine rigaDi(Prodotto prodotto, int quantita) {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(prodotto, quantita);
        return ordine.getRighe().get(0);
    }

    private Ordine ordineDaUnPezzoA(String prezzo) {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(new Prodotto("UNO", "Pezzo unico", new BigDecimal(prezzo), 1, 0), 1);
        return ordine;
    }

    @Test
    void ogniRigaPrelevaDalMagazzino() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(trapano, 2);
        assertEquals(3, trapano.getDisponibili());
    }

    @Test
    void loStessoProdottoDueVolteFiniscenellaStessaRiga() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(viti, 3);
        ordine.aggiungiRiga(viti, 2);
        assertEquals(1, ordine.getRighe().size());
        assertEquals(5, ordine.getRighe().get(0).getQuantita());
    }

    @Test
    void ilPrezzoDellaRigaRestaQuelloDelMomentoDellOrdine() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(viti, 1);
        assertEquals(new BigDecimal("6.90"), ordine.getRighe().get(0).getPrezzoUnitario());
    }

    @Test
    void annullareRestituisceIPezzi() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(trapano, 2);
        ordine.annulla();
        assertEquals(StatoOrdine.ANNULLATO, ordine.getStato());
        assertEquals(5, trapano.getDisponibili());
    }

    @Test
    void unOrdineSpeditoNonSiAnnulla() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(trapano, 1);
        ordine.spedisci();
        assertThrows(OperazioneNonConsentitaException.class, ordine::annulla);
        assertEquals(4, trapano.getDisponibili());
    }

    @Test
    void unOrdineAnnullatoNonSiAnnullaDueVolte() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(trapano, 1);
        ordine.annulla();
        assertThrows(OperazioneNonConsentitaException.class, ordine::annulla);
        assertEquals(5, trapano.getDisponibili());
    }

    @Test
    void nonSiAggiungonoRigheAUnOrdineSpedito() {
        Ordine ordine = new Ordine("Rossi", ADESSO);
        ordine.aggiungiRiga(viti, 1);
        ordine.spedisci();
        assertThrows(OperazioneNonConsentitaException.class, () -> ordine.aggiungiRiga(viti, 1));
    }

    @Test
    void clienteObbligatorio() {
        assertThrows(RichiestaNonValidaException.class, () -> new Ordine("  ", ADESSO));
    }
}
