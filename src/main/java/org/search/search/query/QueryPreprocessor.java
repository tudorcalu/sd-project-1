package org.search.search.query;

public final class QueryPreprocessor {
    private final QueryBuilder pipeline;

    public QueryPreprocessor(QueryBuilder pipeline) {
        this.pipeline = pipeline;
    }

    public static QueryPreprocessor createDefaultPipeline() {
        QueryBuilder pipeline = new LogicDecorator(
                new SynonymDecorator(
                        new SanitizationDecorator(
                                new BaseQueryBuilder())));
        return new QueryPreprocessor(pipeline);
    }

    public String preprocess(String rawQuery) {
        return pipeline.build(rawQuery);
    }
}
