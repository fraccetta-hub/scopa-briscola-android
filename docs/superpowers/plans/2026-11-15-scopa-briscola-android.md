# ScopaBriscola Android — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Android app Kotlin/Compose per giocare a Scopa e Briscola via Bluetooth Classic con carte siciliane, scoring cumulativo, statistiche locali e messaggi ironici.

**Architecture:** Server BT è autoritativo sullo stato di gioco; il client invia intent (`PLAY_CARD`, `TAKE`), il server valida e trasmette `GAME_STATE`. La logica di gioco è pura Kotlin (no Android deps), testabile con JUnit. La UI è Jetpack Compose con un unico NavGraph.

**Tech Stack:** Kotlin, Jetpack Compose, Bluetooth Classic (RFCOMM), Room, Hilt, DataStore, kotlinx.serialization, JUnit 4, Coil (immagini asset)

**Spec:** `docs/superpowers/specs/2026-11-15-scopa-briscola-android-design.md`

## Global Constraints

- Min SDK 26, Target SDK 35
- Linguaggio: Kotlin
- Tutte le stringhe UI in `res/values/strings.xml` (nessuna stringa hardcoded)
- Nomi file carte: `{valore}_{seme}.png` (es. `asso_denari.png`, `fante_coppe.png`)
- Nomi file risultato: `win_1.png` … `win_N.png`, `lose_1.png` … `lose_N.png` in `assets/result/`
- Carte siciliane: 40 carte, semi = `denari|coppe|spade|bastoni`, valori = `asso|2|3|4|5|6|7|fante|cavallo|re`
- UUID Bluetooth fisso: `"e3b8c5a2-1f4d-4c7a-9e6b-2d0f8a3c5e7b"`
- Room database name: `"scopa_briscola_db"`
- Server = autoritativo; Client non modifica stato localmente

## Review Focus

1. **Scopa — presa multipla quando esiste anche presa singola:** deve essere vietata; test verifica che `ScopaRules.validCaptures` non la includa se esiste match singolo.
2. **Scopa — scopa con ultima carta del round:** non deve incrementare `scopeCount`; test con scenario ultimo turno.
3. **Briscola — gestione ultima carta mazzo (briscola visibile):** chi vince il penultimo trick la riceve; l'altro non pesca; test con mazzo da 3 carte.
4. **Bluetooth — disconnessione mid-game:** `BtManager` emette `ConnectionEvent.Lost` e il ViewModel mostra dialog "Riprova/Abbandona" senza crashare.
5. **Primiera Scopa — giocatore senza carte di un seme:** quel seme vale 0 nel totale primiera; test con distribuzione asimmetrica.

---

## File Structure

```
app/src/main/
  java/com/fraccetta/scopabriscola/
    domain/
      Card.kt                     # Card, Suit, Rank, Deck
      GameResult.kt               # SessionResult, RoundResult
    game/
      scopa/
        ScopaRules.kt             # validCaptures(), isScopa(), scoreRound()
        ScopaGame.kt              # ScopaGame state machine
        ScopaScorer.kt            # primiera(), scoreRound() -> RoundScore
      briscola/
        BriscolaRules.kt          # trickWinner(), cardPoints(), totalPoints()
        BriscolaGame.kt           # BriscolaGame state machine
    bluetooth/
      BtProtocol.kt               # Message sealed class + JSON serialization
      BtServer.kt                 # BluetoothServerSocket, accept loop
      BtClient.kt                 # BluetoothSocket connect
      BtManager.kt                # StateFlow<BtState>, send(), events Flow
    data/
      db/
        AppDatabase.kt            # Room DB
        StatisticsEntity.kt       # entità stats
        StatisticsDao.kt          # DAO
      prefs/
        UserPreferences.kt        # DataStore: playerName, cardBackPath
      repository/
        StatsRepository.kt
    ui/
      navigation/
        AppNavGraph.kt            # NavHost con tutte le destinazioni
      home/
        HomeScreen.kt
        HomeViewModel.kt
      lobby/
        LobbyScreen.kt            # host/join, BT scan, nomi
        LobbyViewModel.kt
      game/
        GameScreen.kt             # composable condiviso
        scopa/
          ScopaGameViewModel.kt
          ScopaTableComposable.kt # tavolo, mano, prese
        briscola/
          BriscolaGameViewModel.kt
          BriscolaTableComposable.kt
      result/
        ResultScreen.kt           # immagine casuale + messaggio ironico
        ResultViewModel.kt
      stats/
        StatsScreen.kt
        StatsViewModel.kt
      settings/
        SettingsScreen.kt
        SettingsViewModel.kt
      theme/
        Theme.kt
        Color.kt
        Type.kt
    di/
      AppModule.kt                # Hilt bindings
    ScopaBriscolaApp.kt           # Application class

  res/
    values/strings.xml
    values/strings_ironic.xml     # messaggi ironici (array)
  assets/
    cards/                        # 40 PNG carte siciliane
    backs/default_back.png
    result/                       # win_1.png … lose_N.png (utente le mette qui)

app/src/test/
  domain/CardTest.kt
  game/scopa/ScopaRulesTest.kt
  game/scopa/ScopaScorerTest.kt
  game/briscola/BriscolaRulesTest.kt
  game/briscola/BriscolaGameTest.kt
  bluetooth/BtProtocolTest.kt
```

---

### Task 1: Project Setup

**Files:**
- Create: `app/build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ScopaBriscolaApp.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/di/AppModule.kt`

**Interfaces:**
- Produces: progetto compilabile, `ScopaBriscolaApp : Application()` annotato `@HiltAndroidApp`

- [ ] **Step 1: Crea struttura progetto Android con Android Studio o `gradle init`**

