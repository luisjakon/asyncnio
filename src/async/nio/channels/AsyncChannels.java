package async.nio.channels;

import async.nio.channels.AsyncSocketChannels.AsynchronousServerSocketChannel;
import async.nio.channels.AsyncSocketChannels.AsynchronousSocketChannel;
import async.nio.channels.impl.Defaults;
import async.nio.net.SocketOption;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.Set;
import java.util.concurrent.*;

public class AsyncChannels {

    public interface CompletionHandler<V, A> {

        void completed(V result, A attachment);

        void failed(Throwable exc, A attachment);
    }

    public interface AsynchronousChannel extends Channel {
    }

    public interface AsynchronousByteChannel extends AsynchronousChannel {

        Future<Integer> read(ByteBuffer dst);

        <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler);

        Future<Integer> write(ByteBuffer src);

        <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler);
    }

    public interface NetworkChannel extends Channel {

        NetworkChannel bind(SocketAddress local) throws IOException;

        SocketAddress getLocalAddress() throws IOException;

        <T> NetworkChannel setOption(SocketOption<T> name, T value) throws IOException;

        <T> T getOption(SocketOption<T> name) throws IOException;

        Set<SocketOption<?>> supportedOptions();
    }

    public static abstract class AsynchronousChannelProvider {

        protected AsynchronousChannelProvider() {
        }

        public abstract AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads, ThreadFactory threadFactory)
                throws IOException;

        public abstract AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor, int initialSize)
                throws IOException;

        public abstract AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group) throws IOException;

        public abstract AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(AsynchronousChannelGroup group)
                throws IOException;

        public static AsynchronousChannelProvider provider() {
            return Defaults.defaultProvider();
        }
    }

    public static abstract class AsynchronousChannelGroup {

        private final AsynchronousChannelProvider provider;

        protected AsynchronousChannelGroup(AsynchronousChannelProvider provider) {
            if (provider == null)
                throw new IllegalArgumentException("Provider cannot not be null.");
            this.provider = provider;
        }

        public static AsynchronousChannelGroup withFixedThreadPool(int nThreads, ThreadFactory threadFactory) throws IOException {
            return AsynchronousChannelProvider.provider().openAsynchronousChannelGroup(
                    Executors.newFixedThreadPool(nThreads, threadFactory), nThreads);
        }

        public static AsynchronousChannelGroup withThreadPool(ExecutorService executor) throws IOException {
            return AsynchronousChannelProvider.provider().openAsynchronousChannelGroup(executor, 1);
        }

        public static AsynchronousChannelGroup withCachedThreadPool(ExecutorService executor, int initialSize) throws IOException {
            return AsynchronousChannelProvider.provider().openAsynchronousChannelGroup(executor, initialSize);
        }

        public final AsynchronousChannelProvider provider() {
            return provider;
        }

        public abstract boolean isShutdown();

        public abstract boolean isTerminated();

        public abstract void shutdown();

        public abstract void shutdownNow() throws IOException;

        public abstract boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

        public static AsynchronousChannelGroup defaultGroup() throws IOException {
            return Defaults.defaultGroup();
        }
    }

    private AsyncChannels() {
    } // Singleton
}
