package async.nio.channels.impl;

import async.nio.channels.AsyncChannels.AsynchronousChannelGroup;
import async.nio.channels.AsyncChannels.AsynchronousChannelProvider;
import async.nio.channels.AsyncSocketChannels.AsynchronousServerSocketChannel;
import async.nio.channels.AsyncSocketChannels.AsynchronousSocketChannel;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class AsyncChannelProvider extends AsynchronousChannelProvider {

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads, ThreadFactory threadFactory) throws IOException {
        return new AsyncChannelGroup(this, Executors.newFixedThreadPool(nThreads, threadFactory), -1, 1);
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor, int initialSize) throws IOException {
        return new AsyncChannelGroup(this, executor, initialSize, 1);
    }

    @Override
    public AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group) throws IOException {
        return new AsyncSocketChannel(checkGroup(group), SocketChannel.open());
    }

    @Override
    public AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(AsynchronousChannelGroup group) throws IOException {
        return new AsyncServerSocketChannel(checkGroup(group));
    }

    private AsyncChannelGroup checkGroup(AsynchronousChannelGroup group) throws IOException {
        if (group == null) {
            return Defaults.defaultGroup();
        }
        if (group.provider() != this) {
            throw new IllegalArgumentException("AsynchronousChannelGroup not created by this provider.");
        }

        return (AsyncChannelGroup) group;
    }

    protected AsyncChannelProvider() {
    } // Singleton
}
