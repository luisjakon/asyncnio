package async.nio.dispatchables;

import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public class DispatchableChannels {

    public static class InterestOps {
        public final static int OP_ACCEPT = SelectionKey.OP_ACCEPT;
        public final static int OP_CONNECT = SelectionKey.OP_CONNECT;
        public final static int OP_READ = SelectionKey.OP_READ;
        public final static int OP_WRITE = SelectionKey.OP_WRITE;
        public final static int OP_CLOSE = SelectionKey.OP_ACCEPT << 1;
    }

    public interface Dispatchable {

        SelectableChannel getSelectableChannel();

        void interestOps(int ops);

        int interestOps();

        void onConnectionClosed();
    }

    public interface DispatchableChannel<T> extends Dispatchable, Channel {

        void onConnectionReady();

        void onReadReady();

        void onWriteReady();
    }

    public interface DispatchableServerChannel<T> extends Dispatchable, Channel {

        void onAcceptReady();
    }

    private DispatchableChannels() {
    } // Singleton
}
