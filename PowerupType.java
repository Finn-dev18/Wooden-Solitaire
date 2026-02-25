public enum PowerupType {
    UNDO("Undo", "Macht die letzte Brettänderung rückgängig."),
    MOVE("Move", "Versetze eine Kugel auf ein beliebiges leeres Feld."),
    BOMB("Bomb", "Entfernt eine Kugel auf dem gewählten Feld."),
    BRIDGE("Bridge Jump", "Sprung über Distanz 3. Entfernt 2 übersprungene Kugeln."),
    RANDSTURM("Randsturm", "Schiebt alle Kugeln zufällig zu einer Wand.");

    private final String label;
    private final String description;

    PowerupType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
