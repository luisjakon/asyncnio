package async.nio.dispatchers;

import async.nio.dispatchables.DispatchableChannels.Dispatchable;
import async.nio.dispatchables.DispatchableChannels.InterestOps;
import async.nio.dispatchers.Dispatchers.ChannelDispatcher;
import async.nio.util.Logger;
import async.nio.util.Selectors;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class SimpleChannelDispatcher<T extends Dispatchable> implements Runnable, ChannelDispatcher<T> {
    protected final Logger LOG = Logger.getLogger(getClass());// .setDebug(true);

    public static int MIN_SPINS = 20;
    public static int MAX_SPINS = 512;
    public static long MAX_DELTA_NS = 53416;// 104858;//262144;//524288;
    public static long DEFAULT_TIMEOUT = 500; // in millis
    public static long[] TIMEOUTS = {30, 50, 100, 250, 500};// ,1000, 2000, 4000, 10000, 150000};

    private static final Dispatchable[] DISPATCHABLE_ARRAY = new Dispatchable[0];

    private String name;
    private final Queue<T> channels;
    private final BlockingQueue<T> closed_channels;
    private Selector selector;
    private long timeout = DEFAULT_TIMEOUT;
    private volatile boolean shutdown;

    protected SimpleChannelDispatcher(String name) {
        this.name = name;
        try {
            selector = Selector.open();
            channels = new LinkedList<T>();
            closed_channels = new LinkedBlockingQueue<T>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String name() {
        return name;
    }

    public boolean isOpen() {
        return !shutdown;
    }

    public SelectorProvider selectorProvider() {
        return selector.provider();
    }

    public boolean registerInterestFor(T channel) {
        if (shutdown)
            return false;

        try {
            synchronized (channels) {
                if (channels.contains(channel)) {
                    return true;
                }
                return channels.add(channel);
            }
        } finally {
            // LOG.debug("Waking up selector: " + channel.interestOps());
            wakeup();
        }
    }

    @Override
    public void run() {

        if (shutdown) {
            return;
        }
        LOG.debug("<{0}@{1}> timeout:{2}, selector: {3}", getClass().getSimpleName(), Logger.getHashCode(this), timeout,
                selector);

        int spinLoops = 0;
        boolean spinDetect = true;

        // Main Dispatcher EventLoop
        while (!shutdown) {
            try {

                // Process closed channels
                this.processClosedChannels();

                // Register new requests
                this.processRegisteredOps();

                // Sleep if required
                this.sleep();

                // Log spin time.
                long spinTime = System.nanoTime();

                // FIXME: Look into more sophisticated spin detection mechanisms
                if (selector.select() <= 0) {
                    if (spinDetect && detectSpin(spinTime, spinLoops++)) {
                        selector = Selectors.replaceSelector(selector);
                        selector.selectNow();
                    } else {
                        continue;
                    }
                }

                // Selector in good behavior. Reset Selector spin detection checks.
                if (spinDetect && spinLoops >= MAX_SPINS) {
                    spinLoops = 0;
                }

                // Process I/O requests on selected channels
                this.processSelectedKeys();

            } catch (ClosedSelectorException e) {
                LOG.debug("DispatcherEventLoop ClosedSelectorException: ", e);
            } catch (IOException e) {
                LOG.debug("DispatcherEventLoop IOException: ", e);
            } catch (Exception e) {
                LOG.debug("DispatcherEventLoop Exception: ", e);
            }
        }

        cleanup();
        LOG.info("DispatcherEventLoop Exiting: " + shutdown);
    }

    protected void sleep() {
        // This overridable method allows subclasses to:
        //   1. Give time for selection keys to update.
        //   2. Force the runner to sleep when spinning uncontrollably.
    }

    public void wakeup() {
        if (selector == null)
            return;
        selector.wakeup();
    }

    private void processRegisteredOps() {
        T[] clones;
        synchronized (channels) {
            if (channels.size() == 0)
                return;

            clones = (T[]) channels.toArray(DISPATCHABLE_ARRAY);
            channels.clear();
        }
        for (T channel : clones) {
            registerOps(channel, channel.interestOps());
        }
        clones = null;
    }

    private void registerOps(T channel, int ops) {
        try {
            if (ops >= InterestOps.OP_CLOSE) {
                closed_channels.add(channel);
                return;
            }

            SelectionKey key = getOrCreateKey(channel);
            key.interestOps(key.interestOps() | ops);

        } catch (IOException e) {
            LOG.debug(e.toString());
        }
    }

    private void processSelectedKeys() {
        for (SelectionKey key : selector.selectedKeys()) {
            try {
                handleIO((T) key.attachment(), key);
            } catch (Exception e) {
                LOG.warning(e.toString());
            }
        }
        selector.selectedKeys().clear();
    }

    private void processClosedChannels() {
        T channel;
        for (; ; ) {
            try {
                channel = closed_channels.poll();
                if (channel == null) break;
                closeChannel(channel);
            } catch (Exception e) {
                LOG.debug("Error closing channel");
            }
        }
    }

    protected abstract void handleIO(T channel, SelectionKey key);

    private boolean detectSpin(long lastTime, int loops) {
        long delta = System.nanoTime() - lastTime;

        if (loops > MAX_SPINS && delta < MAX_DELTA_NS) {
            LOG.warning(selector + " spin detected [Loops: " + loops + ", Time diff: " + delta + "]");
            return true;
        }
        return false;
    }

    public void shutdown() {
        synchronized (this) {
            if (shutdown) {
                return;
            }
            this.shutdown = true;
        }
        // Force selector to run one last time
        // before shutting down...
        wakeup();
    }

    private void cleanup() {
        synchronized (this) {
            closeChannels(channels);
            closeChannels(closed_channels);
            closeSelector();
        }
    }

    private void closeChannels(Queue<T> channels) {
        for (T channel : channels) {
            closeChannel(channel);
        }
        channels.clear();
    }

    private void closeChannel(T channel) {
        closeChannel(channel.getSelectableChannel());
        closeNotify(channel);
    }

    private void closeChannel(final SelectableChannel channel) {
        if (channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException e) {
                LOG.warning("closeChannel(): " + e.getMessage());
            }
        }
    }

    private void closeNotify(T channel) {
        try {
            channel.onConnectionClosed();
        } catch (Exception e) {
            LOG.warning("onConnectionClosed(): " + e.getMessage());
        }
    }

    private void closeSelector() {
        try {
            for (SelectionKey key : selector.keys()) {
                key.channel().close();
                key.cancel();
                // FIXME: Should channels be closed here too?
            }
            this.selector.close();
        } catch (Exception e) {
            LOG.warning(e.getMessage());
        }
    }

    protected SelectionKey getOrCreateKey(T channel) throws IOException {
        SelectionKey key;
        if ((key = channel.getSelectableChannel().keyFor(selector)) != null) {
            return key;
        }
        return channel.getSelectableChannel().register(selector, 0, channel);
    }

    private Selector newSelector() throws IOException {
        return (selector == null) ? Selector.open() : Selectors.replaceSelector(selector);
    }

}
