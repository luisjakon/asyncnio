package async.nio.channels.impl;

import java.util.concurrent.atomic.AtomicReference;

public class Events {

    public enum State {
        READY, PENDING, CANCELLED, // <= Normal States
        NOTIFICATION_READY, DONE,
        NOTIFICATION_FAILED, TIMEDOUT, UNKNOWN // <= Error States
    }

    public static abstract class BaseEvent {

        AtomicReference<State> state = new AtomicReference<State>(State.READY);

        public boolean compareAndSet(State expect, State update) {
            return state.compareAndSet(expect, update);
        }

        public void setState(State update) {
            state.set(update);
        }

        public State getState() {
            return state.get();
        }

        public void clear() {
            state.set(State.READY);
        }
    }

    public static abstract class PendingEvent<V> extends BaseEvent {

        public V result;
        public Throwable error;

        public void set(V result) {
            this.result = result;
        }

        public void setException(Throwable err) {
            this.error = err;
        }

        public void setReady() {
            setState(State.READY);
        }

        public void setPending() {
            setState(State.PENDING);
        }

        public void setTimedout() {
            setState(State.TIMEDOUT);
        }

        public boolean isReady() {
            return getState() == State.READY;
        }

        public boolean isPending() {
            return getState() == State.PENDING;
        }

        public boolean isTimedout() {
            return getState() == State.TIMEDOUT;
        }

        public boolean isCancelled() {
            return getState() == State.CANCELLED;
        }

        public boolean isDone() {
            return getState() == State.DONE;
        }

        public void cancel() {
            setState(State.CANCELLED);
        }

        public void done() {
            setState(State.DONE);
        }

        public void reset() {
            clear();
        }

        public void clear() {
            super.clear();
            result = null;
            error = null;
        }
    }

    private Events() {
    } // Singleton
}
