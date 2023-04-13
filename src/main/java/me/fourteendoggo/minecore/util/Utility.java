package me.fourteendoggo.minecore.util;

public class Utility {

    public static <T extends AutoCloseable & Iterable<E>, E> void loopResource(T resource, ThrowingConsumer<E> consumer) {
        try (resource) {
            for (E element : resource) {
                consumer.accept(element);
            }
        } catch (Exception e) {
            sneakyThrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <E extends Exception> void sneakyThrow(Exception e) throws E {
        throw (E) e;
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }
}
