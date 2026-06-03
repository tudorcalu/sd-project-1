package org.search.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QueryParser {

    private static final String PATH_PREFIX = "path:";
    private static final String CONTENT_PREFIX = "content:";
    private static final String COLOR_PREFIX = "color:";

    public ParsedQuery parse(String query) {
        if (query == null || query.isBlank()) {
            return new ParsedQuery(List.of(), List.of(), List.of(), List.of(), "");
        }

        List<String> pathTerms = new ArrayList<>();
        List<String> contentTerms = new ArrayList<>();
        List<String> freeTerms = new ArrayList<>();
        List<String> colorTerms = new ArrayList<>();
        List<String> tokens = tokenize(query.trim());

        String pendingQualifier = null;
        for (String token : tokens) {
            String normalized = token.trim();
            if (normalized.isEmpty()) {
                continue;
            }

            String lowerToken = normalized.toLowerCase(Locale.ROOT);
            if (isQualifierToken(lowerToken)) {
                pendingQualifier = lowerToken;
                continue;
            }

            if (lowerToken.startsWith(PATH_PREFIX)) {
                addQualifierValue(pathTerms, normalized.substring(PATH_PREFIX.length()));
                pendingQualifier = null;
                continue;
            }
            if (lowerToken.startsWith(CONTENT_PREFIX)) {
                addQualifierValue(contentTerms, normalized.substring(CONTENT_PREFIX.length()));
                pendingQualifier = null;
                continue;
            }
            if (lowerToken.startsWith(COLOR_PREFIX)) {
                addQualifierValue(colorTerms, normalized.substring(COLOR_PREFIX.length()));
                pendingQualifier = null;
                continue;
            }

            if (pendingQualifier != null) {
                if (pendingQualifier.equals(PATH_PREFIX)) {
                    addQualifierValue(pathTerms, normalized);
                } else if (pendingQualifier.equals(CONTENT_PREFIX)) {
                    addQualifierValue(contentTerms, normalized);
                } else {
                    addQualifierValue(colorTerms, normalized);
                }
                pendingQualifier = null;
            } else {
                freeTerms.add(normalized);
            }
        }

        return new ParsedQuery(pathTerms, contentTerms, freeTerms, colorTerms, query.trim());
    }

    private static boolean isQualifierToken(String lowerToken) {
        return lowerToken.equals(PATH_PREFIX)
                || lowerToken.equals(CONTENT_PREFIX)
                || lowerToken.equals(COLOR_PREFIX);
    }

    private void addQualifierValue(List<String> target, String rawValue) {
        String value = stripWrappingQuotes(rawValue.trim());
        if (!value.isEmpty()) {
            target.add(value);
        }
    }

    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c == '"' || c == '\'') && !isEscaped(input, i)) {
                if (inQuotes && c == quoteChar) {
                    inQuotes = false;
                } else if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else {
                    current.append(c);
                }
                continue;
            }

            if (!inQuotes && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private boolean isEscaped(String input, int index) {
        int slashCount = 0;
        for (int i = index - 1; i >= 0 && input.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 != 0;
    }

    private String stripWrappingQuotes(String value) {
        if (value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }
}
