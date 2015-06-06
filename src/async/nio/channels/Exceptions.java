package async.nio.channels;

import java.io.IOException;

public class Exceptions {

    public static class AlreadyBoundException extends IllegalStateException {
    }

    public static class AcceptPendingException extends IllegalStateException {
    }

    public static class ReadPendingException extends IllegalStateException {
    }

    public static class WritePendingException extends IllegalStateException {
    }

    public static class InterruptedByTimeoutException extends IOException {
    }

    public static class ShutdownChannelGroupException extends IllegalStateException {
    }

    public static final class Rethrow {

        public static <E extends Exception> void as(Class<E> exceptionClass, Throwable t) throws E, Error, RuntimeException {
            throwUnchecked(t);

            if (exceptionClass.isInstance(t)) {
                throw exceptionClass.cast(t);
            }
        }

        public static void throwUnchecked(Throwable t) {
            if (t instanceof Error) {
                throw (Error) t;
            }

            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }

        private Rethrow() {
        }
    }

    private Exceptions() {
    } // Singleton
}
