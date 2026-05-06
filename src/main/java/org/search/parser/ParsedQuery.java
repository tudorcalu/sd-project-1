package org.search.parser;

import java.util.Collections;
import java.util.List;

public class ParsedQuery {
    private final List<String> pathTerms;
    private final List<String> contentTerms;
    private final List<String> freeTerms;
    private final String rawQuery;

    public ParsedQuery(List<String> pathTerms, List<String> contentTerms, List<String> freeTerms, String rawQuery) {
        this.pathTerms = List.copyOf(pathTerms);
        this.contentTerms = List.copyOf(contentTerms);
        this.freeTerms = List.copyOf(freeTerms);
        this.rawQuery = rawQuery;
    }

    public List<String> getPathTerms() {
        return Collections.unmodifiableList(pathTerms);
    }

    public List<String> getContentTerms() {
        return Collections.unmodifiableList(contentTerms);
    }

    public List<String> getFreeTerms() {
        return Collections.unmodifiableList(freeTerms);
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public boolean hasQualifiers() {
        return !pathTerms.isEmpty() || !contentTerms.isEmpty();
    }

    public String describeForUi() {
        StringBuilder sb = new StringBuilder();
        if (!pathTerms.isEmpty()) {
            sb.append("path terms: ").append(pathTerms);
        }
        if (!contentTerms.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("content terms: ").append(contentTerms);
        }
        if (!freeTerms.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("free terms: ").append(freeTerms);
        }
        if (sb.length() == 0) {
            return "(empty)";
        }
        return sb.toString();
    }
}
