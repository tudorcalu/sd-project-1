package org.search.ranking;

import org.search.application.SearchResult;
import org.search.parser.ParsedQuery;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

public class AlphabeticalRanker implements Ranker {
    @Override
    public RankingStrategy strategy() {
        return RankingStrategy.ALPHABETICAL;
    }

    @Override
    public Comparator<SearchResult> comparator(ParsedQuery parsedQuery, Map<String, Integer> historyBoostMap) {
        return Comparator.comparing(result -> result.getPath().toLowerCase(Locale.ROOT));
    }
}