Package: `com.fraccetta.scopabriscola`

- [ ] **Step 2: Aggiungi dipendenze in `build.gradle.kts`**

```kotlin
// Compose BOM 2024.09.00, Hilt 2.51, Room 2.6.1,
// DataStore 1.1.1, kotlinx.serialization-json 1.7.1,
// Coil-compose 2.7.0, Navigation-compose 2.8.0
// Bluetooth: nessuna libreria aggiuntiva (android.bluetooth SDK)
```

- [ ] **Step 3: Aggiungi permessi in `AndroidManifest.xml`**

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

- [ ] **Step 4: Crea `ScopaBriscolaApp.kt`**

`@HiltAndroidApp class ScopaBriscolaApp : Application()`

- [ ] **Step 5: Crea `AppModule.kt` stub Hilt**

`@Module @InstallIn(SingletonComponent::class) object AppModule`

- [ ] **Step 6: Verifica build**

```
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: project scaffolding, Gradle deps, Hilt, Bluetooth permissions"
```

---

### Task 2: Domain Model — Card, Suit, Rank, Deck

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/domain/Card.kt`
- Create: `app/src/test/java/com/fraccetta/scopabriscola/domain/CardTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class Suit { DENARI, COPPE, SPADE, BASTONI }
  enum class Rank(val displayName: String, val numericValue: Int) {
      ASSO("asso",1), DUE("2",2), TRE("3",3), QUATTRO("4",4),
      CINQUE("5",5), SEI("6",6), SETTE("7",7),
      FANTE("fante",8), CAVALLO("cavallo",9), RE("re",10)
  }
  data class Card(val rank: Rank, val suit: Suit) {
      val assetName: String  // es. "asso_denari"
  }
  object Deck {
      fun full(): List<Card>          // 40 carte
      fun shuffled(): List<Card>
  }
  ```

- [ ] **Step 1: Scrivi `CardTest.kt`**

```kotlin
@Test fun deckHas40Cards() = assertEquals(40, Deck.full().size)
@Test fun deckAllUnique() = assertEquals(40, Deck.full().toSet().size)
@Test fun assetNameFormat() = assertEquals("asso_denari", Card(Rank.ASSO, Suit.DENARI).assetName)
@Test fun shuffledDeckSameCards() {
    val d = Deck.shuffled()
    assertEquals(40, d.size)
    assertEquals(Deck.full().toSet(), d.toSet())
}
```

- [ ] **Step 2: Esegui test, verifica FAIL**

```
./gradlew test --tests "*.CardTest"
```

- [ ] **Step 3: Implementa `Card.kt`**

`assetName = "${rank.displayName}_${suit.name.lowercase()}"`

- [ ] **Step 4: Esegui test, verifica PASS**

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: Card domain model — Suit, Rank, Card, Deck"
```

---

### Task 3: Scopa — Regole e Scoring

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/game/scopa/ScopaRules.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/game/scopa/ScopaScorer.kt`
- Create: `app/src/test/java/com/fraccetta/scopabriscola/game/scopa/ScopaRulesTest.kt`
- Create: `app/src/test/java/com/fraccetta/scopabriscola/game/scopa/ScopaScorerTest.kt`

**Interfaces:**
- Consumes: `Card`, `Suit`, `Rank` da Task 2
- Produces:
  ```kotlin
  object ScopaRules {
      // Restituisce tutte le combinazioni di carte sul tavolo catturabili
      // con `playedCard`. Se esiste match singolo (stessa Rank),
      // restituisce SOLO quelli (presa singola obbligatoria).
      fun validCaptures(playedCard: Card, tableCards: List<Card>): List<List<Card>>
      
      // true se tableCards era vuoto prima della presa (scopa)
      // ma isLastCardOfRound=true impedisce di contarla
      fun isScopa(tableCards: List<Card>, isLastCardOfRound: Boolean): Boolean
  }
  
  data class RoundScore(
      val carte: Int,        // 0 o 1
      val denari: Int,       // 0 o 1
      val settebello: Int,   // 0 o 1
      val primiera: Int,     // 0 o 1
      val scope: Int         // numero di scope fatte
  ) { val total: Int get() = carte + denari + settebello + primiera + scope }
  
  object ScopaScorer {
      fun primieraValue(rank: Rank): Int  // 7=21,6=18,A=16,5=15,4=14,3=13,2=12,F/C/R=10
      fun scoreRound(
          p1Captured: List<Card>, p1Scope: Int,
          p2Captured: List<Card>, p2Scope: Int
      ): Pair<RoundScore, RoundScore>
  }
  ```

- [ ] **Step 1: Scrivi `ScopaRulesTest.kt`**

```kotlin
// validCaptures: match singolo obbligatorio
@Test fun singleMatchForbidsMulti() {
    val played = Card(Rank.SETTE, Suit.DENARI)
    val table = listOf(Card(Rank.SETTE, Suit.COPPE), Card(Rank.TRE, Suit.BASTONI), Card(Rank.QUATTRO, Suit.SPADE))
    val captures = ScopaRules.validCaptures(played, table)
    // deve restituire SOLO la presa singola [7 Coppe], non [3+4]
    assertEquals(1, captures.size)
    assertEquals(listOf(Card(Rank.SETTE, Suit.COPPE)), captures[0])
}

// validCaptures: multi quando no match singolo
@Test fun multiCaptureWhenNoSingle() {
    val played = Card(Rank.SETTE, Suit.DENARI)
    val table = listOf(Card(Rank.TRE, Suit.BASTONI), Card(Rank.QUATTRO, Suit.SPADE))
    val captures = ScopaRules.validCaptures(played, table)
    assertEquals(1, captures.size)
    assertEquals(2, captures[0].size)
}

