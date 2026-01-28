import java.util.Objects;

public class PlayerAccount {

    private final String name;
    private int credits;
    private int hammerCount;
    private int slideCount;

    public PlayerAccount(String name) {
        this(name, 0, 0);
    }

    public PlayerAccount(String name, int credits, int hammerCount) {
        this(name, credits, hammerCount, 0);
    }

    public PlayerAccount(String name, int credits, int hammerCount, int slideCount) {
        this.name = name;
        this.credits = Math.max(0, credits);
        this.hammerCount = Math.max(0, hammerCount);
        this.slideCount = Math.max(0, slideCount);
    }

    public String getName() {
        return name;
    }

    public int getCredits() {
        return credits;
    }

    public int getHammerCount() {
        return hammerCount;
    }

    public int getSlideCount() {
        return slideCount;
    }

    public void addCredits(int amount) {
        if (amount > 0) {
            credits += amount;
        }
    }

    public boolean spendCredits(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (credits < amount) {
            return false;
        }
        credits -= amount;
        return true;
    }

    public void addHammer(int amount) {
        if (amount > 0) {
            hammerCount += amount;
        }
    }

    public boolean useHammer() {
        if (hammerCount <= 0) {
            return false;
        }
        hammerCount--;
        return true;
    }

    public void addSlide(int amount) {
        if (amount > 0) {
            slideCount += amount;
        }
    }

    public boolean useSlide() {
        if (slideCount <= 0) {
            return false;
        }
        slideCount--;
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PlayerAccount other = (PlayerAccount) obj;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
