package java.util.concurrent.atomic;

import java.io.Serializable;

/** See {@link AtomicIntegerArray} — same reason, long-valued. */
public class AtomicLongArray implements Serializable {

    private final long[] array;

    public AtomicLongArray(int length) {
        array = new long[length];
    }

    public AtomicLongArray(long[] initial) {
        array = initial.clone();
    }

    public final int length() {
        return array.length;
    }

    public final long get(int i) {
        return array[i];
    }

    public final void set(int i, long value) {
        array[i] = value;
    }

    public final void lazySet(int i, long value) {
        array[i] = value;
    }

    public final long getAndSet(int i, long value) {
        long old = array[i];
        array[i] = value;
        return old;
    }

    public final boolean compareAndSet(int i, long expect, long update) {
        if (array[i] == expect) {
            array[i] = update;
            return true;
        }
        return false;
    }

    public final boolean weakCompareAndSet(int i, long expect, long update) {
        return compareAndSet(i, expect, update);
    }

    public final long getAndIncrement(int i) {
        return array[i]++;
    }

    public final long getAndDecrement(int i) {
        return array[i]--;
    }

    public final long getAndAdd(int i, long delta) {
        long old = array[i];
        array[i] = old + delta;
        return old;
    }

    public final long incrementAndGet(int i) {
        return ++array[i];
    }

    public final long decrementAndGet(int i) {
        return --array[i];
    }

    public final long addAndGet(int i, long delta) {
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
