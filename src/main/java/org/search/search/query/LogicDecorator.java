package org.search.search.query;

import java.util.Locale;

public class LogicDecorator extends QueryDecorator {
    public LogicDecorator(QueryBuilder delegate) {
        super(delegate);
    }

    @Override
    protected String transform(String query) {
        if (query.isEmpty()) {
            return query;
        }
        StringBuilder sb = new StringBuilder();
        for (String token : query.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (token.equalsIgnoreCase("OR")) {
                sb.append(" OR ");
                continue;
            }
            if (token.startsWith("(") && token.endsWith(")")) {
                appendGroup(sb, token.substring(1, token.length() - 1));
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(addWildcard(token));
        }
        return sb.toString().trim();
    }

    private void appendGroup(StringBuilder sb, String inner) {
        sb.append('(');
        String[] parts = inner.split("(?i)\\s+OR\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append(addWildcard(parts[i].trim()));
        }
        sb.append(')');
    }

    private static String addWildcard(String token) {
        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.endsWith("*") || trimmed.contains("\"")) {
            return trimmed;
        }
        if (trimmed.matches("[A-Za-z0-9_\\-]+")) {
            return trimmed + "*";
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