// isScopa: false per ultima carta del round
@Test fun scopaFalseOnLastCard() {
    val table = listOf(Card(Rank.ASSO, Suit.DENARI))
    assertFalse(ScopaRules.isScopa(table, isLastCardOfRound = true))
}

@Test fun scopaTrueNormally() {
    val table = listOf(Card(Rank.ASSO, Suit.DENARI))
    assertTrue(ScopaRules.isScopa(table, isLastCardOfRound = false))
}

// nessuna presa possibile: lista vuota
@Test fun noCapturePossible() {
    val played = Card(Rank.RE, Suit.SPADE)
    val table = listOf(Card(Rank.ASSO, Suit.DENARI), Card(Rank.DUE, Suit.COPPE))
    assertTrue(ScopaRules.validCaptures(played, table).isEmpty())
}
```

- [ ] **Step 2: Scrivi `ScopaScorerTest.kt`**

```kotlin
@Test fun settebelloGoesToOwner() {
    val settebello = Card(Rank.SETTE, Suit.DENARI)
    val p1 = listOf(settebello)
    val p2 = emptyList<Card>()
    val (s1, s2) = ScopaScorer.scoreRound(p1, 0, p2, 0)
    assertEquals(1, s1.settebello)
    assertEquals(0, s2.settebello)
}

@Test fun carteParity() {
    // 20 e 20: nessuno prende il punto carte
    val half = Deck.full().take(20)
    val (s1, s2) = ScopaScorer.scoreRound(half, 0, Deck.full().drop(20), 0)
    assertEquals(0, s1.carte + s2.carte)
}

@Test fun primieraWinner() {
    // p1 ha 7 di ogni seme (massima primiera), p2 ha i restanti
    val p1 = Suit.values().map { Card(Rank.SETTE, it) }
    val p2 = Deck.full().filter { it.rank != Rank.SETTE }
    val (s1, s2) = ScopaScorer.scoreRound(p1, 0, p2, 0)
    assertEquals(1, s1.primiera)
    assertEquals(0, s2.primiera)
}

@Test fun primieraWithMissingSuit() {
    // p1 manca di BASTONI: il suo totale primiera usa 0 per quel seme
    val p1 = listOf(Card(Rank.SETTE, Suit.DENARI), Card(Rank.SETTE, Suit.COPPE), Card(Rank.SETTE, Suit.SPADE))
    val p2 = Deck.full().filter { it !in p1 }
    val (s1, s2) = ScopaScorer.scoreRound(p1, 0, p2, 0)
    // p2 ha BASTONI quindi probabile vinca primiera
    assertEquals(1, s2.primiera)
}

@Test fun scopeCountedInTotal() {
    val p1 = Deck.full().take(20)
    val (s1, _) = ScopaScorer.scoreRound(p1, 3, Deck.full().drop(20), 0)
    assertEquals(3, s1.scope)
    assertTrue(s1.total >= 3)
}
```

- [ ] **Step 3: Esegui test, verifica FAIL**

```
./gradlew test --tests "*.ScopaRulesTest" --tests "*.ScopaScorerTest"
```

- [ ] **Step 4: Implementa `ScopaRules.kt`**

`validCaptures`: cerca prima match per Rank uguale; se trovati, restituisce solo quelli. Altrimenti genera tutte le sottoliste di `tableCards` la cui somma `numericValue` == `playedCard.rank.numericValue`.

`isScopa`: `tableCards.size == 1 && !isLastCardOfRound` (il tavolo aveva una sola carta → presa completa → scopa, se non è l'ultima del round).

- [ ] **Step 5: Implementa `ScopaScorer.kt`**

Primiera: per ogni giocatore, per ogni seme, prendi la carta con `primieraValue` massimo (0 se mancante). Somma i 4 valori. Chi ha somma maggiore prende il punto; pari = nessuno.

- [ ] **Step 6: Esegui test, verifica PASS**

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: Scopa rules (validCaptures, isScopa) and scorer (primiera, settebello, carte, denari)"
```

---

### Task 4: Scopa — State Machine

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/game/scopa/ScopaGame.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/domain/GameResult.kt`
- Create: `app/src/test/java/com/fraccetta/scopabriscola/game/scopa/ScopaGameTest.kt`

**Interfaces:**
- Consumes: `ScopaRules`, `ScopaScorer`, `Deck`
- Produces:
  ```kotlin
  data class ScopaState(
      val tableCards: List<Card>,
      val player1Hand: List<Card>,
      val player2Hand: List<Card>,
      val player1Captured: List<Card>,
      val player2Captured: List<Card>,
      val player1Scope: Int,
      val player2Scope: Int,
      val player1SessionScore: Int,   // punteggio cumulato sessione
      val player2SessionScore: Int,
      val currentPlayer: Int,          // 1 o 2
      val phase: ScopaPhase,
      val lastCapture: Pair<Int, List<Card>>?  // (player, cards) per highlight
  )
  
  enum class ScopaPhase { WAITING_PLAY, WAITING_CAPTURE_CHOICE, ROUND_END, SESSION_END }
  
  sealed class ScopaAction {
      data class PlayCard(val card: Card) : ScopaAction()
      data class ChooseCapture(val captureGroup: List<Card>) : ScopaAction()
      object NextRound : ScopaAction()
  }
  
  class ScopaGame(winScore: Int = 11) {
      fun newSession(): ScopaState
      fun apply(state: ScopaState, action: ScopaAction): ScopaState
      // Lancia IllegalStateException se l'azione è illegale nello stato corrente
  }
  ```

- [ ] **Step 1: Scrivi `ScopaGameTest.kt`**

```kotlin
@Test fun dealGives3CardsEach4OnTable() {
    val g = ScopaGame(); val s = g.newSession()
    assertEquals(3, s.player1Hand.size)
    assertEquals(3, s.player2Hand.size)
    assertEquals(4, s.tableCards.size)
}

