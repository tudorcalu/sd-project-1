package org.search.search.query;

public class BaseQueryBuilder implements QueryBuilder {
    @Override
    public String build(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        return rawQuery.trim();
    }
}
