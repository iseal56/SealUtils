package dev.iseal.sealUtils.utils;

public class Pair<T, U> {
    private final T first;
    private final U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", first-type=" + first.getClass().getSimpleName() +
                ", second=" + second +
                ", second-type=" + second.getClass().getSimpleName() +
                '}';
    }
}