package async.nio.dispatchables;

import async.nio.channels.AsyncChannels.CompletionHandler;
import async.nio.channels.impl.Events.PendingEvent;

import java.util.concurrent.Callable;

public class DispatchableChannelEvents {

    public static abstract class PendingConnectEvent<V> extends PendingChannelEvent<V> {
    }

    public static abstract class PendingAcceptEvent<V> extends PendingChannelEvent<V> {
    }

    public static abstract class PendingIOEvent<V> extends PendingChannelEvent<V> {
    }

    @SuppressWarnings("rawtypes")
    public static abstract class PendingChannelEvent<V> extends PendingEvent<V> {

        public Callable<V> function;
        public Object attachment;
        public CompletionHandler handler;

        public PendingChannelEvent() {
            super();
        }

        public V whenReady() throws Exception {
            return function.call();
        }

        public void clear() {
            super.clear();
            function = null;
            attachment = null;
            handler = null;
        }
    }

    private DispatchableChannelEvents() {
    } // Singleton
}
