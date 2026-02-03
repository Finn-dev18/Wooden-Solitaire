public enum PowerupType {
    HINT("Hint"),
    BOMB("Bomb"),
    SWAP("Swap"),
    FREEZE("Freeze"),
    LASER("Laser"),
    SHIELD("Shield");

    private final String label;

    PowerupType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
