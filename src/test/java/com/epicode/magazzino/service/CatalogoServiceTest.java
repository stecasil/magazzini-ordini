package com.epicode.magazzino.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epicode.magazzino.model.Prodotto;
import com.epicode.magazzino.model.RichiestaNonValidaException;
import com.epicode.magazzino.model.RisorsaNonTrovataException;
import com.epicode.magazzino.repository.ProdottoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Test di integrazione del catalogo: service, repository e database H2 veri. */
@SpringBootTest
@Transactional
class CatalogoServiceTest {

    @Autowired
    private CatalogoService catalogo;

    @Autowired
    private ProdottoRepository prodotti;

    @BeforeEach
    void prodottiDiTest() {
        prodotti.save(new Prodotto("T-VITI", "Viti di test", new BigDecimal("2.50"), 20, 5));
        prodotti.save(new Prodotto("T-ATTR", "Attrezzo di test", new BigDecimal("15.00"), 3, 1));
    }

    @Test
    void lElencoEOrdinatoPerNomeEComprendeIProdottiDiTest() {
        List<String> nomi = catalogo.elenco().stream().map(Prodotto::getNome).toList();
        assertTrue(nomi.contains("Viti di test"));
        assertTrue(nomi.contains("Attrezzo di test"));
        assertEquals(nomi.stream().sorted().toList(), nomi);
    }

    @Test
    void trovaUnProdottoPerCodice() {
        assertEquals("Viti di test", catalogo.trova("T-VITI").getNome());
    }

    @Test
    void unProdottoInesistenteNonSiTrova() {
        RisorsaNonTrovataException e = assertThrows(RisorsaNonTrovataException.class,
                () -> catalogo.trova("T-NESSUNO"));
        assertEquals("Prodotto T-NESSUNO non trovato", e.getMessage());
    }

    @Test
    void riassortireAumentaLaGiacenzaSalvata() {
        catalogo.riassortisci("T-ATTR", 7);
        assertEquals(10, prodotti.findByCodice("T-ATTR").orElseThrow().getDisponibili());
    }

    @Test
    void riassortireZeroPezziNonValido() {
        assertThrows(RichiestaNonValidaException.class, () -> catalogo.riassortisci("T-ATTR", 0));
        assertEquals(3, prodotti.findByCodice("T-ATTR").orElseThrow().getDisponibili());
    }

    @Test
    void riassortireUnProdottoInesistente() {
        assertThrows(RisorsaNonTrovataException.class, () -> catalogo.riassortisci("T-NESSUNO", 1));
    }
}
