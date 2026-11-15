# ScopaBriscola Android — Design Document

**Data:** 2026-11-15  
**Autore:** fraccetta  
**Stato:** APPROVATO

---

## 1. Intento e Perché

App Android per giocare a Scopa e Briscola in due, via Bluetooth, con carte siciliane. I due telefoni si connettono direttamente (nessun server). Ogni giocatore imposta il proprio nome sul proprio dispositivo. L'app salva le statistiche localmente.

---

## 2. Goals / Non-Goals / Anti-Goals

### Goals
- Scopa e Briscola funzionanti e corrette nei punteggi
- Multiplayer Bluetooth Classic (2 giocatori, stessa stanza)
- Carte siciliane (Denari, Coppe, Spade, Bastoni; Asso→7, Fante, Cavallo, Re)
- Dorso delle carte personalizzabile (immagine fornita dall'utente)
- Round-based: Scopa → primo a 11 pt cumulati; Briscola → primo a superare 120 pt cumulati
- Messaggi ironici fine partita con immagini (vittoria/sconfitta) fornite dall'utente
- Statistiche persistenti per ogni giocatore (nome, partite vinte/perse, punti)
- UI in italiano, architettura pronta per multilingual (string resources)

### Non-Goals
- Online multiplayer (internet)
- Gioco contro AI/CPU
- Più di 2 giocatori
- Varianti regionali extra

### Anti-Goals
- App pesante, complicata da lanciare
- Dipendenza da account / login

---

## 3. Regole Implementate

### Carte Siciliane
- **Mazzo:** 40 carte, 4 semi (Denari, Coppe, Spade, Bastoni)
- **Carte per seme:** Asso (1), 2, 3, 4, 5, 6, 7, Fante (8), Cavallo (9), Re (10)
- Grafica: stile siciliano (immagini asset incluse nell'app)

### SCOPA

#### Svolgimento turno
- Ogni giocatore riceve 3 carte in mano; il tavolo inizia con 4 carte scoperte.
- Si pesca dal mazzo quando entrambi hanno esaurito la mano (si ridistribuiscono 3 carte ciascuno).
- Nel proprio turno si gioca 1 carta:
  - **Presa:** se sul tavolo c'è una carta di uguale valore, si prendono entrambe.
  - **Presa multipla:** si può prendere una combinazione di carte che sommano al valore della carta giocata (es. giocare 7 e prendere 3+4). Regola: si preferisce la presa singola (una carta di ugual valore) se esiste; altrimenti si sceglie la combinazione.
  - **Scopa:** se si prendono TUTTE le carte del tavolo, si fa scopa (+1 punto separato).
  - **Nessuna presa:** la carta rimane sul tavolo.
- Fine mano: chi ha preso per ultimo prende le eventuali carte rimaste sul tavolo (ma NON vale scopa).

#### Punteggio di fine round
| Punto         | Condizione                                                                 |
|---------------|----------------------------------------------------------------------------|
| **Carte**     | Chi ha più di 20 carte prese (+1 pt; pari = nessuno)                       |
| **Denari**    | Chi ha più di 5 carte di Denari (+1 pt; pari = nessuno)                    |
| **Settebello**| Chi ha il 7 di Denari (+1 pt)                                              |
| **Primiera**  | Chi ha il punteggio primiera più alto (+1 pt). Valore primiera: 7=21, 6=18, Asso=16, 5=15, 4=14, 3=13, 2=12, Re/Cavallo/Fante=10. Si somma il valore più alto per ogni seme posseduto (max 4 semi). |
| **Scope**     | +1 pt per ogni scopa fatta durante il round                                |

#### Vittoria Scopa
Prima a raggiungere ≥ 11 punti cumulati (fine round; se entrambi superano 11 nello stesso round, vince chi ne ha di più; se ancora pari, si gioca un altro round).

---

### BRISCOLA

#### Svolgimento turno
- Mazzo da 40 carte. Si distribuiscono 3 carte ciascuno.
- Si gira una carta dal mazzo: il suo seme è la **briscola** (atout). La carta rimane visibile sotto il mazzo.
- Turni: il giocatore di mano gioca una carta; l'avversario risponde. Non c'è obbligo di seguire il seme.
- **Chi prende:** chi ha giocato la carta di valore più alto dello stesso seme del primo, OPPURE chi ha giocato briscola (se l'altro non ha giocato briscola). Se entrambi briscola, vince il valore più alto.
- Dopo ogni turno entrambi pescano una carta dal mazzo (prima chi ha vinto la presa), finché il mazzo è esaurito.
- Quando il mazzo è esaurito si gioca fino a esaurimento mano.

#### Valori carte Briscola
| Carta   | Punti |
|---------|-------|
| Asso    | 11    |
| Tre     | 10    |
| Re      | 4     |
| Cavallo | 3     |
| Fante   | 2     |
| 4,5,6,7,2 | 0  |

Totale punti per partita: **120**.

#### Vittoria Briscola
I punti si accumulano tra partite. Primo a superare **120 punti cumulati** vince la sessione. (Una singola partita vale max 120 pt; vince chi ha >60 in quella partita, ma i punti si accumulano verso il totale di 120.)

---

## 4. Architettura

### Stack tecnologico
- **Linguaggio:** Kotlin
- **UI:** Jetpack Compose
- **Min SDK:** 26 (Android 8.0) — copertura ~95% dispositivi attivi
- **Bluetooth:** Bluetooth Classic (BluetoothSocket, RFCOMM) — adeguato per dati di gioco low-latency nella stessa stanza
- **Persistenza:** Room (SQLite) per statistiche
- **DI:** Hilt
- **Serializzazione messaggi BT:** JSON (kotlinx.serialization)
- **Stringhe:** `strings.xml` italiano; struttura pronta per `values-en/`, `values-es/`, ecc.

### Moduli principali
```
app/
├── bluetooth/      # BtServer, BtClient, BtManager, MessageProtocol
├── game/
│   ├── scopa/      # ScopaGame, ScopaScorer, ScopaRules
│   └── briscola/   # BriscolaGame, BriscolaScorer, BriscolaRules
├── ui/
│   ├── home/       # Schermata principale, scelta gioco
│   ├── lobby/      # Pairing BT, nomi giocatori
│   ├── game/       # Tavolo di gioco (Compose)
│   ├── result/     # Schermata fine round/partita con immagini
│   └── stats/      # Statistiche
├── data/
│   ├── db/         # Room database, DAO, entità
│   └── prefs/      # DataStore per nome giocatore, dorso carta
└── assets/
    ├── cards/      # 40 immagini carte siciliane (PNG, es. asso_denari.png)
    ├── backs/      # Dorso default; slot per dorso personalizzato
    └── result/     # Immagini vittoria/sconfitta (utente le mette qui)
```

### Protocollo Bluetooth
Un dispositivo fa da **Server** (aspetta connessione), l'altro da **Client** (cerca e si connette). Dopo la connessione i ruoli di gioco (chi inizia) si determinano con un handshake.

Messaggi JSON:
```json
{ "type": "PLAY_CARD", "card": "7_DENARI" }
{ "type": "TAKE", "cards": ["3_COPPE","4_BASTONI"] }
{ "type": "GAME_STATE", "state": { ... } }
{ "type": "CHAT_PING" }   // keepalive
```

### Flusso sessione
```
Home → Scegli gioco → Lobby BT (host/join) → Handshake nomi
     → Round di gioco → Schermata risultato round
     → [se non ha vinto] Round successivo
     → Schermata vittoria/sconfitta → Statistiche aggiornate
```

---

## 5. Asset Gestiti dall'Utente

### Dorso carte
- Formato atteso: PNG, almeno 200×300 px
- L'utente mette il file in `assets/backs/custom_back.png` (o lo seleziona dall'app tramite picker)
- **Scelta del designer (mia):** per ora si seleziona da Settings → "Cambia dorso" via file picker Android. Il dorso scelto viene salvato in DataStore.

### Immagini vittoria / sconfitta
- **Bundled nell'APK** — l'utente le sostituisce prima di compilare, non si cambiano dall'app.
- Cartelle: `assets/result/win_1.png`, `win_2.png`, ... e `lose_1.png`, `lose_2.png`, ...
- L'app ne mostra **una casuale** ad ogni fine partita.
- Formato atteso: PNG, almeno 400×400 px.

### Regole Scopa — precisazioni
- Presa singola obbligatoria: se sul tavolo esiste una carta di ugual valore, NON si può scegliere una combinazione multipla.
- Scopa con l'ultima carta del round NON conta come scopa (punti normali, non +1).

### Regole Briscola — precisazioni
- Ultima carta del mazzo (la briscola visibile): chi vince il penultimo trick la prende; l'avversario non riceve carta.
- Primo turno di ogni partita: vantaggio a chi ha "ospitato" (Server BT).

### Carte siciliane
- 40 PNG bundled nell'APK (assets), denominate `{valore}_{seme}.png`
- Valori: asso, 2, 3, 4, 5, 6, 7, fante, cavallo, re
- Semi: denari, coppe, spade, bastoni
- **Fonte:** Wikimedia Commons, licenza libera

---

## 6. Statistiche

Tabella `statistics` in Room:
| Campo           | Tipo    | Note                        |
|-----------------|---------|-----------------------------|
| player_name     | TEXT    | nome locale                 |
| game_type       | TEXT    | "SCOPA" o "BRISCOLA"        |
| sessions_played | INTEGER |                             |
| sessions_won    | INTEGER |                             |
| sessions_lost   | INTEGER |                             |
| total_points    | INTEGER | punti accumulati nel tempo  |
| total_scope     | INTEGER | solo Scopa: scope fatte     |
| last_played     | INTEGER | timestamp                   |

Schermata statistiche: lista ordinata per sessioni vinte, con percentuale vittorie.

---

## 7. Messaggi Ironici Fine Partita

**Vittoria (esempio):**
- "Sei il Campione! ...per ora." ⭐
- "Bravo! Anche un orologio rotto segna l'ora giusta due volte al giorno." 🏆
- "Hai vinto! Probabilmente per caso, ma lo scriviamo lo stesso."

**Sconfitta (esempio):**
- "Hai perso. Ma almeno hai imparato qualcosa... o forse no."
- "Sconfitta epica. Da raccontare ai nipoti."
- "Ci vuole talento per perdere così. Complimenti."

Messaggi selezionati casualmente da una lista hardcoded (5+ per vittoria, 5+ per sconfitta). Estendibili tramite `strings.xml`.

---

## 8. Localizzazione

Tutte le stringhe UI in `res/values/strings.xml` (italiano).  
Struttura pronta per aggiungere `res/values-en/strings.xml`, ecc.  
Nessuna stringa hardcoded nel codice.

---

## 9. Scelte del Designer (mie — verificare)

| Scelta | Dettaglio |
|--------|-----------|
| Min SDK 26 | Android 8.0, buona copertura |
| BT Classic vs BLE | Classic: più semplice, sufficiente per 2 giocatori in stanza |
| Compose UI | Moderno, animazioni carte più facili |
| Room per stats | Leggero, nessuna dipendenza cloud |
| Dorso personalizzabile via file picker | Più flessibile che solo bundle |
| Immagini vittoria/sconfitta bundled + importabili | Utente le prepara prima o le cambia da settings |
| Hilt per DI | Standard Android, testabile |

---

## 10. Fuori Scope (Future)

- Varianti (Scopa d'Assi, Scopone, Briscola a squadre)
- Gioco online
- Suoni / musica di sottofondo
- Animazioni 3D carte
