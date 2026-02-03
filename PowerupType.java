public enum PowerupType {
    HINT("Hint", 1),
    BOMB("Bomb", 6),
    SWAP("Swap", 4),
    FREEZE("Freeze", 5),
    LASER("Laser", 7),
    SHIELD("Shield", 5);

    private final String label;
    private final int cost;

    PowerupType(String label, int cost) {
        this.label = label;
        this.cost = cost;
    }

    public String getLabel() {
        return label;
    }

    public int getCost() {
        return cost;
    }
}
