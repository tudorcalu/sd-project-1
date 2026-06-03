package org.search.ranking;

import java.util.EnumMap;
import java.util.Map;

public final class RankerRegistry {
    private static final Map<RankingStrategy, Ranker> RANKERS = new EnumMap<>(RankingStrategy.class);

    static {
        RANKERS.put(RankingStrategy.PATH_SCORE, new PathScoreRanker());
        RANKERS.put(RankingStrategy.ALPHABETICAL, new AlphabeticalRanker());
        RANKERS.put(RankingStrategy.LAST_MODIFIED, new LastModifiedRanker());
    }

    private RankerRegistry() {
    }

    public static Ranker forStrategy(RankingStrategy strategy) {
        return RANKERS.getOrDefault(strategy, RANKERS.get(RankingStrategy.PATH_SCORE));
    }
}
