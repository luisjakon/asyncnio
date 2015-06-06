package async.nio.dispatchers;

import async.nio.dispatchables.DispatchableChannels.DispatchableChannel;
import async.nio.dispatchables.DispatchableChannels.DispatchableServerChannel;
import async.nio.dispatchables.DispatchableChannels.Dispatcher;
import async.nio.util.Logger;

import java.nio.channels.SelectionKey;

public class Dispatchers {

    public interface ChannelDispatcher<T> extends Dispatcher<T> {

        void wakeup();

        void shutdown();
    }

    public static class ClientChannelDispatcher extends SimpleChannelDispatcher<DispatchableChannel> {
        protected final Logger LOG = Logger.getLogger(getClass());

        public ClientChannelDispatcher(String name) {
            super(name);
        }

        @Override
        public void handleIO(DispatchableChannel channel, SelectionKey key) {

            if (!key.isValid())
                return;

            if (key.isWritable()) {
                int ops = key.interestOps() & ~SelectionKey.OP_WRITE;
                key.interestOps(ops);
                channel.interestOps(ops);
                channel.onWriteReady();
            }

            if (key.isConnectable()) {
                key.interestOps(0);
                channel.interestOps(0);
                channel.onConnectionReady();
                return;
            }

            if (key.isReadable()) {
                int ops = key.interestOps() & ~SelectionKey.OP_READ;
                key.interestOps(ops);
                channel.interestOps(ops);
                channel.onReadReady();
            }

        }
    }

    public static class ServerChannelDispatcher extends SimpleChannelDispatcher<DispatchableServerChannel> {
        protected final Logger LOG = Logger.getLogger(getClass());

        public ServerChannelDispatcher(String name) {
            super(name);
        }

        @Override
        public void handleIO(DispatchableServerChannel channel, SelectionKey key) {

            if (!key.isValid())
                return;

            if (key.isAcceptable()) {
                key.interestOps(0);
                channel.onAcceptReady();
            }
        }
    }

    private Dispatchers() {
    } // Singleton
}
