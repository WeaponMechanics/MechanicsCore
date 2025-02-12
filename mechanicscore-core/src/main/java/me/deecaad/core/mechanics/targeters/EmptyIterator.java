package me.deecaad.core.mechanics.targeters;

import java.util.Iterator;

/**
 * An iterator that has no elements.
 *
 * <p>Do not use this class directly. Instead, use {@link #emptyIterator()}.
 *
 * @param <E> the type of elements in the iterator
 */
public final class EmptyIterator<E> implements Iterator<E> {

    private static final EmptyIterator<?> INSTANCE = new EmptyIterator<>();

    private EmptyIterator() {
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public E next() {
        throw new IllegalStateException();
    }

    /**
     * Returns an empty iterator.
     *
     * @return an empty iterator
     * @param <T> the type of elements in the iterator
     */
    @SuppressWarnings("unchecked")
    public static <T> EmptyIterator<T> emptyIterator() {
        return (EmptyIterator<T>) INSTANCE;
    }
}
