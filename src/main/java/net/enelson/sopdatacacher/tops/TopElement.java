package net.enelson.sopdatacacher.tops;

public class TopElement {
    private final String player;
    private final double score;

    public TopElement(String player, double score) {
        this.player = player;
        this.score = score;
    }

    public String getPlayer() {
        return player;
    }

    public double getScore() {
        return score;
    }
}