@Test fun playCardWithNoCapture_staysOnTable() {
    val g = ScopaGame()
    var s = g.newSession()
    // forza scenario: gioca carta che non cattura nulla
    val card = s.player1Hand.first { cap -> ScopaRules.validCaptures(cap, s.tableCards).isEmpty() }
        ?: return  // skip se non esiste
    s = g.apply(s, ScopaAction.PlayCard(card))
    assertTrue(s.tableCards.contains(card))
}

@Test fun sessionEndsAtWinScore() {
    // simula raggiungimento win score
    val g = ScopaGame(winScore = 1)
    var s = g.newSession()
    // inietta stato con player1SessionScore = 1 e fase ROUND_END
    // poi applica NextRound e verifica SESSION_END
    // (test white-box tramite reflection o factory method)
    // Implementare come factory: ScopaGame.withState(...)
}
```

- [ ] **Step 2: Esegui test, verifica FAIL**

```
./gradlew test --tests "*.ScopaGameTest"
```

- [ ] **Step 3: Implementa `ScopaGame.kt`**

`apply` gestisce: `PlayCard` → se validCaptures vuoto, carta va al tavolo, turno passa; se un solo gruppo, presa auto; se multipli, fase `WAITING_CAPTURE_CHOICE`. `ChooseCapture` → rimuove carte dal tavolo, le aggiunge a captured, controlla scopa, pesca se mano vuota, controlla fine round. Fine round: calcola `scoreRound`, aggiorna `playerNSessionScore`, se ≥ winScore → `SESSION_END`, altrimenti `ROUND_END`.

- [ ] **Step 4: Esegui test, verifica PASS**

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: ScopaGame state machine — deal, play, capture, round end, session end"
```

---

### Task 5: Briscola — Regole e State Machine

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/game/briscola/BriscolaRules.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/game/briscola/BriscolaGame.kt`
- Create: `app/src/test/java/com/fraccetta/scopabriscola/game/briscola/BriscolaRulesTest.kt`
- Create: `app/src/test/java/com/fraccetta/scopabriscola/game/briscola/BriscolaGameTest.kt`

**Interfaces:**
- Consumes: `Card`, `Suit`, `Rank`, `Deck`
- Produces:
  ```kotlin
  object BriscolaRules {
      fun cardPoints(rank: Rank): Int  // A=11,3=10,R=4,C=3,F=2,altri=0
      fun trickWinner(
          firstCard: Card, secondCard: Card,
          firstPlayer: Int, briscolaSuit: Suit
      ): Int  // 1 o 2
  }
  
  data class BriscolaState(
      val briscolaSuit: Suit,
      val briscolaCard: Card,          // ultima carta del mazzo (visibile)
      val deckCards: List<Card>,       // esclude briscolaCard
      val player1Hand: List<Card>,
      val player2Hand: List<Card>,
      val player1Captured: List<Card>,
      val player2Captured: List<Card>,
      val player1SessionPoints: Int,   // cumulato tra partite
      val player2SessionPoints: Int,
      val currentPlayer: Int,
      val firstCardPlayed: Card?,      // carta del primo giocatore nel trick corrente
      val firstPlayerOfTrick: Int,
      val phase: BriscolaPhase
  )
  
  enum class BriscolaPhase { WAITING_FIRST, WAITING_SECOND, GAME_END, SESSION_END }
  
  class BriscolaGame(winPoints: Int = 121) {
      fun newSession(): BriscolaState
      fun newGame(existingState: BriscolaState): BriscolaState  // nuova partita, conserva punteggi sessione
      fun apply(state: BriscolaState, card: Card): BriscolaState
  }
  ```

- [ ] **Step 1: Scrivi `BriscolaRulesTest.kt`**

```kotlin
@Test fun cardPoints() {
    assertEquals(11, BriscolaRules.cardPoints(Rank.ASSO))
    assertEquals(10, BriscolaRules.cardPoints(Rank.TRE))
    assertEquals(4, BriscolaRules.cardPoints(Rank.RE))
    assertEquals(3, BriscolaRules.cardPoints(Rank.CAVALLO))
    assertEquals(2, BriscolaRules.cardPoints(Rank.FANTE))
    assertEquals(0, BriscolaRules.cardPoints(Rank.SETTE))
}

@Test fun briscolaBeatsHigherCard() {
    val briscolaCard = Card(Rank.DUE, Suit.DENARI)  // briscola bassa
    val highCard = Card(Rank.ASSO, Suit.COPPE)        // asso non-briscola
    // p2 risponde con briscola bassa, p1 ha giocato asso: vince p2
    assertEquals(2, BriscolaRules.trickWinner(highCard, briscolaCard, 1, Suit.DENARI))
}

@Test fun higherCardSameSuitWins() {
    val first = Card(Rank.RE, Suit.SPADE)
    val second = Card(Rank.ASSO, Suit.SPADE)
    // p2 risponde con asso stesso seme: vince p2
    assertEquals(2, BriscolaRules.trickWinner(first, second, 1, Suit.DENARI))
}

@Test fun differentSuiteNoBriscolaFirstWins() {
    val first = Card(Rank.DUE, Suit.COPPE)
    val second = Card(Rank.RE, Suit.SPADE)   // re seme diverso, no briscola
    assertEquals(1, BriscolaRules.trickWinner(first, second, 1, Suit.DENARI))
}
```

- [ ] **Step 2: Scrivi `BriscolaGameTest.kt`**

```kotlin
@Test fun dealGives3CardsEach() {
    val g = BriscolaGame(); val s = g.newSession()
    assertEquals(3, s.player1Hand.size)
    assertEquals(3, s.player2Hand.size)
}

