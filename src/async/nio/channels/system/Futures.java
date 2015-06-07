package async.nio.channels.system;

import async.nio.channels.AsyncChannels.CompletionHandler;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class Futures {

    public static final Callable<Object> NOOP = new Callable<Object>() {
        @Override
        public Object call() throws Exception {
            return null;
        }
    };

    public static class FutureResult<T> extends FutureTask {

        public FutureResult() {
            super(NOOP);
        }

        public FutureResult(Throwable t) {
            super(NOOP);
            setException(t);
        }

        public FutureResult(Object v) {
            super(NOOP);
            set(v);
        }

        @Override
        public final void set(Object v) {
            super.set(v);
        }

        @Override
        public final void setException(Throwable t) {
            super.setException(t);
        }
    }

    public static class FutureCompletionHandler<T> extends FutureResult<T> implements CompletionHandler<T, Void> {

        @Override
        public void completed(T result, Void attachment) {
            super.set(result);
        }

        @Override
        public void failed(Throwable exc, Void attachment) {
            super.setException(exc);
        }
    }

    private Futures() {
    } // Singleton
}
