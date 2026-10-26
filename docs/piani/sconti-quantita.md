# Piano — Sconti per quantità e spedizione gratuita

Specifica: regole R1–R6 in [../corso/M6-G1.md](../corso/M6-G1.md).

## Decisioni

- `Ordine.totale()` è sostituito da `totaleMerce()`, `speseSpedizione()` e
  `totaleDaPagare()`, così non resta un "totale" ambiguo.
- La percentuale di sconto è **calcolata** dalla quantità della riga e non viene
  salvata: niente nuove colonne e coerenza automatica con l'unione delle righe (R3).
- Gli ordini ANNULLATI mostrano gli importi come prima, senza casi speciali.
- L'arrotondamento `HALF_UP` si applica una volta sola, sul subtotale netto della riga
  (non si arrotonda prima lo sconto): 3,10 − 5% = 2,945 → 2,95, non 2,94.

## Dominio (`model/`)

- `RigaOrdine.percentualeSconto()`: 0 / 5 / 10 per 1–9 / 10–49 / ≥50 pezzi (R1).
- `RigaOrdine.importoLordo()`: prezzo × quantità.
- `RigaOrdine.subtotale()`: lordo × (100 − %) / 100, `setScale(2, HALF_UP)` (R2).
- R3: `Ordine.aggiungiRiga` unisce già lo stesso prodotto in una riga, e lo
  sconto si calcola sulla quantità unita.
- `Ordine.totaleMerce()`: somma dei subtotali scontati (R4).
- `Ordine.speseSpedizione()`: 9,90 € se la merce è < 150,00 €, altrimenti 0,00 €;
  `totaleDaPagare()` = merce + spedizione (R5). Costanti `SPESE_SPEDIZIONE` e
  `SOGLIA_SPEDIZIONE_GRATUITA`.

## Pagine (R6)

- `ordine-dettaglio.html`: colonna "Sconto" (`n %`). In fondo: "Totale merce",
  "Spedizione" ("Gratuita" se 0) e "Totale da pagare".
- `ordini.html`: la colonna "Totale da pagare" mostra `totaleDaPagare()`.

## Test

- `OrdineTest`: confini 9/10 e 49/50; subtotali scontati e arrotondamento
  HALF_UP (2,375 → 2,38; 2,945 → 2,95 e 6,885 → 6,89, che distinguono HALF_UP da
  HALF_EVEN); unione 5 + 5 → 5% e 45 + 5 → 10%; prodotti diversi non si sommano;
  somma della merce; confini 149,99/150,00; soglia confrontata con la merce già scontata.
- `OrdineServiceTest`: 25,00 € di merce + 9,90 € = 34,90 €.
- `PagineTest`: il dettaglio e l'elenco mostrano sconto, spedizione e totale da pagare;
  spedizione "Gratuita" e riga "0 %" (COL-1KG × 30 + GUA-L × 1 → 162,80 €).
- Mutazione manuale `10 → 11` in R1: 6 test diventano rossi.
- Mutazione manuale `HALF_UP → HALF_EVEN` nel subtotale: 2 test diventano rossi.

## Verifica

`.\mvnw.cmd test` → verde.