@Test fun lastCardIsbriscola() {
    val s = BriscolaGame().newSession()
    assertEquals(s.briscolaSuit, s.briscolaCard.suit)
}

@Test fun penultimateWinnerGetsBriscolaCard() {
    // scenario con mazzo da 1 carta (la briscola) dopo il turno corrente
    // il vincitore del trick deve ricevere briscolaCard; l'altro no
    // usa BriscolaGame con stato iniettato
}

@Test fun sessionEndsWhenCumulated121() {
    val g = BriscolaGame(winPoints = 121)
    // inietta stato con player1SessionPoints = 110 e game appena finito
    // applica newGame e verifica SESSION_END se p1 raggiunge 121
}
```

- [ ] **Step 3: Esegui test, verifica FAIL**

```
./gradlew test --tests "*.BriscolaRulesTest" --tests "*.BriscolaGameTest"
```

- [ ] **Step 4: Implementa `BriscolaRules.kt`**

`trickWinner`: se secondCard ha suit == briscolaSuit e firstCard no → 2 vince. Se firstCard ha suit == briscolaSuit e secondCard no → 1 vince. Se stesso seme → vince chi ha `numericValue` maggiore. Diversi semi, nessuno briscola → 1 vince (non ha seguito ma primo ha giocato).

- [ ] **Step 5: Implementa `BriscolaGame.kt`**

`apply`: fase `WAITING_FIRST` → salva carta come `firstCardPlayed`, fase → `WAITING_SECOND`. Fase `WAITING_SECOND` → calcola vincitore trick, aggiunge carte a captured, pesca (vincitore prima poi perdente; se rimane solo briscolaCard, vincitore la prende; se mazzo esaurito non pesca), vincitore diventa `currentPlayer`, controlla fine partita (mani vuote). Fine partita: somma punti, aggiorna `playerNSessionPoints`, se ≥ winPoints → `SESSION_END`, altrimenti `GAME_END`.

- [ ] **Step 6: Esegui test, verifica PASS**

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: Briscola rules (trickWinner, cardPoints) and game state machine"
```

---

### Task 6: Bluetooth Layer

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/bluetooth/BtProtocol.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/bluetooth/BtServer.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/bluetooth/BtClient.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/bluetooth/BtManager.kt`
- Create: `app/src/test/java/com/fraccetta/scopabriscola/bluetooth/BtProtocolTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  // --- BtProtocol.kt ---
  @Serializable
  sealed class BtMessage {
      @Serializable @SerialName("HANDSHAKE")
      data class Handshake(val playerName: String, val role: String) : BtMessage()
      
      @Serializable @SerialName("PLAY_CARD")
      data class PlayCard(val cardAssetName: String) : BtMessage()
      
      @Serializable @SerialName("CHOOSE_CAPTURE")
      data class ChooseCapture(val cardAssetNames: List<String>) : BtMessage()
      
      @Serializable @SerialName("GAME_STATE")
      data class GameState(val stateJson: String) : BtMessage()
      // stateJson contiene ScopaState o BriscolaState serializzato
      
      @Serializable @SerialName("NEXT_ROUND")
      object NextRound : BtMessage()
      
      @Serializable @SerialName("PING")
      object Ping : BtMessage()
  }
  
  object BtProtocol {
      fun encode(msg: BtMessage): String     // JSON line
      fun decode(line: String): BtMessage    // può lanciare SerializationException
  }
  
  // --- BtManager.kt ---
  sealed class BtState {
      object Idle : BtState()
      object Hosting : BtState()
      object Scanning : BtState()
      data class Connected(val remoteDevice: String) : BtState()
      object Disconnected : BtState()
  }
  
  sealed class ConnectionEvent {
      object Connected : ConnectionEvent()
      data class MessageReceived(val msg: BtMessage) : ConnectionEvent()
      object Lost : ConnectionEvent()
  }
  
  class BtManager(context: Context) {
      val state: StateFlow<BtState>
      val events: Flow<ConnectionEvent>
      fun startHosting()           // avvia BtServer
      fun startScanning()          // avvia BtClient scan
      fun connect(device: BluetoothDevice)
      fun send(msg: BtMessage)
      fun disconnect()
  }
  ```

- [ ] **Step 1: Scrivi `BtProtocolTest.kt`**

```kotlin
@Test fun encodeDecodeRoundtrip() {
    val msg = BtMessage.PlayCard("asso_denari")
    val encoded = BtProtocol.encode(msg)
    val decoded = BtProtocol.decode(encoded)
    assertEquals(msg, decoded)
}

@Test fun encodeDecodeHandshake() {
    val msg = BtMessage.Handshake("Mario", "SERVER")
    assertEquals(msg, BtProtocol.decode(BtProtocol.encode(msg)))
}

