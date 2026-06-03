package org.search.ranking;

import org.search.application.SearchResult;
import org.search.parser.ParsedQuery;

import java.util.Comparator;
import java.util.Map;

public class LastModifiedRanker implements Ranker {
    @Override
    public RankingStrategy strategy() {
        return RankingStrategy.LAST_MODIFIED;
    }

    @Override
    public Comparator<SearchResult> comparator(ParsedQuery parsedQuery, Map<String, Integer> historyBoostMap) {
        return Comparator.comparingLong(SearchResult::getLastModified).reversed();
    }
}
