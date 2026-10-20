package pl.projekt.szachy.computer;

/** Relative difficulty presets, not advertised Elo ratings. */
public enum Difficulty {
    EASY("Łatwy", 0, 200), MEDIUM("Średni", 8, 500), HARD("Trudny", 18, 1000);
    private final String label;
    final int skill;
    final int thinkingMillis;
    Difficulty(String label, int skill, int thinkingMillis) {
        this.label = label;
        this.skill = skill;
        this.thinkingMillis = thinkingMillis;
    }
    @Override public String toString() { return label; }
}
