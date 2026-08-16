package java.util.concurrent.atomic;

import java.io.Serializable;

/**
 * Supplied for the wasm build only: TeaVM's class library omits the atomic array
 * types, and Gson's TypeAdapters static initialiser references them, which makes
 * the whole of Gson unreachable without this.
 *
 * The browser runs a single thread, so plain array access already has the
 * atomicity the contract promises.
 */
public class AtomicIntegerArray implements Serializable {

    private final int[] array;

    public AtomicIntegerArray(int length) {
        array = new int[length];
    }

    public AtomicIntegerArray(int[] initial) {
        array = initial.clone();
    }

    public final int length() {
        return array.length;
    }

    public final int get(int i) {
        return array[i];
    }

    public final void set(int i, int value) {
        array[i] = value;
    }

    public final void lazySet(int i, int value) {
        array[i] = value;
    }

    public final int getAndSet(int i, int value) {
        int old = array[i];
        array[i] = value;
        return old;
    }

    public final boolean compareAndSet(int i, int expect, int update) {
        if (array[i] == expect) {
            array[i] = update;
            return true;
        }
        return false;
    }

    public final boolean weakCompareAndSet(int i, int expect, int update) {
        return compareAndSet(i, expect, update);
    }

    public final int getAndIncrement(int i) {
        return array[i]++;
    }

    public final int getAndDecrement(int i) {
        return array[i]--;
    }

    public final int getAndAdd(int i, int delta) {
        int old = array[i];
        array[i] = old + delta;
        return old;
    }

    public final int incrementAndGet(int i) {
        return ++array[i];
    }

    public final int decrementAndGet(int i) {
        return --array[i];
    }

    public final int addAndGet(int i, int delta) {
        array[i] += delta;
        return array[i];
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(array[i]);
        }
        return b.append(']').toString();
    }
}
