package async.nio.dispatchables;

import async.nio.channels.AsyncChannels.CompletionHandler;
import async.nio.channels.AsyncChannels.NetworkChannel;
import async.nio.channels.AsyncSocketChannels.AsynchronousSocketChannel;
import async.nio.channels.Exceptions.AcceptPendingException;
import async.nio.channels.impl.AsyncChannelGroup;
import async.nio.channels.impl.AsyncSocketChannel;
import async.nio.channels.impl.Defaults;
import async.nio.channels.impl.Events.State;
import async.nio.channels.impl.Futures.FutureCompletionHandler;
import async.nio.dispatchables.DispatchableChannelEvents.PendingAcceptEvent;
import async.nio.dispatchables.DispatchableChannels.DispatchableServerChannel;
import async.nio.dispatchables.DispatchableChannels.InterestOps;
import async.nio.dispatchers.Dispatchers.ServerChannelDispatcher;
import async.nio.net.SocketOption;
import async.nio.net.SocketOptions;
import async.nio.net.StandardSocketOptions;
import async.nio.util.CircularList;
import async.nio.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

public class DispatchableServerSocketChannel implements DispatchableServerChannel, NetworkChannel {

    public static final int DISPATCHERS = 10;
    protected static final Logger LOG = Logger.getLogger(DispatchableServerSocketChannel.class);// .setDebug(true);

    protected final ServerSocketChannel schannel;
    protected final AsyncChannelGroup group;

    protected Dispatchers<ServerChannelDispatcher> dispatchers;
    protected PendingAcceptEvent<AsynchronousSocketChannel> accept;

    protected volatile int interests;
    protected volatile Boolean shutdown = false;

    public DispatchableServerSocketChannel(AsyncChannelGroup group, ServerSocketChannel sc) throws IOException {
        if (sc == null) {
            throw new IOException("Missing channel: " + sc);
        }

        this.group = (group == null) ? Defaults.defaultGroup() : group;

        this.schannel = sc;
        this.schannel.configureBlocking(false);
        this.schannel.socket().setReuseAddress(true);

        this.dispatchers = new Dispatchers<ServerChannelDispatcher>(DISPATCHERS);
        this.accept = new PendingAcceptEvent<AsynchronousSocketChannel>() {
        };
    }

    protected AsyncChannelGroup group() {
        return group;
    }

    protected boolean isAcceptPending() {
        return accept.isPending();
    }


    @Override
    public SelectableChannel getSelectableChannel() {
        return schannel;
    }

    @Override
    public void interestOps(int ops) {
        interests = ops;
    }

    @Override
    public int interestOps() {
        return interests;
    }


    @Override
    public NetworkChannel bind(SocketAddress local) throws IOException {
        return bind(local, 0);
    }

    public NetworkChannel bind(SocketAddress local, int backlog) throws IOException {
        schannel.socket().bind(local, backlog);
        dispatchers.start(group);
        return this;
    }

    public Future<AsynchronousSocketChannel> accept() {
        // Create future accept response
        FutureCompletionHandler<AsynchronousSocketChannel> fut;

        // Register accept interest with dispatchers
        accept(null, fut = new FutureCompletionHandler<AsynchronousSocketChannel>());

        // Return accept future to caller
        return fut;
    }

    public <A> void accept(A attachment, CompletionHandler<AsynchronousSocketChannel, ? super A> handler) {
        if (!accept.compareAndSet(State.READY, State.PENDING)) {
            throw new AcceptPendingException();
        }

        // Setup acceptable parameters
        accept.attachment = attachment;
        accept.handler = handler;

        // Register accept interest with dispatchers
        dispatchers.accept(this);
    }

    public ServerSocket socket() throws IOException {
        return schannel.socket();
    }

    public ServerSocketChannel socketChannel() throws IOException {
        return schannel;
    }

