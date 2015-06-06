package async.nio.util;

import java.util.ArrayList;

public class CircularList<T> extends ArrayList<T> {

    private volatile int idx = 0;

    public CircularList(int size) {
        super(size);
    }

    public T next() {
        return get(idx++ % size());
    }

    public T previous() {
        return get(idx-- % size());
    }
}
