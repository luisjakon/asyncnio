package async.nio.dispatchables;

import async.nio.channels.AsyncChannels.CompletionHandler;
import async.nio.channels.AsyncChannels.NetworkChannel;
import async.nio.channels.impl.AsyncChannelGroup;
import async.nio.channels.system.Futures.FutureCompletionHandler;
import async.nio.net.SocketOption;
import async.nio.net.SocketOptions;
import async.nio.net.StandardSocketOptions;
import async.nio.util.Nio.ByteBuffers;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DispatchableSocketChannel extends DispatchableChannelBase<SocketChannel> implements NetworkChannel {

    @SuppressWarnings("serial")
    private static final Set<SocketOption<?>> defaultSocketOptions = Collections.unmodifiableSet(new HashSet<SocketOption<?>>() {
        {
            add(StandardSocketOptions.SO_SNDBUF);
            add(StandardSocketOptions.SO_RCVBUF);
            add(StandardSocketOptions.SO_KEEPALIVE);
            add(StandardSocketOptions.SO_REUSEADDR);
            add(StandardSocketOptions.SO_TIMEOUT);
            add(StandardSocketOptions.TCP_NODELAY);
            add(StandardSocketOptions.SO_LINGER);
        }
    });

    public DispatchableSocketChannel(AsyncChannelGroup group, SocketChannel channel) throws IOException {
        super(group, channel);
    }

    protected Socket getSocket() throws IOException {
        return channel.socket();
    }

    protected SocketChannel getSocketChannel() throws IOException {
        return channel;
    }

    @Override
    protected boolean shouldDispatchConnect() {
        return channel.isConnectionPending();
    }

    @Override
    public void finishConnect() throws IOException {
        channel.finishConnect();
    }

    @Override
    public boolean isOpen() {
        return !shutdown || channel.isOpen();
    }

    @Override
    public NetworkChannel bind(SocketAddress local) throws IOException {
        if (shutdown || channel == null)
            throw new ClosedChannelException();

        channel.socket().bind(local);
        return this;
    }

    public Future<Void> connect(SocketAddress remote) {
        FutureCompletionHandler<Void> fut = new FutureCompletionHandler<Void>();
        connect(remote, null, fut);
        return fut;
    }

    public <Void, A> void connect(final SocketAddress remote, final A attachment, final CompletionHandler<Void, ? super A> handler) {
        Callable<Void> function = new Callable<Void>() {
            @Override
            public Void call() throws IOException {
                channel.connect(remote);
                return null;
            }
        };
        connect0(function, attachment, handler);
    }

    public Future<Integer> read(ByteBuffer dst) {
        FutureCompletionHandler<Integer> fut = new FutureCompletionHandler<Integer>();
        read(dst, 0, TimeUnit.MILLISECONDS, null, fut);
        return fut;
    }

    public <A> void read(final ByteBuffer dst, long timeout, TimeUnit unit, final A attachment, final CompletionHandler<Integer, ? super A> handler) {
        Callable<Integer> function = new Callable<Integer>() {
            @Override
            public Integer call() throws IOException {
                return channel.read(dst);
            }
        };
        read0(function, timeout, unit, attachment, handler, dst.remaining() == 0);
    }

    public <A> void read(final ByteBuffer[] dsts, final int offset, final int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        Callable<Long> function = new Callable<Long>() {
            @Override
            public Long call() throws IOException {
                return channel.read(dsts, offset, length);
            }
        };
        read0(function, timeout, unit, attachment, handler, ByteBuffers.remaining(dsts) == 0);
    }

    public Future<Integer> write(ByteBuffer src) {
        FutureCompletionHandler<Integer> fut = new FutureCompletionHandler<Integer>();
        write(src, 0, TimeUnit.MILLISECONDS, null, fut);
        return fut;
    }

    public <A> void write(final ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        Callable<Integer> function = new Callable<Integer>() {
            @Override
            public Integer call() throws IOException {
                return channel.write(src);
            }
        };
        write0(function, timeout, unit, attachment, handler);
    }

    public <A> void write(final ByteBuffer[] srcs, final int offset, final int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        Callable<Long> function = new Callable<Long>() {
            @Override
            public Long call() throws IOException {
                return channel.write(srcs, offset, length);
            }
        };
        write0(function, timeout, unit, attachment, handler);
    }

    public Future<Long> transferTo(long position, long count, WritableByteChannel target) {
        FutureCompletionHandler<Long> fut = new FutureCompletionHandler<Long>();
        transferTo(position, count, target, 0, TimeUnit.MILLISECONDS, null, fut);
        return fut;
    }

    public <A> void transferTo(final long position, final long count, final WritableByteChannel target, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        Callable<Number> function = new Callable<Number>() {
            @Override
            public Number call() throws IOException {
                return ((FileChannel) target).transferFrom(channel, position, count);
            }
        };
        read0(function, timeout, unit, attachment, handler, false);
    }

    public Future<Long> transferFrom(ReadableByteChannel target, long position, long count) {
        FutureCompletionHandler<Long> fut = new FutureCompletionHandler<Long>();
        transferFrom(target, position, count, 0, TimeUnit.MILLISECONDS, null, fut);
        return fut;
    }

    public <A> void transferFrom(final ReadableByteChannel target, final long position, final long count, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        Callable<Long> function = new Callable<Long>() {
            @Override
            public Long call() throws IOException {
                return ((FileChannel) target).transferTo(position, count, channel);
            }
        };
        write0(function, timeout, unit, attachment, handler);
    }

    @Override
    public <T> NetworkChannel setOption(SocketOption<T> name, T value) throws IOException {
        SocketOptions.setOption(channel, name, value);
        return this;
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return SocketOptions.getOption(channel, name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return defaultSocketOptions;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return channel.socket().getLocalSocketAddress();
    }

    public SocketAddress getRemoteAddress() throws IOException {
        return channel.socket().getRemoteSocketAddress();
    }

    public void shutdownInput() throws IOException {
        channel.socket().shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        channel.socket().shutdownOutput();
    }
}