@Test fun decodeInvalidThrows() {
    assertThrows(SerializationException::class.java) {
        BtProtocol.decode("not-json")
    }
}
```

- [ ] **Step 2: Esegui test, verifica FAIL**

```
./gradlew test --tests "*.BtProtocolTest"
```

- [ ] **Step 3: Implementa `BtProtocol.kt`**

Usa `kotlinx.serialization` con `PolymorphicSerializer` e `SerialName` per il discriminatore `type`. `encode` aggiunge `\n` alla fine per line-based reading.

- [ ] **Step 4: Esegui test protocol, verifica PASS**

- [ ] **Step 5: Implementa `BtServer.kt`**

`BluetoothServerSocket` con UUID `e3b8c5a2-...`. Loop `accept()` su coroutine `Dispatchers.IO`. Una volta connesso, legge linee da `BufferedReader`, emette su channel.

- [ ] **Step 6: Implementa `BtClient.kt`**

Scan dispositivi paired (`bondedDevices`). `connect(device)` su `Dispatchers.IO` con retry 1×. Legge linee e scrive su channel.

- [ ] **Step 7: Implementa `BtManager.kt`**

Coordina `BtServer`/`BtClient`. Espone `StateFlow<BtState>` e `Flow<ConnectionEvent>`. `send()` scrive su output stream. Keepalive: invia `Ping` ogni 15 s se connesso.

- [ ] **Step 8: Build verifica (no crash)**

```
./gradlew assembleDebug
```

- [ ] **Step 9: Commit**

```bash
git add .
git commit -m "feat: Bluetooth layer — BtProtocol, BtServer, BtClient, BtManager"
```

---

### Task 7: Persistenza — Room Stats + DataStore Prefs

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/data/db/AppDatabase.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/data/db/StatisticsEntity.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/data/db/StatisticsDao.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/data/prefs/UserPreferences.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/data/repository/StatsRepository.kt`
- Modify: `app/src/main/java/com/fraccetta/scopabriscola/di/AppModule.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Entity(tableName = "statistics")
  data class StatisticsEntity(
      @PrimaryKey val id: String,  // "$playerName-$gameType"
      val playerName: String,
      val gameType: String,        // "SCOPA" | "BRISCOLA"
      val sessionsPlayed: Int = 0,
      val sessionsWon: Int = 0,
      val sessionsLost: Int = 0,
      val totalPoints: Int = 0,
      val totalScope: Int = 0,
      val lastPlayed: Long = 0L
  )
  
  @Dao interface StatisticsDao {
      @Query("SELECT * FROM statistics ORDER BY sessionsWon DESC")
      fun getAllStats(): Flow<List<StatisticsEntity>>
      
      @Query("SELECT * FROM statistics WHERE playerName=:name AND gameType=:type LIMIT 1")
      suspend fun getStats(name: String, type: String): StatisticsEntity?
      
      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun upsert(entity: StatisticsEntity)
  }
  
  class UserPreferences(context: Context) {
      val playerName: Flow<String>           // default ""
      val cardBackPath: Flow<String>         // default "" = usa default_back
      suspend fun setPlayerName(name: String)
      suspend fun setCardBackPath(path: String)
  }
  
  class StatsRepository(dao: StatisticsDao) {
      fun getAllStats(): Flow<List<StatisticsEntity>>
      suspend fun recordResult(playerName: String, gameType: String,
                               won: Boolean, points: Int, scope: Int = 0)
  }
  ```

- [ ] **Step 1: Implementa `StatisticsEntity.kt`, `StatisticsDao.kt`, `AppDatabase.kt`**

Database name: `"scopa_briscola_db"`, version 1.

- [ ] **Step 2: Implementa `UserPreferences.kt`**

DataStore con chiavi `"player_name"` e `"card_back_path"`.

- [ ] **Step 3: Implementa `StatsRepository.kt`**

`recordResult`: legge row esistente, aggiorna contatori, fa `upsert`.

- [ ] **Step 4: Aggiorna `AppModule.kt`**

Provide `AppDatabase`, `StatisticsDao`, `UserPreferences`, `StatsRepository` come `@Singleton`.

- [ ] **Step 5: Build verifica**

```
./gradlew assembleDebug
```

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: Room stats DB, DataStore prefs, StatsRepository"
```

---

### Task 8: Asset — Carte Siciliane

**Files:**
- Create: script `scripts/download_cards.py` (tool build, non in APK)
- Populate: `app/src/main/assets/cards/` (40 PNG)
- Create: `app/src/main/assets/backs/default_back.png`

**Interfaces:**
- Produces: 40 file PNG in `assets/cards/` con nomi `{rank}_{suit}.png`

- [ ] **Step 1: Cerca carte siciliane su Wikimedia Commons**

URL base: `https://commons.wikimedia.org/wiki/Category:Sicilian_playing_cards`

Scarica manualmente o con script le 40 immagini. Se non disponibili con licenza libera, usa le **carte napoletane** (più comuni su Wikimedia, stesso schema 40 carte) come placeholder, da sostituire.

- [ ] **Step 2: Rinomina file secondo convenzione**

Schema: `asso_denari.png`, `2_denari.png`, ..., `re_bastoni.png`

Script Python di utilità in `scripts/rename_cards.py` se necessario.

- [ ] **Step 3: Crea `default_back.png`**

Immagine dorso placeholder (colore pieno + testo "Scopa & Briscola") se l'utente non ha ancora fornito il dorso personalizzato.

- [ ] **Step 4: Verifica asset caricabili**

Scrivi un test instrumentato che apre ogni asset e verifica che non sia null:
```kotlin
@Test fun allCardAssetsExist() {
    Suit.values().forEach { suit ->
        Rank.values().forEach { rank ->
            val name = "${rank.displayName}_${suit.name.lowercase()}.png"
            assertNotNull(context.assets.open("cards/$name"))
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/
git commit -m "feat: add 40 Sicilian card assets and default card back"
```

---

