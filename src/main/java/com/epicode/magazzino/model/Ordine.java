package com.epicode.magazzino.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un ordine di un cliente. Le regole di business stanno qui, non nel controller:
 * - si aggiungono righe solo a un ordine CONFERMATO, e ogni riga preleva dalla giacenza;
 * - lo stesso prodotto aggiunto due volte finisce nella stessa riga;
 * - annullare restituisce i pezzi al magazzino; un ordine spedito non si annulla;
 * - spedizione 9,90 € sotto i 150,00 € di merce scontata, gratuita da 150,00 € in su.
 */
@Entity
@Table(name = "ordini")
public class Ordine {

    public static final BigDecimal SPESE_SPEDIZIONE = new BigDecimal("9.90");
    public static final BigDecimal SOGLIA_SPEDIZIONE_GRATUITA = new BigDecimal("150.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cliente;

    @Column(nullable = false)
    private LocalDateTime creatoIl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoOrdine stato;

    @OneToMany(mappedBy = "ordine", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<RigaOrdine> righe = new ArrayList<>();

    protected Ordine() {
        // per JPA
    }

    public Ordine(String cliente, LocalDateTime creatoIl) {
        if (cliente == null || cliente.isBlank()) {
            throw new RichiestaNonValidaException("Il cliente è obbligatorio");
        }
        this.cliente = cliente.strip();
        this.creatoIl = creatoIl;
        this.stato = StatoOrdine.CONFERMATO;
    }

    public void aggiungiRiga(Prodotto prodotto, int quantita) {
        if (stato != StatoOrdine.CONFERMATO) {
            throw new OperazioneNonConsentitaException("Non si aggiungono righe a un ordine " + stato);
        }
        prodotto.preleva(quantita);
        for (RigaOrdine riga : righe) {
            if (riga.getProdotto().getCodice().equals(prodotto.getCodice())) {
                riga.aumenta(quantita);
                return;
            }
        }
        righe.add(new RigaOrdine(this, prodotto, quantita));
    }

    /** Somma dei subtotali scontati delle righe. */
    public BigDecimal totaleMerce() {
        BigDecimal totale = BigDecimal.ZERO.setScale(2);
        for (RigaOrdine riga : righe) {
            totale = totale.add(riga.subtotale());
        }
        return totale;
    }

    /** 9,90 € sotto i 150,00 € di merce; gratuita da 150,00 € in su. */
    public BigDecimal speseSpedizione() {
        if (totaleMerce().compareTo(SOGLIA_SPEDIZIONE_GRATUITA) >= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return SPESE_SPEDIZIONE;
    }

    public BigDecimal totaleDaPagare() {
        return totaleMerce().add(speseSpedizione());
    }

    public void spedisci() {
        if (stato != StatoOrdine.CONFERMATO) {
            throw new OperazioneNonConsentitaException("Si spedisce solo un ordine confermato");
        }
        stato = StatoOrdine.SPEDITO;
    }

    public void annulla() {
        if (stato == StatoOrdine.SPEDITO) {
            throw new OperazioneNonConsentitaException("Un ordine spedito non si può annullare");
        }
        if (stato == StatoOrdine.ANNULLATO) {
            throw new OperazioneNonConsentitaException("L'ordine è già annullato");
        }
        for (RigaOrdine riga : righe) {
            riga.getProdotto().riassortisci(riga.getQuantita());
        }
        stato = StatoOrdine.ANNULLATO;
    }

    public Long getId() {
        return id;
    }

    public String getCliente() {
        return cliente;
    }

    public LocalDateTime getCreatoIl() {
        return creatoIl;
    }

    public StatoOrdine getStato() {
        return stato;
    }

    public List<RigaOrdine> getRighe() {
        return Collections.unmodifiableList(righe);
    }
}
