package com.epicode.magazzino.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epicode.magazzino.model.Ordine;
import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.model.RigaOrdine;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Query derivate e mapping JPA, su H2 in memoria (slice JPA, senza dati iniziali). */
@DataJpaTest
class RepositoryTest {

    private static final LocalDateTime IERI = LocalDateTime.of(2026, 10, 25, 9, 0);
    private static final LocalDateTime OGGI = LocalDateTime.of(2026, 10, 26, 9, 0);

    @Autowired
    private ProdottoRepository prodotti;

    @Autowired
    private OrdineRepository ordini;

    @Autowired
    private EntityManager em;

    private Prodotto viti;

    @BeforeEach
    void prodottiDiTest() {
        viti = prodotti.save(new Prodotto("T-VITI", "Viti di test", new BigDecimal("2.50"), 20, 5));
        prodotti.save(new Prodotto("T-ATTR", "Attrezzo di test", new BigDecimal("15.00"), 3, 1));
    }

    @Test
    void trovaUnProdottoPerCodice() {
        Prodotto trovato = prodotti.findByCodice("T-VITI").orElseThrow();
        assertEquals("Viti di test", trovato.getNome());
    }

    @Test
    void unCodiceSconosciutoNonTrovaNulla() {
        assertTrue(prodotti.findByCodice("T-NESSUNO").isEmpty());
    }

    @Test
    void iProdottiSonoOrdinatiPerNome() {
        List<String> nomi = prodotti.findAllByOrderByNomeAsc().stream().map(Prodotto::getNome).toList();
        assertTrue(nomi.indexOf("Attrezzo di test") < nomi.indexOf("Viti di test"));
        assertEquals(nomi.stream().sorted().toList(), nomi);
    }

    @Test
    void gliOrdiniSonoElencatiDalPiuRecente() {
        Ordine vecchio = ordini.save(new Ordine("Vecchio", IERI));
        Ordine nuovo = ordini.save(new Ordine("Nuovo", OGGI));

        List<Ordine> elenco = ordini.findAllByOrderByCreatoIlDesc();

        assertTrue(elenco.indexOf(nuovo) < elenco.indexOf(vecchio));
    }

    @Test
    void salvareUnOrdineSalvaAncheLeRighe() {
        Ordine ordine = new Ordine("Rossi", OGGI);
        ordine.aggiungiRiga(viti, 4);
        Long id = ordini.save(ordine).getId();
        em.flush();
        em.clear();

        Ordine riletto = ordini.findById(id).orElseThrow();

        assertEquals(1, riletto.getRighe().size());
        RigaOrdine riga = riletto.getRighe().get(0);
        assertEquals(4, riga.getQuantita());
        assertEquals(new BigDecimal("2.50"), riga.getPrezzoUnitario());
        assertEquals("T-VITI", riga.getProdotto().getCodice());
        assertEquals(16, prodotti.findByCodice("T-VITI").orElseThrow().getDisponibili());
    }
}
