package org.search.parser;

import java.util.Collections;
import java.util.List;

public class ParsedQuery {
    private final List<String> pathTerms;
    private final List<String> contentTerms;
    private final List<String> freeTerms;
    private final List<String> colorTerms;
    private final String rawQuery;

    public ParsedQuery(List<String> pathTerms, List<String> contentTerms, List<String> freeTerms, String rawQuery) {
        this(pathTerms, contentTerms, freeTerms, List.of(), rawQuery);
    }

    public ParsedQuery(
            List<String> pathTerms,
            List<String> contentTerms,
            List<String> freeTerms,
            List<String> colorTerms,
            String rawQuery) {
        this.pathTerms = List.copyOf(pathTerms);
        this.contentTerms = List.copyOf(contentTerms);
        this.freeTerms = List.copyOf(freeTerms);
        this.colorTerms = List.copyOf(colorTerms);
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

    public List<String> getColorTerms() {
        return Collections.unmodifiableList(colorTerms);
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public boolean hasQualifiers() {
        return !pathTerms.isEmpty() || !contentTerms.isEmpty() || !colorTerms.isEmpty();
    }

    public boolean hasFtsTerms() {
        return !freeTerms.isEmpty() || !contentTerms.isEmpty();
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
        if (!colorTerms.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("color terms: ").append(colorTerms);
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
