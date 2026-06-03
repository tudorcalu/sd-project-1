package org.search.ranking;

import org.search.application.SearchResult;
import org.search.parser.ParsedQuery;

import java.util.Comparator;
import java.util.Map;

public interface Ranker {
    RankingStrategy strategy();

    Comparator<SearchResult> comparator(ParsedQuery parsedQuery, Map<String, Integer> historyBoostMap);
}
