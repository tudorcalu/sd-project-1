package org.search.ranking;

public enum RankingStrategy {
    PATH_SCORE("Path score"),
    ALPHABETICAL("Alphabetical"),
    LAST_MODIFIED("Last modified");

    private final String label;

    RankingStrategy(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
