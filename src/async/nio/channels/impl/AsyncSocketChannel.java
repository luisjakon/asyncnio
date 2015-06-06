package async.nio.channels.impl;

import async.nio.channels.AsyncChannels.CompletionHandler;
import async.nio.channels.AsyncSocketChannels.AsynchronousSocketChannel;
import async.nio.dispatchables.DispatchableSocketChannel;
import async.nio.net.SocketOption;
import async.nio.net.SocketOptions;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AsyncSocketChannel extends AsynchronousSocketChannel {

    protected SocketChannel channel;
    protected DispatchableSocketChannel impl;

    public AsyncSocketChannel(AsyncChannelGroup group) throws IOException {
        this(group, SocketChannel.open());
    }

    public AsyncSocketChannel(AsyncChannelGroup grp, SocketChannel sc) throws IOException {
        super(grp.provider());
        channel = sc;
        impl = new DispatchableSocketChannel(grp, sc);
    }

    @Override
    protected Socket getSocket() throws IOException {
        return channel == null ? null : channel.socket();
    }

    @Override
    protected SocketChannel getSocketChannel() throws IOException {
        return channel;
    }

    @Override
    public AsynchronousSocketChannel bind(SocketAddress local) throws IOException {
        impl.bind(local);
        return this;
    }

    @Override
    public Future<Void> connect(SocketAddress remote) {
        return impl.connect(remote);
    }

    @Override
    public <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
        impl.connect(remote, attachment, handler);
    }

    @Override
    public Future<Integer> read(ByteBuffer dst) {
        return impl.read(dst);
    }

    @Override
    public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        impl.read(dst, timeout, unit, attachment, handler);
    }

    @Override
    public <A> void read(ByteBuffer[] dst, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        impl.read(dst, offset, length, timeout, unit, attachment, handler);
    }

    @Override
    public Future<Integer> write(ByteBuffer src) {
        return impl.write(src);
    }

    @Override
    public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
        impl.write(src, timeout, unit, attachment, handler);
    }

    @Override
    public <A> void write(ByteBuffer[] src, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        impl.write(src, offset, length, timeout, unit, attachment, handler);
    }

    @Override
    public Future<Long> transferTo(long position, long count, WritableByteChannel target) {
        return impl.transferFrom(channel, position, count);
    }

    @Override
    public <A> void transferTo(long position, long count, WritableByteChannel target, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        impl.transferTo(position, count, target, timeout, unit, attachment, handler);
    }

    @Override
    public Future<Long> transferFrom(ReadableByteChannel source, long position, long count) {
        return impl.transferFrom(source, position, count);
    }

    @Override
    public <A> void transferFrom(ReadableByteChannel source, long position, long count, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler) {
        impl.transferFrom(source, position, count, timeout, unit, attachment, handler);
    }

    @Override
    public AsynchronousSocketChannel shutdownInput() throws IOException {
        if (channel != null)
            channel.socket().shutdownInput();
        return this;
    }

    @Override
    public AsynchronousSocketChannel shutdownOutput() throws IOException {
        if (channel != null)
            channel.socket().shutdownOutput();
        return this;
    }

    @Override
    public boolean isOpen() {
        return impl != null && impl.isOpen();
    }

    @Override
    public synchronized void close() throws IOException {
        impl.close();
        channel = null;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return channel == null ? null : channel.socket().getLocalSocketAddress();
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return channel == null ? null : channel.socket().getRemoteSocketAddress();
    }

    @Override
    public <V> V getOption(SocketOption<V> name) throws IOException {
        return SocketOptions.getOption(channel, name);
    }

    @Override
    public <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        SocketOptions.setOption(channel, name, value);
        return this;
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return impl.supportedOptions();
    }
}
