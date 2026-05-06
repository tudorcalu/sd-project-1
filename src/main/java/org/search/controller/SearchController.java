package org.search.controller;

import org.search.db.QueryHandler;
import org.search.parser.ParsedQuery;
import org.search.parser.QueryParser;
import org.search.parser.Tokenizer;
import org.search.ranking.RankingStrategy;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SearchController {

    private final QueryParser queryParser = new QueryParser();

    public SearchController() {
        QueryHandler.initializeDatabase();
    }

    public List<SearchResult> search(String queryStr, RankingStrategy rankingStrategy) {
        List<SearchResult> results = new ArrayList<>();
        ParsedQuery parsedQuery = queryParser.parse(queryStr);
        if (parsedQuery.getRawQuery().isBlank()) {
            return results;
        }
        QueryHandler.recordSearchQuery(parsedQuery.getRawQuery());

        Set<String> candidatePaths = findCandidatePaths(parsedQuery);
        List<QueryHandler.FileRecord> records = QueryHandler.getFilesByPaths(candidatePaths, 300);
        for (QueryHandler.FileRecord record : records) {
            String snippet = buildSnippet(record.getContent(), queryStr);
            results.add(new SearchResult(record.getPath(), snippet, record.getLastModified(), record.getPathScore()));
        }

        applyRanking(results, rankingStrategy, parsedQuery);
        List<String> topPaths = new ArrayList<>();
        for (int i = 0; i < Math.min(20, results.size()); i++) {
            topPaths.add(results.get(i).getPath());
        }
        QueryHandler.recordResultHits(parsedQuery.getRawQuery(), topPaths);
        return results;
    }

    public String describeParsedQuery(String queryStr) {
        ParsedQuery parsed = queryParser.parse(queryStr == null ? "" : queryStr);
        return parsed.describeForUi();
    }

    public List<String> getSuggestions(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }
        return QueryHandler.getSuggestions(prefix.trim(), 5);
    }

    private Set<String> findCandidatePaths(ParsedQuery parsedQuery) {
        Set<String> candidates = null;

        List<String> pathTokens = toQueryTokens(parsedQuery.getPathTerms());
        if (!pathTokens.isEmpty()) {
            Set<String> pathMatches = null;
            for (String token : pathTokens) {
                pathMatches = intersect(pathMatches, QueryHandler.getPathsMatchingTokenWithTypos(token, "path"));
            }
            if (pathMatches == null) {
                pathMatches = Set.of();
            }
            candidates = intersect(candidates, pathMatches);
        }

        List<String> contentTokens = toQueryTokens(parsedQuery.getContentTerms());
        if (!contentTokens.isEmpty()) {
            Set<String> contentMatches = null;
            for (String token : contentTokens) {
                contentMatches = intersect(contentMatches, QueryHandler.getPathsMatchingTokenWithTypos(token, "content"));
            }
            if (contentMatches == null) {
                contentMatches = Set.of();
            }
            candidates = intersect(candidates, contentMatches);
        }

        List<String> freeTokens = toQueryTokens(parsedQuery.getFreeTerms());
        for (String freeToken : freeTokens) {
            Set<String> freeMatches = new HashSet<>();
            freeMatches.addAll(QueryHandler.getPathsMatchingTokenWithTypos(freeToken, "path"));
            freeMatches.addAll(QueryHandler.getPathsMatchingTokenWithTypos(freeToken, "content"));
            candidates = intersect(candidates, freeMatches);
        }

        return candidates != null ? candidates : Set.of();
    }

    private List<String> toQueryTokens(List<String> queryTerms) {
        List<String> tokens = new ArrayList<>();
        for (String term : queryTerms) {
            tokens.addAll(Tokenizer.tokenize(term));
        }
        return tokens;
    }

    private Set<String> intersect(Set<String> left, Set<String> right) {
        if (left == null) {
            return new HashSet<>(right);
        }
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return intersection;
    }

    private String buildSnippet(String content, String queryStr) {
        if (content == null || content.isBlank()) {
            return "";
        }

        int idx = content.toLowerCase(Locale.ROOT).indexOf(queryStr.toLowerCase(Locale.ROOT));
        if (idx != -1) {
            int start = Math.max(0, idx - 40);
            int end = Math.min(content.length(), idx + queryStr.length() + 80);
            return "..." + content.substring(start, end).replace('\n', ' ') + "...";
        }
        return content.substring(0, Math.min(content.length(), 100)).replace('\n', ' ') + "...";
    }

    private void applyRanking(List<SearchResult> results, RankingStrategy strategy, ParsedQuery parsedQuery) {
        Map<String, Integer> historyBoostMap = QueryHandler.getResultHitCounts(parsedQuery.getRawQuery());
        Comparator<SearchResult> comparator;
        if (strategy == RankingStrategy.ALPHABETICAL) {
            comparator = Comparator.comparing(result -> result.getPath().toLowerCase(Locale.ROOT));
        } else if (strategy == RankingStrategy.LAST_MODIFIED) {
            comparator = Comparator.comparingLong(SearchResult::getLastModified).reversed();
        } else {
            comparator = Comparator.comparingDouble((SearchResult result) ->
                    computePathScoreRankingValue(result, parsedQuery, historyBoostMap)).reversed();
        }
        results.sort(comparator);
    }

    private double computePathScoreRankingValue(SearchResult result, ParsedQuery parsedQuery, Map<String, Integer> historyBoostMap) {
        String pathLower = result.getPath().toLowerCase(Locale.ROOT);
        double value = result.getPathScore();

        int pathTermMatches = countMatches(pathLower, parsedQuery.getPathTerms());
        int freeTermMatches = countMatches(pathLower, parsedQuery.getFreeTerms());
        value += (pathTermMatches * 6.0) + (freeTermMatches * 3.0);

        long oneMonthMs = 30L * 24 * 60 * 60 * 1000;
        long ageMs = Math.max(0, System.currentTimeMillis() - result.getLastModified());
        double recencyBoost = Math.max(0.0, 1.0 - ((double) ageMs / oneMonthMs)) * 5.0;
        value += recencyBoost;


        int rawHistoryHits = historyBoostMap.getOrDefault(result.getPath(), 0);
        value += Math.min(rawHistoryHits, 10) * 0.4;
        return value;
    }

    private int countMatches(String haystack, List<String> terms) {
        int matches = 0;
        for (String term : terms) {
            if (haystack.contains(term.toLowerCase(Locale.ROOT))) {
                matches++;
            }
        }
        return matches;
    }

    public static class SearchResult {
        private static final DateTimeFormatter LAST_MODIFIED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        private final String path;
        private final String snippet;
        private final long lastModified;
        private final double pathScore;

        public SearchResult(String path, String snippet, long lastModified, double pathScore) {
            this.path = path;
            this.snippet = snippet;
            this.lastModified = lastModified;
            this.pathScore = pathScore;
        }

        public String getPath() { return path; }
        public String getSnippet() { return snippet; }
        public long getLastModified() { return lastModified; }
        public double getPathScore() { return pathScore; }

        public String getLastModifiedFormatted() {
            if (lastModified <= 0) {
                return "unknown";
            }
            return LAST_MODIFIED_FORMAT.format(Instant.ofEpochMilli(lastModified));
        }
        
        @Override
        public String toString() {
            return "File: " + path + "\nLast modified: " + getLastModifiedFormatted()
                    + "\nPathScore: " + String.format(Locale.ROOT, "%.2f", pathScore)
                    + "\nSnippet: " + snippet + "\n";
        }
    }
}
