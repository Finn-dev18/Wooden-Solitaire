# 🪵 Wooden Solitaire

Ein klassisches **Solitaire** Spiel, in Java/BlueJ für die Konsole.
Informatik Projekt der K1 von Henry Horlebein, Finn Späh und Aiden Kurz-Feuerstein.

## 🎮 Über das Spiel

Textbasierte, digitale Version des klassischen "Wooden Solitaire", bei dem man über andere Kugeln springen muss, um diese vom Feld zu entfernen. Ziel des Spiels ist es nur noch eine  bzw. möglichst wenig Kugeln auf dem Feld übrig zu haben.

### ✨ Abläufe 
- Das Spiel startet mit einem komplett mit Kugeln befülltem Feld, wobei die mittlere Kugel fehlt
- Man springt nun so lange über eine andere nebenliegende Kugel, in einen nicht belegten "Slot", bis nur noch eine Kugel vorhanden ist oder es keinen möglichen Zug mehr gibt.

### 🚀 Regeln
- Die Kugel muss über eine andere, danebenliegende Kugel springen
-	Die Kugel muss in ein freies, nicht schon durch eine andere Kugel belegtes Feld, springen
-	Das Spiel ist vorbei, wenn es keinen möglichen Zug mehr oder nur noch eine Kugel gibt

### Elemente/Klassen:
Beim überlegen wie viele und welche Klassen wir benötigen, haben wir eine Liste erstellt und sind auf diese sechs Klassen gekommen. 
-	Board
    - Enthält das Spielfeld und alle Kugeln
-	Move validator
    - Prüft ob der vom Player gemacht Zug durchgeführt werden kann, ohne dabei Regeln zu missachten 
-	Input
    - Scannt die Konsole nach dem Input des Spielers und sortiert dabei nach Reihe und Zeile, sodass das Spiel intuitiv und das Eingabeformat nicht zu kompliziert ist
- Move 
    - Wirkt als zwischen "Stück" zwischen dem Input, dem MoveValidator und dem Board
-	Gamestatus 
     - Prüft ob das Spiel vorbei ist, also ob noch Züge übrig sind
-	Game
    - Verbindet alle Klassen miteinander und bildet die Konsolen ausgaben, die das Spiel verständlich machen 

## 🕹️ Spielanleitung

Das Spielfeld wird mit Koordinaten angezeigt:
- **Spalten:** A bis G
- **Zeilen:** 1 bis 7
- **Symbole:** 
  - `●` = Spielstein (Ball)
  - `○` = Leeres Feld

### Einen Zug machen
Gib die **Start-Koordinate** und die **Ziel-Koordinate** getrennt durch ein Leerzeichen ein.

**Beispiel:**
Um einen Stein von **E4** nach **E6** zu bewegen:
```text
Zug eingeben: E4 E6
```

### Spiel beenden
Tippe `exit`, um das Spiel zu verlassen.

## 👥 Autoren

- **Finn Späh**
- **Henry Horlebein**
- **Aiden Kurzfeuerstein**

---
*Erstellt am 09.12.2025 - Version 1.2*