### Task 9: UI — Theme, Navigation, Home, Settings

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/theme/Color.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/navigation/AppNavGraph.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/home/HomeViewModel.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/MainActivity.kt`

**Interfaces:**
- Consumes: `UserPreferences`, `StatsRepository` via Hilt
- Produces: app navigabile: Home → Scopa/Briscola/Stats/Settings

- [ ] **Step 1: Implementa `Theme.kt` e `Color.kt`**

Palette verde tavolo da gioco: `TableGreen = Color(0xFF2D6A2D)`, `CardCream = Color(0xFFFFF8DC)`, `AccentGold = Color(0xFFDAA520)`. MaterialTheme con queste colori.

- [ ] **Step 2: Implementa `AppNavGraph.kt`**

Route: `"home"`, `"lobby/{gameType}"`, `"game/scopa"`, `"game/briscola"`, `"result/{winner}/{gameType}"`, `"stats"`, `"settings"`. Usa `NavHost` + `composable {}`.

- [ ] **Step 3: Implementa `HomeScreen.kt`**

Pulsanti: "Gioca a Scopa", "Gioca a Briscola", "Statistiche", "Impostazioni". Mostra nome giocatore corrente.

- [ ] **Step 4: Implementa `SettingsScreen.kt` + `SettingsViewModel.kt`**

Campo testo per nome giocatore (salvato in DataStore). Pulsante "Cambia dorso" → `ActivityResultContracts.GetContent("image/*")` → salva URI in DataStore.

- [ ] **Step 5: Build + avvia su emulatore, verifica navigazione**

```
./gradlew installDebug
```

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: theme, navigation, home screen, settings screen"
```

---

### Task 10: UI — Lobby Bluetooth

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/lobby/LobbyScreen.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/lobby/LobbyViewModel.kt`

**Interfaces:**
- Consumes: `BtManager` (via Hilt), `UserPreferences`
- Produces: schermata che permette a due telefoni di connettersi via BT e poi naviga a game screen

- [ ] **Step 1: Implementa `LobbyViewModel.kt`**

Espone `StateFlow<LobbyUiState>`:
```kotlin
data class LobbyUiState(
    val btState: BtState,
    val pairedDevices: List<BluetoothDevice>,
    val remoteName: String?,
    val error: String?
)
```
Metodi: `startHosting()`, `startScanning()`, `connectTo(device)`. Dopo `ConnectionEvent.Connected`, scambia `Handshake`, naviga a game screen.

- [ ] **Step 2: Implementa `LobbyScreen.kt`**

Due pulsanti: "Ospita partita" / "Cerca partita". In scanning: lista dispositivi paired. Spinner mentre connette. Mostra nome remoto dopo handshake.

- [ ] **Step 3: Gestione permessi BT runtime**

`rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())` per `BLUETOOTH_CONNECT` e `BLUETOOTH_SCAN` su SDK ≥ 31.

- [ ] **Step 4: Test manuale su due dispositivi fisici**

Verifica connessione e scambio nomi.

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: BT lobby screen — host/join, device list, handshake"
```

---

### Task 11: UI — Tavolo Scopa

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/game/scopa/ScopaGameViewModel.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/game/scopa/ScopaTableComposable.kt`

**Interfaces:**
- Consumes: `ScopaGame`, `BtManager`, `ScopaRules`
- Produces: tavolo di gioco Scopa funzionante

- [ ] **Step 1: Implementa `ScopaGameViewModel.kt`**

Server: tiene `ScopaGame` e `ScopaState`, applica azioni, invia `GameState` via BT dopo ogni move.
Client: riceve `GameState`, aggiorna UI state locale; invia `PlayCard`/`ChooseCapture` al server.
Espone:
```kotlin
val uiState: StateFlow<ScopaUiState>
fun onCardPlayed(card: Card)      // locale se è il proprio turno
fun onCaptureChosen(group: List<Card>)
fun onNextRound()
```

- [ ] **Step 2: Implementa `ScopaTableComposable.kt`**

Layout: area tavolo (centro), mano giocatore locale (bottom), punteggio (top).
- Carte: `Image(painter = painterResource(...))` da assets con Coil o `BitmapFactory`.
- Fase `WAITING_CAPTURE_CHOICE`: bottom sheet con opzioni presa.
- Fase `ROUND_END`: dialog con punteggi del round e pulsante "Prossimo round".
- Navigazione a `ResultScreen` su `SESSION_END`.

- [ ] **Step 3: Carte asset loading utility**

```kotlin
@Composable fun CardImage(card: Card, modifier: Modifier = Modifier) {
    // usa LocalContext.current.assets.open("cards/${card.assetName}.png")
    // converti in ImageBitmap e mostra; fallback testo se asset mancante
}
```

- [ ] **Step 4: Test manuale partita completa**

Gioca una sessione completa su due dispositivi, verifica punteggi.

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: Scopa game table UI — card display, capture chooser, round end dialog"
```

---

### Task 12: UI — Tavolo Briscola

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/game/briscola/BriscolaGameViewModel.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/game/briscola/BriscolaTableComposable.kt`

**Interfaces:**
- Consumes: `BriscolaGame`, `BriscolaRules`, `BtManager`
- Produces: tavolo di gioco Briscola funzionante

- [ ] **Step 1: Implementa `BriscolaGameViewModel.kt`**

Stessa architettura server/client di ScopaGameViewModel. Metodo `onCardPlayed(card: Card)`. Gestisce entrambe le fasi `WAITING_FIRST` e `WAITING_SECOND`. Su `GAME_END`: mostra dialog risultato partita, pulsante "Nuova partita" chiama `BriscolaGame.newGame()`. Su `SESSION_END`: naviga a ResultScreen.

- [ ] **Step 2: Implementa `BriscolaTableComposable.kt`**

Layout: briscola visibile (angolo), mazzo (sopra briscola), centro trick (due carte affiancate con highlight 1.5s al vincitore), mano giocatore locale (bottom).

- [ ] **Step 3: Test manuale partita completa Briscola**

Verifica accumulo punti tra partite, navigazione a ResultScreen su 121.

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: Briscola game table UI — trick display, multi-game session, win condition"
```

