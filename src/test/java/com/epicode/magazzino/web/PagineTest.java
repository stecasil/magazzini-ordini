package com.epicode.magazzino.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Le pagine principali si aprono e il form crea davvero un ordine. */
@SpringBootTest
@AutoConfigureMockMvc
class PagineTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void elencoProdotti() throws Exception {
        mvc.perform(get("/prodotti"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trapano a batteria 18V")));
    }

    @Test
    void ilFormCreaUnOrdineEPortaAlDettaglio() throws Exception {
        mvc.perform(post("/ordini")
                        .param("cliente", "Ferramenta Bianchi")
                        .param("righe[0].codice", "GUA-L")
                        .param("righe[0].quantita", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/ordini/*"));
    }

    @Test
    void ilFormMostraLErroreSeLaScortaNonBasta() throws Exception {
        mvc.perform(post("/ordini")
                        .param("cliente", "Ferramenta Bianchi")
                        .param("righe[0].codice", "NAS-50")
                        .param("righe[0].quantita", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Scorta insufficiente per NAS-50")));
    }

    @Test
    void ilDettaglioMostraScontoSpedizioneETotaleDaPagare() throws Exception {
        // 10 x 3,20 € = 32,00 € meno 5% = 30,40 €; sotto 150 € → spedizione 9,90 € → 40,30 €
        String dettaglio = mvc.perform(post("/ordini")
                        .param("cliente", "Ferramenta Verdi")
                        .param("righe[0].codice", "GUA-L")
                        .param("righe[0].quantita", "10"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();

        mvc.perform(get(dettaglio))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("5 %")))
                .andExpect(content().string(containsString("Totale merce")))
                .andExpect(content().string(containsString("30,40 €")))
                .andExpect(content().string(containsString("Spedizione")))
                .andExpect(content().string(containsString("9,90 €")))
                .andExpect(content().string(containsString("Totale da pagare")))
                .andExpect(content().string(containsString("40,30 €")));

        mvc.perform(get("/ordini"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Totale da pagare")))
                .andExpect(content().string(containsString("40,30 €")));
    }

    @Test
    void ilDettaglioMostraSpedizioneGratuitaEScontoZero() throws Exception {
        // 30 x 5,60 € = 168,00 € − 5% = 159,60 €; 1 x 3,20 € (0 %) → merce 162,80 €, spedizione gratuita
        String dettaglio = mvc.perform(post("/ordini")
                        .param("cliente", "Ferramenta Gialli")
                        .param("righe[0].codice", "COL-1KG")
                        .param("righe[0].quantita", "30")
                        .param("righe[1].codice", "GUA-L")
                        .param("righe[1].quantita", "1"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();

        mvc.perform(get(dettaglio))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0 %")))
                .andExpect(content().string(containsString("159,60 €")))
                .andExpect(content().string(containsString("Gratuita")))
                .andExpect(content().string(containsString("162,80 €")));
    }

    @Test
    void ordineInesistente() throws Exception {
        mvc.perform(get("/ordini/9999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errore"))
                .andExpect(content().string(containsString("Ordine 9999 non trovato")));
    }

    @Test
    void laHomePortaAiProdotti() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prodotti"));
    }

    @Test
    void elencoOrdiniMostraLOrdineCreato() throws Exception {
        String dettaglio = creaOrdine("Ferramenta Elenco", "MET-5", 1);
        String id = dettaglio.substring(dettaglio.lastIndexOf('/') + 1);

        mvc.perform(get("/ordini"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordini"))
                .andExpect(content().string(containsString("Ferramenta Elenco")))
                .andExpect(content().string(containsString("/ordini/" + id)));
    }

    @Test
    void ilFormNuovoOrdineElencaIProdotti() throws Exception {
        mvc.perform(get("/ordini/nuovo"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordine-nuovo"))
                .andExpect(model().attributeExists("form", "prodotti"))
                .andExpect(content().string(containsString("Nuovo ordine")))
                .andExpect(content().string(containsString("Guanti da lavoro taglia L")));
    }

    @Test
    void ilFormSenzaRigheMostraLErrore() throws Exception {
        mvc.perform(post("/ordini").param("cliente", "Ferramenta Bianchi"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordine-nuovo"))
                .andExpect(model().attributeExists("prodotti"))
                .andExpect(content().string(containsString("Un ordine deve avere almeno una riga")));
    }

    @Test
    void ilFormConUnProdottoInesistenteMostraLErroreSenza404() throws Exception {
        mvc.perform(post("/ordini")
                        .param("cliente", "Ferramenta Bianchi")
                        .param("righe[0].codice", "NON-ESISTE")
                        .param("righe[0].quantita", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordine-nuovo"))
                .andExpect(content().string(containsString("Prodotto NON-ESISTE non trovato")));
    }

    @Test
    void spedireUnOrdineMostraIlMessaggio() throws Exception {
        String dettaglio = creaOrdine("Ferramenta Spedizioni", "COL-1KG", 1);

        mvc.perform(post(dettaglio + "/spedisci"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(dettaglio))
                .andExpect(flash().attribute("messaggio", "Ordine spedito"));

        mvc.perform(get(dettaglio))
                .andExpect(content().string(containsString("SPEDITO")));
    }

    @Test
    void spedireDueVolteMostraLErrore() throws Exception {
        String dettaglio = creaOrdine("Ferramenta Spedizioni", "COL-1KG", 1);
        mvc.perform(post(dettaglio + "/spedisci"));

        mvc.perform(post(dettaglio + "/spedisci"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(dettaglio))
                .andExpect(flash().attribute("errore", "Si spedisce solo un ordine confermato"));
    }

    @Test
    void annullareUnOrdineMostraIlMessaggio() throws Exception {
        String dettaglio = creaOrdine("Ferramenta Annulli", "GUA-L", 1);

        mvc.perform(post(dettaglio + "/annulla"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(dettaglio))
                .andExpect(flash().attribute("messaggio",
                        "Ordine annullato: i pezzi sono tornati in magazzino"));

        mvc.perform(get(dettaglio))
                .andExpect(content().string(containsString("ANNULLATO")));
    }

    @Test
    void annullareUnOrdineSpeditoMostraLErrore() throws Exception {
        String dettaglio = creaOrdine("Ferramenta Annulli", "GUA-L", 1);
        mvc.perform(post(dettaglio + "/spedisci"));

        mvc.perform(post(dettaglio + "/annulla"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errore", "Un ordine spedito non si può annullare"));
    }

    @Test
    void spedireUnOrdineInesistenteTornaConLErrore() throws Exception {
        // Il controller intercetta ogni MagazzinoException sulle POST: niente 404, solo flash.
        mvc.perform(post("/ordini/9999/spedisci"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ordini/9999"))
                .andExpect(flash().attribute("errore", "Ordine 9999 non trovato"));
    }

    private String creaOrdine(String cliente, String codice, int quantita) throws Exception {
        return mvc.perform(post("/ordini")
                        .param("cliente", cliente)
                        .param("righe[0].codice", codice)
                        .param("righe[0].quantita", String.valueOf(quantita)))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
    }
}
