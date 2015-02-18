package com.samsung.multiscreen.msf20.casteroids.model;

/**
 * Contains a player's score data.
 * 
 * @author Dan McCafferty
 * 
 */
public class ScoreData implements Comparable<ScoreData> {
    // The player's name
    private final String name;

    // The player's score
    private final int score;

    /**
     * Constructor.
     * 
     * @param name
     *            The player's name.
     * @param score
     *            The palyer's score.
     */
    public ScoreData(String name, int score) {
        super();
        this.name = name;
        this.score = score;
    }

    /**
     * Returns the player's name.
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Return's the player's score.
     * 
     * @return
     */
    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "ScoreData [name=" + name + ", score=" + score + "]";
    }

    @Override
    public int compareTo(ScoreData another) {
        return (getScore() - another.getScore());
    }
}
