package org.search.search.query;

public abstract class QueryDecorator implements QueryBuilder {
    protected final QueryBuilder delegate;

    protected QueryDecorator(QueryBuilder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String build(String rawQuery) {
        return transform(delegate.build(rawQuery));
    }

    protected abstract String transform(String query);
}
