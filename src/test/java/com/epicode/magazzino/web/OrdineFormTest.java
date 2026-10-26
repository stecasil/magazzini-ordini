package com.epicode.magazzino.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.epicode.magazzino.service.NuovoOrdine;
import org.junit.jupiter.api.Test;

class OrdineFormTest {

    @Test
    void unFormNuovoHaTreRigheVuote() {
        OrdineForm form = new OrdineForm();
        assertEquals(OrdineForm.RIGHE, form.getRighe().size());
        form.getRighe().forEach(riga -> {
            assertNull(riga.getCodice());
            assertNull(riga.getQuantita());
        });
    }

    @Test
    void leRigheCompilateDiventanoRigheDellaRichiesta() {
        OrdineForm form = new OrdineForm();
        form.setCliente("Ferramenta Bianchi");
        compila(form, 0, "VIT", 4);
        compila(form, 2, "TRA", 1);

        NuovoOrdine richiesta = form.toRichiesta();

        assertEquals("Ferramenta Bianchi", richiesta.cliente());
        assertEquals(2, richiesta.righe().size());
        assertEquals(new NuovoOrdine.Riga("VIT", 4), richiesta.righe().get(0));
        assertEquals(new NuovoOrdine.Riga("TRA", 1), richiesta.righe().get(1));
    }

    @Test
    void leRigheSenzaCodiceOSenzaQuantitaSiIgnorano() {
        OrdineForm form = new OrdineForm();
        compila(form, 0, "  ", 4);
        compila(form, 1, "VIT", null);
        compila(form, 2, null, 3);

        assertTrue(form.toRichiesta().righe().isEmpty());
    }

    @Test
    void unFormVuotoProduceUnaRichiestaSenzaRighe() {
        NuovoOrdine richiesta = new OrdineForm().toRichiesta();
        assertNull(richiesta.cliente());
        assertTrue(richiesta.righe().isEmpty());
    }

    private void compila(OrdineForm form, int indice, String codice, Integer quantita) {
        form.getRighe().get(indice).setCodice(codice);
        form.getRighe().get(indice).setQuantita(quantita);
    }
}
