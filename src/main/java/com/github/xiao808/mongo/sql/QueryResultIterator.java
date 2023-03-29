package com.github.xiao808.mongo.sql;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;

import java.util.Iterator;

/**
 * Wrapper {@link java.util.Iterator} around the {@link MongoCursor}.
 * @param <T> the type of elements returned by this iterator
 */
public class QueryResultIterator<T> implements Iterator<T>, AutoCloseable {

    private final MongoCursor<T> mongoCursor;

    /**
     * Default constructor.
     * @param mongoIterable the wrapped {@link MongoIterable}
     */
    public QueryResultIterator(final MongoIterable<T> mongoIterable) {
        this.mongoCursor = mongoIterable.iterator();
    }

    @Override
    public boolean hasNext() {
        return mongoCursor.hasNext();
    }

    @Override
    public T next() {
        return mongoCursor.next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        mongoCursor.close();
    }
}
