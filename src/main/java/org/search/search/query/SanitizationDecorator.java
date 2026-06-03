package org.search.search.query;

public class SanitizationDecorator extends QueryDecorator {
    public SanitizationDecorator(QueryBuilder delegate) {
        super(delegate);
    }

    @Override
    protected String transform(String query) {
        if (query.isEmpty()) {
            return query;
        }
        return query
                .replace("\"", " ")
                .replace("'", " ")
                .replace("(", " ")
                .replace(")", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