    public SocketAddress getLocalAddress() throws IOException {
        return schannel.socket().getLocalSocketAddress();
    }

    public boolean isOpen() {
        return shutdown || schannel.isOpen();
    }

    @Override
    public void onAcceptReady() {

        // Check false triggers (e.g. no accept calls pending)
        if (!accept.isPending()) {
            return;
        }

        // Set event notification ready status
        accept.setState(State.NOTIFICATION_READY);

        try {
            // Listen for incoming connections
            SocketChannel ch = schannel.accept();

            // False nio trigger.  Do nothing & wait for next incoming connection...
            if (ch == null) {
                return;
            }

            // Create channel & notify caller
            AsyncSocketChannel channel = new AsyncSocketChannel(group, ch);
            group.notify(accept, channel);

            // Clean up - (GC cleanliness)
            channel = null;
            ch = null;
        } catch (Exception e) {
            group.notifyError(accept, e);
        }
    }

    @Override
    public void onConnectionClosed() {
        group.notifyError(accept, new AsynchronousCloseException());
        dispatchers.shutdown();
        group.shutdown();
    }

    public void close() throws IOException {
        close(new AsynchronousCloseException());
    }

    public void close(Exception ex) {
        synchronized (this) {
            if (shutdown) {
                return;
            }
            shutdown = true;
        }

        // Let dispatchers know we are closing this channel
        dispatchers.close(this);
    }

    public boolean isClosing() {
        return shutdown;
    }

    public <T> NetworkChannel setOption(SocketOption<T> name, T value) throws IOException {
        SocketOptions.setServerOption(schannel, name, value);
        return this;
    }

    public <T> T getOption(SocketOption<T> name) throws IOException {
        return SocketOptions.getServerOption(schannel, name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return supportedOptions;
    }

    @SuppressWarnings("serial")
    protected static final Set<SocketOption<?>> supportedOptions = Collections.unmodifiableSet(new HashSet<SocketOption<?>>() {
        {
            add(StandardSocketOptions.SO_RCVBUF);
            add(StandardSocketOptions.SO_REUSEADDR);
            add(StandardSocketOptions.SO_TIMEOUT);
            add(StandardSocketOptions.TCP_NODELAY);
        }
    });

    /**
     * Server Dispatchers Helper Class
     */
    private class Dispatchers<T extends ServerChannelDispatcher> {
        CircularList<T> dispatchers;

        public Dispatchers(int size) {
            dispatchers = new CircularList<T>(size);
        }

        public T next() {
            return dispatchers.next();
        }

        public void start(AsyncChannelGroup group) {
            // Initialize event dispatchers
            for (int i = 0; i < DISPATCHERS; i++) {
                dispatchers.add((T) new ServerChannelDispatcher(
                        "AsyncServerChannelDispatcher: [ ChannelGroup: \"#" + group.hashCode()
                                + "\", Server: \"#" + this.hashCode() + "-" + i + "\" ]"));
            }

            // Activate event dispatchers
            for (T d : dispatchers) {
                new Thread(d, d.name()) {{
                    setDaemon(true);
                }}.start();
            }
        }

        public void accept(DispatchableServerChannel channel) {
            // Let dispatchers know we're ready and interested in accepting connections
            channel.interestOps(InterestOps.OP_ACCEPT);

            // Pick an event dispatcher to handle accepted connections
            dispatchers.next().registerInterestFor(channel);
        }

        public void close(DispatchableServerChannel channel) {
            // Let dispatchers know we're ready to close
            channel.interestOps(InterestOps.OP_CLOSE);

            // Force dispatcher wakeup to proceed with close request
            for (T d : dispatchers) {
                d.registerInterestFor(channel);
                d.wakeup();
            }
        }

        public void shutdown() {
            // Shutdown registered dispatchers
            for (T d : dispatchers) {
                d.shutdown();
            }

            // Clear dispatcher list
            dispatchers.clear();
        }
    }
}


