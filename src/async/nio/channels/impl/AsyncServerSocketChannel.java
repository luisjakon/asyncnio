package async.nio.channels.impl;

import async.nio.channels.AsyncChannels.CompletionHandler;
import async.nio.channels.AsyncSocketChannels.AsynchronousServerSocketChannel;
import async.nio.channels.AsyncSocketChannels.AsynchronousSocketChannel;
import async.nio.dispatchables.DispatchableServerSocketChannel;
import async.nio.net.SocketOption;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import java.util.concurrent.Future;

public class AsyncServerSocketChannel extends AsynchronousServerSocketChannel {

    protected ServerSocketChannel schannel;
    protected DispatchableServerSocketChannel impl;

    protected AsyncServerSocketChannel(AsyncChannelGroup group, ServerSocketChannel sc) throws IOException {
        super(group.provider());
        impl = new DispatchableServerSocketChannel(group, sc);
    }

    protected AsyncServerSocketChannel(AsyncChannelGroup group) throws IOException {
        this(group, ServerSocketChannel.open());
    }

    @Override
    public AsynchronousServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        impl.bind(local, backlog);
        return this;
    }

    @Override
    public <A> void accept(A attachment, CompletionHandler<AsynchronousSocketChannel, ? super A> handler) {
        impl.accept(attachment, handler);
    }

    @Override
    public Future<AsynchronousSocketChannel> accept() {
        return impl.accept();
    }

    @Override
    public ServerSocket socket() throws IOException {
        return impl.socket();
    }

    @Override
    public ServerSocketChannel socketChannel() throws IOException {
        return impl.socketChannel();
    }

    @Override
    public boolean isOpen() {
        return impl.isOpen();
    }

    @Override
    public void close() throws IOException {
        impl.close();
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return socket().getLocalSocketAddress();
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return impl.getOption(name);
    }

    @Override
    public <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        impl.setOption(name, value);
        return this;
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return impl.supportedOptions();
    }
}
