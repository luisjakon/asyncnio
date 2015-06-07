package async.nio.dispatchers;

import async.nio.dispatchables.DispatchableChannels;
import async.nio.util.Logger;

import java.nio.channels.SelectionKey;

public class ClientChannelDispatcher extends SimpleChannelDispatcher<DispatchableChannels.DispatchableChannel> {

    protected final Logger LOG = Logger.getLogger(getClass());

    public ClientChannelDispatcher(String name) {
        super(name);
    }

    @Override
    public void handleIO(DispatchableChannels.DispatchableChannel channel, SelectionKey key) {

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