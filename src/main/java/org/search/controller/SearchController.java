package org.search.controller;

import org.search.application.SearchResult;
import org.search.application.SearchService;
import org.search.ranking.RankingStrategy;

import java.util.List;

public class SearchController {

    private final SearchService searchService;

    public SearchController() {
        this(new SearchService());
    }

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    public List<SearchResult> search(String queryStr, RankingStrategy rankingStrategy) {
        return searchService.search(queryStr, rankingStrategy);
    }

    public String describeParsedQuery(String queryStr) {
        return searchService.describeParsedQuery(queryStr);
    }

    public List<String> getSuggestions(String prefix) {
        return searchService.getSuggestions(prefix);
    }
}
