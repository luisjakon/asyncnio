package async.nio.channels.system;

import async.nio.channels.AsyncChannels.CompletionHandler;
import async.nio.channels.Exceptions.InterruptedByTimeoutException;
import async.nio.channels.system.Events.PendingEvent;
import async.nio.channels.system.Events.State;

import java.util.concurrent.ExecutorService;

public class Notifications {

    public static <V, A> void send(ExecutorService notifier, PendingEvent<V> event, A attachment, CompletionHandler<V, A> handler) {
        notifier.execute(handler(event, attachment, handler));
    }

    private static <V, A> Runnable handler(final PendingEvent<V> event, final A attachment, final CompletionHandler<V, A> handler) {
        return new Runnable() {
            public void run() {
                handle(event, attachment, handler);
            }
        };
    }

    private static <V, A> void handle(final PendingEvent<V> event, final A attachment, final CompletionHandler<V, A> handler) {
        try {

            Throwable error = event.error;
            V result = event.result;

            switch (event.getState()) {
                case TIMEDOUT:
                    event.clear();
                    event.setTimedout();
                    handler.failed(new InterruptedByTimeoutException(), attachment);
                    return;

                case NOTIFICATION_READY:
                    event.clear();
                    if (error != null) {
                        handler.failed(error, attachment);
                    } else {
                        handler.completed(result, attachment);
                    }
                    return;

                default:
                    // FIX ME: Should other states be
                    // reported as an error condition??
                    event.clear();
                    event.setState(State.UNKNOWN);
                    return;
            }
        } catch (Exception e) {
            // This should protect against rogue user code or
            // missing arguments
            if (event != null) {
                event.clear();
            }
            e.printStackTrace();
        }
    }

    private Notifications() {
    } // Singleton
}
