#  **Wooden Solitaire**

Ein klassisches **Solitaire** Spiel, in Java/BlueJ für die Konsole.
Informatik Projekt der K1 von Henry Horlebein, Finn Späh und Aiden Kurz-Feuerstein.

##  **Über das Spiel**

Textbasierte, digitale Version des klassischen "Wooden Solitaire", bei dem man über andere Kugeln springen muss, um diese vom Feld zu entfernen. Ziel des Spiels ist es nur noch eine  bzw. möglichst wenig Kugeln auf dem Feld übrig zu haben.

###  **Abläufe** 
- Das Spiel startet mit einem komplett mit Kugeln befülltem Feld, wobei die mittlere Kugel fehlt
- Man springt nun so lange über eine andere nebenliegende Kugel, in einen nicht belegten "Slot", bis nur noch eine Kugel vorhanden ist oder es keinen möglichen Zug mehr gibt.

### **Regeln**
- Die Kugel muss über eine andere, danebenliegende Kugel springen
-	Die Kugel muss in ein freies, nicht schon durch eine andere Kugel belegtes Feld, springen
-	Das Spiel ist vorbei, wenn es keinen möglichen Zug mehr oder nur noch eine Kugel gibt

### **Elemente/Klassen:**
Beim überlegen wie viele und welche Klassen wir benötigen, haben wir eine Liste erstellt und sind auf diese sechs Klassen gekommen. 
-	**Board**
    - Enthält das Spielfeld und alle Kugeln
-	**MoveValidator**
    - Prüft ob der vom Player gemacht Zug durchgeführt werden kann, ohne dabei Regeln zu missachten 
-	**Input**
    - Scannt die Konsole nach dem Input des Spielers und sortiert dabei nach Reihe und Zeile, sodass das Spiel intuitiv und das Eingabeformat nicht zu kompliziert ist
- **Move** 
    - Wirkt als zwischen "Stück" zwischen dem Input, dem MoveValidator und dem Board
-	**GameStatus** 
     - Prüft ob das Spiel vorbei ist, also ob noch Züge übrig sind
-	**Game**
    - Verbindet alle Klassen miteinander und bildet die Konsolen ausgaben, die das Spiel verständlich machen 


### **Spiel starten**

**Vorraussetzungen:** Eine installierte Version von [BlueJ](https://www.bluej.org/)
1. Öffne das Projekt in BlueJ.
2. Rechts-Click auf die `Game`-Klasse.
3. `void main(String[] args)` auswählen und die Eingabe mit `OK` bestätigen.
4. Spielzug über Konsole Eingeben.

## **Spielanleitung**

Das Spielfeld wird mit Koordinaten angezeigt:
- **Spalten:** A bis G
- **Zeilen:** 1 bis 7
- **Symbole:** 
  - `●` = Spielstein (Ball)
  - `○` = Leeres Feld

### **Einen Zug machen**
Um einen Stein zu bewegen musst du die **Start-Koordinate** und **Ziel-Koordinate** wie in folgenden beispielen eingeben:


#### Beispiel:
Um einen Stein von **E4** nach **E6** zu bewegen:
```text
Zug eingeben: E4 E6
    oder
Zug eingeben: e4 e6
    oder
Zug eingeben: 4E 6E
```
Die Groß und Kleinschreibung und Reihenfolge von Zahl und Buchstabe ist dabei egal, solange zuerst die Start- und anschließend die Ziel-Koordinate kommt.


### **Spiel beenden**
Du kannst das Spiel jederzeit mit dem Befehl `exit` verlassen.

## **UML-Klassendiagramm**
```text
                 ┌────────────────────┐
                 │        Game        │
                 ├────────────────────┤
                 │                    │
                 ├────────────────────┤
                 │   + void main()    │
                 └─────────┬──────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌──────────────┐   ┌──────────────┐   ┌───────────────────┐
│    Board     │   │ MoveValidator│   │       Input       │
├──────────────┤   ├──────────────┤   ├───────────────────┤
│ - field[][]  │   │              │   │    - scanner      │
├──────────────┤   ├──────────────┤   ├───────────────────┤
│ + get()      │   │ + isValid()  │   │ + getUserInput()  │
│ + set()      │   └──────────────┘   │     + parse()     │
│ + print()    │                      └───────────┬───────┘
└──────┬───────┘                                  │
       │                                          ▼
       │                                   ┌──────────────┐
       │                                   │     Move     │   ← Klassenname
       │                                   ├──────────────┤
       │                                   │ - fromRow    │
       │                                   │ - fromCol    │   ← Attribute (Daten)
       │                                   │ - toRow      │
       │                                   │ - toCol      │
       │                                   ├──────────────┤
       │                                   │ + Getter     │   ← Methoden
       │                                   └──────────────┘
       │
       ▼
┌──────────────────┐
│    GameStatus    │
├──────────────────┤
│                  │
├──────────────────┤
│ + hasMovesLeft() │
└──────────────────┘
```
### **Beschreibung UML**
```
    -  → Private
    +  → Public
```
- Die Klassen üben Assoziationen zu einander aus:
    - Eine Klasse kennt, bzw. verwendet eine andere, besitzt sie aber nicht dauerhaft

## **Warum haben wir uns für das Spiel entschieden?**
Wooden Solitär war das einzige, von denen zu uns verfügbaren Spielen, die uns schon bekannt waren. Dementsprechend waren wir schon mit den Regeln vertraut, was beim erstellen der digitalen Version von Vorteil war.
    
## **Autoren**

- **Finn Späh**
- **Henry Horlebein**
- **Aiden Kurz-Feuerstein**

---

## ⏲️ Version History:
```
- v1.4.1: UML-Erweiterung
- v1.4: UML-Diagramm, Formatierung
- v1.3.1: Dokumentation erweitert
- v1.3: Input Erweiterung
- v1.2.2: Syntax Fixed
- v1.2.1: Code Cleanup und ReadMe Erweiterung
- v1.1: Zug- und Bälle Counter hinzugefügt
- v1.0: Initial commit
```