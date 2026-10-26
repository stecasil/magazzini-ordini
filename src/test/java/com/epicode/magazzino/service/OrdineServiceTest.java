package com.epicode.magazzino.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epicode.magazzino.model.OperazioneNonConsentitaException;
import com.epicode.magazzino.model.Ordine;
import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.model.RichiestaNonValidaException;
import com.epicode.magazzino.model.RisorsaNonTrovataException;
import com.epicode.magazzino.model.ScortaInsufficienteException;
import com.epicode.magazzino.model.StatoOrdine;
import com.epicode.magazzino.repository.ProdottoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

/** Test di integrazione: service, repository e database H2 veri; l'ora arriva da un Clock fisso. */
@SpringBootTest
@Transactional
class OrdineServiceTest {

    private static final LocalDateTime ADESSO = LocalDateTime.of(2026, 10, 26, 9, 30);

    @Autowired
    private OrdineService ordini;

    @Autowired
    private ProdottoRepository prodotti;

    @BeforeEach
    void prodottiDiTest() {
        prodotti.save(new Prodotto("T-VITI", "Viti di test", new BigDecimal("2.50"), 20, 5));
        prodotti.save(new Prodotto("T-MART", "Martello di test", new BigDecimal("15.00"), 3, 1));
    }

    @Test
    void creaUnOrdineEScalaLeScorte() {
        Ordine ordine = ordini.crea(new NuovoOrdine("Ferramenta Bianchi", List.of(
                new NuovoOrdine.Riga("T-VITI", 4),
                new NuovoOrdine.Riga("T-MART", 1))));

        assertNotNull(ordine.getId());
        // 25,00 € di merce: sotto 150,00 € si pagano 9,90 € di spedizione
        assertEquals(new BigDecimal("25.00"), ordine.totaleMerce());
        assertEquals(new BigDecimal("34.90"), ordine.totaleDaPagare());
        assertEquals(16, prodotti.findByCodice("T-VITI").orElseThrow().getDisponibili());
        assertEquals(2, prodotti.findByCodice("T-MART").orElseThrow().getDisponibili());
    }

    @Test
    void prodottoInesistente() {
        NuovoOrdine richiesta = new NuovoOrdine("Bianchi", List.of(new NuovoOrdine.Riga("NON-ESISTE", 1)));
        assertThrows(RisorsaNonTrovataException.class, () -> ordini.crea(richiesta));
    }

    @Test
    void scortaInsufficiente() {
        NuovoOrdine richiesta = new NuovoOrdine("Bianchi", List.of(new NuovoOrdine.Riga("T-MART", 4)));
        assertThrows(ScortaInsufficienteException.class, () -> ordini.crea(richiesta));
    }

    @Test
    void ordineSenzaRighe() {
        NuovoOrdine richiesta = new NuovoOrdine("Bianchi", List.of());
        assertThrows(RichiestaNonValidaException.class, () -> ordini.crea(richiesta));
    }

    @Test
    void laDataDiCreazioneArrivaDalClock() {
        Ordine ordine = ordini.crea(new NuovoOrdine("Bianchi", List.of(new NuovoOrdine.Riga("T-VITI", 1))));
        assertEquals(ADESSO, ordine.getCreatoIl());
    }

    @Test
    void righeDelloStessoProdottoSiUnisconoEPrendonoLoSconto() {
        Ordine ordine = ordini.crea(new NuovoOrdine("Bianchi", List.of(
                new NuovoOrdine.Riga("T-VITI", 5),
                new NuovoOrdine.Riga("T-VITI", 5))));

        assertEquals(1, ordine.getRighe().size());
        assertEquals(5, ordine.getRighe().get(0).percentualeSconto());
        assertEquals(new BigDecimal("23.75"), ordine.totaleMerce());   // 25,00 − 5%
        assertEquals(10, prodotti.findByCodice("T-VITI").orElseThrow().getDisponibili());
    }

    @Test
    void seUnaRigaFallisceLeAltreNonPrelevanoNulla() {
        // Chiude la transazione del test: così crea() ha la sua e il rollback è reale.
        TestTransaction.flagForRollback();
        TestTransaction.end();
        int guantiPrima = prodotti.findByCodice("GUA-L").orElseThrow().getDisponibili();
        NuovoOrdine richiesta = new NuovoOrdine("Bianchi", List.of(
                new NuovoOrdine.Riga("GUA-L", 1),
                new NuovoOrdine.Riga("TRA-18V", 9_999)));

        assertThrows(ScortaInsufficienteException.class, () -> ordini.crea(richiesta));
        assertEquals(guantiPrima, prodotti.findByCodice("GUA-L").orElseThrow().getDisponibili());
    }

    @Test
    void clienteVuotoNonValido() {
        NuovoOrdine richiesta = new NuovoOrdine(" ", List.of(new NuovoOrdine.Riga("T-VITI", 1)));
        assertThrows(RichiestaNonValidaException.class, () -> ordini.crea(richiesta));
        assertEquals(20, prodotti.findByCodice("T-VITI").orElseThrow().getDisponibili());
    }

    @Test
    void trovaUnOrdineCreato() {
        Long id = unOrdine().getId();
        Ordine trovato = ordini.trova(id);
        assertEquals("Ferramenta Bianchi", trovato.getCliente());
        assertEquals(1, trovato.getRighe().size());
    }

    @Test
    void unOrdineInesistenteNonSiTrova() {
        RisorsaNonTrovataException e = assertThrows(RisorsaNonTrovataException.class,
                () -> ordini.trova(999_999L));
        assertEquals("Ordine 999999 non trovato", e.getMessage());
    }

    @Test
    void lElencoComprendeGliOrdiniCreati() {
        Ordine primo = unOrdine();
        Ordine secondo = unOrdine();
        List<Ordine> elenco = ordini.elenco();
        assertTrue(elenco.contains(primo));
        assertTrue(elenco.contains(secondo));
    }

    @Test
    void spedireUnOrdineConfermato() {
        Long id = unOrdine().getId();
        ordini.spedisci(id);
        assertEquals(StatoOrdine.SPEDITO, ordini.trova(id).getStato());
    }

    @Test
    void spedireUnOrdineInesistente() {
        assertThrows(RisorsaNonTrovataException.class, () -> ordini.spedisci(999_999L));
    }

    @Test
    void annullareRestituisceLeScorte() {
        Long id = unOrdine().getId();
        assertEquals(18, prodotti.findByCodice("T-VITI").orElseThrow().getDisponibili());

        ordini.annulla(id);

        assertEquals(StatoOrdine.ANNULLATO, ordini.trova(id).getStato());
        assertEquals(20, prodotti.findByCodice("T-VITI").orElseThrow().getDisponibili());
    }

    @Test
    void unOrdineSpeditoNonSiAnnulla() {
        Long id = unOrdine().getId();
        ordini.spedisci(id);
        assertThrows(OperazioneNonConsentitaException.class, () -> ordini.annulla(id));
        assertEquals(18, prodotti.findByCodice("T-VITI").orElseThrow().getDisponibili());
    }

    @Test
    void annullareUnOrdineInesistente() {
        assertThrows(RisorsaNonTrovataException.class, () -> ordini.annulla(999_999L));
    }

    private Ordine unOrdine() {
        return ordini.crea(new NuovoOrdine("Ferramenta Bianchi", List.of(new NuovoOrdine.Riga("T-VITI", 2))));
    }

    @TestConfiguration
    static class OrologioFisso {

        @Bean
        @Primary
        Clock orologioFisso() {
            return Clock.fixed(ADESSO.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        }
    }
}
