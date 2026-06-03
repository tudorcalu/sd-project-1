package org.search.search.query;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class SynonymDecorator extends QueryDecorator {
    private static final Map<String, String> SYNONYMS = new LinkedHashMap<>();

    static {
        SYNONYMS.put("img", "(img OR image OR photo)");
        SYNONYMS.put("pic", "(pic OR picture OR photo)");
        SYNONYMS.put("doc", "(doc OR document OR file)");
        SYNONYMS.put("txt", "(txt OR text)");
    }

    public SynonymDecorator(QueryBuilder delegate) {
        super(delegate);
    }

    @Override
    protected String transform(String query) {
        if (query.isEmpty()) {
            return query;
        }
        String[] tokens = query.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            String replacement = SYNONYMS.get(token.toLowerCase(Locale.ROOT));
            if (replacement == null) {
                replacement = token;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(replacement);
        }
        return sb.toString();
    }
}
