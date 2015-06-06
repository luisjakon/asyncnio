package async.nio.channels;

import async.nio.channels.AsyncChannels.*;
import async.nio.net.SocketOption;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AsyncSocketChannels {

    /**
     * JDK7+ compatible AsynchronousSocketChannel abstract implementation
     */
    public static abstract class AsynchronousSocketChannel implements AsynchronousByteChannel, NetworkChannel {

        private final AsynchronousChannelProvider provider;

        protected AsynchronousSocketChannel(AsynchronousChannelProvider provider) throws IOException {
            this.provider = provider;
        }

        public final AsynchronousChannelProvider provider() {
            return provider;
        }

        public static synchronized AsynchronousSocketChannel open(AsyncChannels.AsynchronousChannelGroup group) throws IOException {
            AsynchronousChannelProvider provider = (group == null) ? AsynchronousChannelProvider.provider() : group.provider();
            return provider.openAsynchronousSocketChannel(group);
        }

        public static AsynchronousSocketChannel open() throws IOException {
            return open(null);
        }

        public abstract AsynchronousSocketChannel bind(SocketAddress local) throws IOException;

        public abstract <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException;

        public abstract AsynchronousSocketChannel shutdownInput() throws IOException;

        public abstract AsynchronousSocketChannel shutdownOutput() throws IOException;

        public abstract SocketAddress getRemoteAddress() throws IOException;

        public abstract Future<Void> connect(SocketAddress remote);

        public abstract <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler);

        public abstract Future<Integer> read(ByteBuffer dst);

        public final <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler) {
            read(dst, 0L, TimeUnit.MILLISECONDS, attachment, handler);
        }

        public abstract <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler);

        public abstract <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler);

        public abstract Future<Integer> write(ByteBuffer src);

        public final <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler) {
            write(src, 0L, TimeUnit.MILLISECONDS, attachment, handler);
        }

        public abstract <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler);

        public abstract <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler);

        public abstract Future<Long> transferTo(long position, long count, WritableByteChannel target);

        public <A> void transferTo(long position, long count, WritableByteChannel target, A attachment, CompletionHandler<Long, ? super A> handler) {
            transferTo(position, count, target, 0, TimeUnit.MILLISECONDS, attachment, handler);
        }

        public abstract <A> void transferTo(long position, long count, WritableByteChannel target, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler);

        public abstract Future<Long> transferFrom(ReadableByteChannel target, long position, long count);

        public <A> void transferFrom(ReadableByteChannel target, long position, long count, A attachment, CompletionHandler<Long, ? super A> handler) {
            transferFrom(target, position, count, 0, TimeUnit.MILLISECONDS, attachment, handler);
        }

        public abstract <A> void transferFrom(ReadableByteChannel target, long position, long count, long timeout, TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler);

        protected abstract Socket getSocket() throws IOException;

        protected abstract SocketChannel getSocketChannel() throws IOException;
    }

    /**
     * JDK7+ compatible AsynchronousServerSocketChannel abstract
     * implementation
     */
    public static abstract class AsynchronousServerSocketChannel implements AsynchronousChannel, NetworkChannel {

        private final AsyncChannels.AsynchronousChannelProvider provider;

        protected AsynchronousServerSocketChannel() {
            this.provider = AsyncChannels.AsynchronousChannelProvider.provider();
        }

        protected AsynchronousServerSocketChannel(AsyncChannels.AsynchronousChannelProvider provider) {
            this.provider = provider;
        }

        public final AsyncChannels.AsynchronousChannelProvider provider() {
            return provider;
        }

        public static AsynchronousServerSocketChannel open(AsynchronousChannelGroup group) throws IOException {
            AsynchronousChannelProvider provider = (group == null) ? AsynchronousChannelProvider.provider() : group.provider();
            return provider.openAsynchronousServerSocketChannel(group);
        }

        public static AsynchronousServerSocketChannel open() throws IOException {
            return open(null);
        }

        public final AsynchronousServerSocketChannel bind(SocketAddress local) throws IOException {
            return bind(local, 32767); // TODO: hmmm...should we use backlogs?? (32767 <= OSX MAX BACKLOG)
        }

        public abstract AsynchronousServerSocketChannel bind(SocketAddress local, int backlog) throws IOException;

        public abstract <A> void accept(A attachment, CompletionHandler<AsynchronousSocketChannel, ? super A> handler);

        public abstract Future<AsynchronousSocketChannel> accept();

        public abstract ServerSocket socket() throws IOException;

        public abstract ServerSocketChannel socketChannel() throws IOException;

        public abstract <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException;
    }

    private AsyncSocketChannels() {
    }   // Singleton
}
