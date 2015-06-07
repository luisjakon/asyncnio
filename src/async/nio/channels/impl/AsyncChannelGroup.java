package async.nio.channels.impl;

import async.nio.channels.AsyncChannels.AsynchronousChannelGroup;
import async.nio.channels.AsyncChannels.AsynchronousChannelProvider;
import async.nio.channels.system.Notifications;
import async.nio.dispatchables.DispatchableChannelEvents.PendingChannelEvent;
import async.nio.dispatchables.DispatchableChannels.DispatchableChannel;
import async.nio.dispatchers.ChannelDispatcher;
import async.nio.dispatchers.ClientChannelDispatcher;
import async.nio.util.CircularList;
import async.nio.util.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class AsyncChannelGroup extends AsynchronousChannelGroup {

    private static final Logger LOG = Logger.getLogger(AsyncChannelGroup.class);

    private ExecutorService notifier;

    private CircularList<ChannelDispatcher<?>> dispatchers;
    private ArrayList<DispatchableChannel<?>> channels;

    private Timer timer;
    private String name;

    protected boolean isSystemGroup = false;

    private volatile boolean shutdown;

    private int DISPATCHER_WAKEUP_DELAY_MS = 500;

    protected AsyncChannelGroup(AsynchronousChannelProvider provider, ExecutorService executor, Integer threads, Integer dispatchers) throws IOException {
        this(provider, null, executor, threads, dispatchers);
    }

    protected AsyncChannelGroup(AsynchronousChannelProvider provider, String name, ExecutorService executor, Integer threads,
                                Integer dispatchers) throws IOException {
        super(provider);

        if (executor == null || executor.isShutdown())
            throw new IllegalArgumentException("Unusable Executor: " + executor);
        if (threads <= 0)
            throw new IllegalArgumentException("Invalid number of threads: " + threads);
        if (dispatchers <= 0)
            throw new IllegalArgumentException("Invalid number of dispatchers: " + dispatchers);

        this.name = (null == name) ? "#" + this.hashCode() : name;

        setupChannels();
        setupChannelDispatchers(dispatchers);
        setupTimers(DISPATCHER_WAKEUP_DELAY_MS);
        setupNotificationSystem(executor, threads);
    }

    protected final void addChannelDispatcher(final String name) throws IOException {
        spawn(new ClientChannelDispatcher(name));
    }

    protected final void removeChannelDispatcher() {
        synchronized (dispatchers) {
            if (dispatchers.isEmpty()) {
                return;
            }
            dispatchers.remove(0).shutdown();
        }
    }

    public ChannelDispatcher<?> getChannelDispatcher() {
        return dispatchers.next();
    }

    public void schedule(TimerTask timedTask, long timeout, TimeUnit unit) {
        timer.schedule(timedTask, unit.toMillis(timeout));
    }

    public void notify(PendingChannelEvent event, Object res) {
        event.set(res);
        Notifications.send(notifier, event, event.attachment, event.handler);
    }

    public void notifyError(PendingChannelEvent event, Throwable err) {
        event.setException(err);
        Notifications.send(notifier, event, event.attachment, event.handler);
    }

    @Override
    public boolean isTerminated() {
        return shutdown || notifier.isTerminated();
    }

    @Override
    public boolean isShutdown() {
        return shutdown || notifier.isShutdown();
    }

    @Override
    public void shutdown() {
        shutdown0(1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdownNow() throws IOException {
        shutdown0(0, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return shutdown0(timeout, unit);
    }

    protected boolean shutdown0(long timeout, TimeUnit unit) {
        synchronized (this) {
            if (shutdown) {
                return true;
            }
            shutdown = true;
        }

        // Skip shutdown for system-wide AsyncChannelGroup
        // since it auto-shutdowns on system.exit()
        if (isSystemGroup) {
            return true;
        }

        return shutdown1(timeout, unit);
    }

    // Called internally
    protected boolean shutdown1(long timeout, TimeUnit unit) {
        teardownTimers();
        teardownChannels();
        teardownChannelDispatchers();
        return teardownNotificationSystem(timeout, unit);
    }

    private void setupChannels() {
        this.channels = new ArrayList<DispatchableChannel<?>>();
    }

    private void setupChannelDispatchers(int dispatchers) throws IOException {
        this.dispatchers = new CircularList<ChannelDispatcher<?>>(dispatchers);
        for (int i = 0; i < dispatchers; i++) {
            addChannelDispatcher("AsyncChannelDispatcher[" + i + "]: [ ChannelGroup: \"" + this.name + "\" ]");
        }
    }

    private void setupTimers(int delay) {
        this.timer = new Timer("AsyncChannelTimer: [ ChannelGroup: \"" + this.name + "\" ]", true);
        this.timer.schedule(new TimerTask() {

            @Override
            public void run() {
                for (ChannelDispatcher<?> d : dispatchers) {
                    d.wakeup();
                }
            }
        }, 100, delay);
    }

    private void setupNotificationSystem(ExecutorService executor, int threads) {
        this.notifier = executor;
        warmUp(notifier, threads);
    }

    private void teardownChannels() {
        for (DispatchableChannel<?> d : channels) {
            try {
                d.close();
            } catch (IOException ex) {
                // This should not stop shutdown
                // so discard problematic channels
                LOG.severe(ex.toString());
            }
        }
        channels.clear();
        channels = null;
    }

    private void teardownChannelDispatchers() {
        for (ChannelDispatcher<?> d : dispatchers) {
            d.shutdown();
        }
        dispatchers.clear();
        dispatchers = null;
    }

    private void teardownTimers() {
        timer.cancel();
        timer = null;
    }

    private boolean teardownNotificationSystem(long timeout, TimeUnit unit) {
        notifier.shutdown();
        try {
            return notifier.awaitTermination(timeout, unit);
        } catch (InterruptedException ex) {
            notifier.shutdownNow();
            return true;
        } finally {
            notifier = null;
        }
    }

    private void spawn(ClientChannelDispatcher d) {
        dispatchers.add(d);
        new Thread(d) {{
            setName(name);
            setDaemon(true);
        }}.start();
        LOG.info("<{0}@{1}>: Started new dispatcher: <ChannelDispatcher@{2}>",
                this.getClass().getSimpleName(), Logger.getHashCode(this), Logger.getHashCode(d));
    }

    private void warmUp(ExecutorService executor, int threads) {
        for (int t = 0; t < threads; t++) {
            executor.execute(new Runnable() {
                public void run() {
                }
            });
        }
    }
}