---

### Task 13: UI — Schermata Risultato + Messaggi Ironici

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/result/ResultScreen.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/result/ResultViewModel.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/strings_ironic.xml`

**Interfaces:**
- Consumes: `StatsRepository`, nomi giocatori, vincitore (parametro nav)
- Produces: schermata fine sessione con immagine casuale + messaggio ironico

- [ ] **Step 1: Aggiungi messaggi ironici in `strings_ironic.xml`**

```xml
<string-array name="win_messages">
    <item>Sei il Campione! ...per ora.</item>
    <item>Bravo! Anche un orologio rotto segna l\'ora giusta due volte al giorno.</item>
    <item>Hai vinto! Probabilmente per caso, ma lo scriviamo lo stesso.</item>
    <item>Incredibile. Anche tu ogni tanto ci riesci.</item>
    <item>Vittoria! Goditi il momento, dura poco.</item>
</string-array>
<string-array name="lose_messages">
    <item>Hai perso. Ma almeno hai imparato qualcosa... o forse no.</item>
    <item>Sconfitta epica. Da raccontare ai nipoti.</item>
    <item>Ci vuole talento per perdere così. Complimenti.</item>
    <item>Non è colpa tua. Sì, invece.</item>
    <item>La prossima volta andrà peggio. No, dai, scherzavo... forse.</item>
</string-array>
```

- [ ] **Step 2: Implementa `ResultViewModel.kt`**

Riceve `winnerName: String`, `gameType: String`, `localPlayerName: String`.
- Carica lista file `assets/result/win_*.png` o `lose_*.png` con `AssetManager.list("result")`.
- Seleziona uno random.
- Chiama `StatsRepository.recordResult(...)`.
- Espone `ResultUiState(imagePath, message, winnerName)`.

- [ ] **Step 3: Implementa `ResultScreen.kt`**

Mostra: immagine casuale (fullscreen o grande), nome vincitore, messaggio ironico, pulsante "Torna al menu". Animazione fade-in sull'immagine.

- [ ] **Step 4: Verifica con immagini placeholder**

Metti `win_1.png` e `lose_1.png` in assets/result/, verifica che appaiano correttamente.

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: result screen — random ironic message, random win/lose image, stats recording"
```

---

### Task 14: UI — Statistiche

**Files:**
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/stats/StatsScreen.kt`
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/stats/StatsViewModel.kt`

**Interfaces:**
- Consumes: `StatsRepository`
- Produces: schermata statistiche con lista giocatori

- [ ] **Step 1: Implementa `StatsViewModel.kt`**

Espone `Flow<List<StatisticsEntity>>` raccolto come `State` in Compose. Separa Scopa e Briscola.

- [ ] **Step 2: Implementa `StatsScreen.kt`**

Tab "Scopa" / "Briscola". Per ogni giocatore: nome, partite giocate/vinte/perse, % vittorie, punti totali, scope totali (solo Scopa).

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: statistics screen — per-game-type stats with win percentage"
```

---

### Task 15: Integrazione Finale + Gestione Disconnessione

**Files:**
- Modify: tutti i ViewModel di gioco
- Create: `app/src/main/java/com/fraccetta/scopabriscola/ui/game/DisconnectionDialog.kt`

**Interfaces:**
- Consumes: `BtManager.events`
- Produces: app completa con gestione robusta della disconnessione

- [ ] **Step 1: Implementa `DisconnectionDialog.kt`**

Composable modale: "Connessione persa. Vuoi riprovare?" con pulsanti "Riprova" / "Abbandona". Abbandona naviga a Home senza salvare stats.

- [ ] **Step 2: Integra `ConnectionEvent.Lost` in tutti i GameViewModel**

Osserva `btManager.events` in `viewModelScope`. Su `Lost`: emette evento UI che mostra `DisconnectionDialog`. Su riconnessione entro 60s (Server riascolta, Client prova a riconnettersi 1×): server reinvia `GAME_STATE`.

- [ ] **Step 3: Test integrazione completa**

Scenario end-to-end su due dispositivi: lobby → partita Scopa → fine round → punteggio → fine sessione → statistiche aggiornate.
Scenario Briscola: lobby → 2+ partite → 121 cumulato → result screen.

- [ ] **Step 4: ProGuard / R8 rules**

Aggiungi rules per kotlinx.serialization e Room in `proguard-rules.pro`.

- [ ] **Step 5: Release build verifica**

```
./gradlew assembleRelease
```

- [ ] **Step 6: Commit finale**

```bash
git add .
git commit -m "feat: disconnection handling, end-to-end integration, ProGuard rules"
```

---

## Riepilogo Task

| Task | Deliverable | Test |
|------|-------------|------|
| 1 | Project setup, Gradle, Hilt | Build verde |
| 2 | Card domain model | CardTest |
| 3 | Scopa rules + scorer | ScopaRulesTest, ScopaScorerTest |
| 4 | Scopa state machine | ScopaGameTest |
| 5 | Briscola rules + state machine | BriscolaRulesTest, BriscolaGameTest |
| 6 | Bluetooth layer | BtProtocolTest |
| 7 | Room DB + DataStore | Build + manuale |
| 8 | Asset carte siciliane | AssetTest instrumentato |
| 9 | Theme, nav, home, settings | Manuale |
| 10 | Lobby BT | Manuale 2 device |
| 11 | Tavolo Scopa UI | Manuale partita completa |
| 12 | Tavolo Briscola UI | Manuale partita completa |
| 13 | Schermata risultato | Manuale |
| 14 | Statistiche UI | Manuale |
| 15 | Integrazione + disconnessione | End-to-end 2 device |
