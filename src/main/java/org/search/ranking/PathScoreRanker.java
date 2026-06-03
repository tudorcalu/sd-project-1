package org.search.ranking;

import org.search.application.SearchResult;
import org.search.parser.ParsedQuery;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

public class PathScoreRanker implements Ranker {
    @Override
    public RankingStrategy strategy() {
        return RankingStrategy.PATH_SCORE;
    }

    @Override
    public Comparator<SearchResult> comparator(ParsedQuery parsedQuery, Map<String, Integer> historyBoostMap) {
        return Comparator.comparingDouble((SearchResult result) ->
                computeValue(result, parsedQuery, historyBoostMap)).reversed();
    }

    private static double computeValue(SearchResult result, ParsedQuery parsedQuery, Map<String, Integer> historyBoostMap) {
        String pathLower = result.getPath().toLowerCase(Locale.ROOT);
        double value = result.getPathScore();

        value += countMatches(pathLower, parsedQuery.getPathTerms()) * 6.0;
        value += countMatches(pathLower, parsedQuery.getFreeTerms()) * 3.0;

        long oneMonthMs = 30L * 24 * 60 * 60 * 1000;
        long ageMs = Math.max(0, System.currentTimeMillis() - result.getLastModified());
        double recencyBoost = Math.max(0.0, 1.0 - ((double) ageMs / oneMonthMs)) * 5.0;
        value += recencyBoost;

        int rawHistoryHits = historyBoostMap.getOrDefault(result.getPath(), 0);
        value += Math.min(rawHistoryHits, 10) * 0.4;
        return value;
    }

    private static int countMatches(String haystack, java.util.List<String> terms) {
        int matches = 0;
        for (String term : terms) {
            if (haystack.contains(term.toLowerCase(Locale.ROOT))) {
                matches++;
            }
        }
        return matches;
    }
}
