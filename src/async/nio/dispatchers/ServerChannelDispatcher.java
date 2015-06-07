package async.nio.dispatchers;

import async.nio.dispatchables.DispatchableChannels;
import async.nio.util.Logger;

import java.nio.channels.SelectionKey;

public class ServerChannelDispatcher extends SimpleChannelDispatcher<DispatchableChannels.DispatchableServerChannel> {

    protected final Logger LOG = Logger.getLogger(getClass());

    public ServerChannelDispatcher(String name) {
        super(name);
    }

    @Override
    public void handleIO(DispatchableChannels.DispatchableServerChannel channel, SelectionKey key) {

        if (!key.isValid())
            return;

        if (key.isAcceptable()) {
            key.interestOps(0);
            channel.onAcceptReady();
        }
    }
}