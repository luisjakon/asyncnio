package async.nio.dispatchables;

import async.nio.channels.AsyncChannels.CompletionHandler;
import async.nio.channels.Exceptions.InterruptedByTimeoutException;
import async.nio.channels.Exceptions.ReadPendingException;
import async.nio.channels.Exceptions.WritePendingException;
import async.nio.channels.impl.AsyncChannelGroup;
import async.nio.channels.impl.Defaults;
import async.nio.channels.system.Events.State;
import async.nio.channels.system.Futures.FutureCompletionHandler;
import async.nio.dispatchables.DispatchableChannelEvents.PendingChannelEvent;
import async.nio.dispatchables.DispatchableChannelEvents.PendingConnectEvent;
import async.nio.dispatchables.DispatchableChannelEvents.PendingIOEvent;
import async.nio.dispatchables.DispatchableChannels.DispatchableChannel;
import async.nio.dispatchables.DispatchableChannels.InterestOps;
import async.nio.dispatchers.ChannelDispatcher;
import async.nio.util.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public abstract class DispatchableChannelBase<T extends SelectableChannel> implements DispatchableChannel {

    protected static Logger LOG = Logger.getLogger(DispatchableChannelBase.class);// .setDebug(true);
    protected T channel;

    protected AsyncChannelGroup group;
    protected ChannelDispatcher dispatcher;

    volatile PendingConnectEvent connect;
    volatile PendingIOEvent read;
    volatile PendingIOEvent write;

    protected volatile int interests;
    protected volatile Boolean shutdown = false;

    public DispatchableChannelBase(AsyncChannelGroup group, T ch) throws IOException {
        if (ch == null)
            throw new IOException("Invalid or Missing channel: " + ch);

        this.connect = new PendingConnectEvent<Void>() {
        };
        this.read = new PendingIOEvent<Number>() {
        };
        this.write = new PendingIOEvent<Number>() {
        };

        this.group = (group == null) ? Defaults.defaultGroup() : group;
        this.dispatcher = group.getChannelDispatcher();

        this.channel = ch;
        this.channel.configureBlocking(false);
    }

    @Override
    public SelectableChannel getSelectableChannel() {
        return channel;
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
    public boolean isOpen() {
        return !shutdown || channel.isOpen();
    }

    @Override
    public synchronized void close() throws IOException {
        if (shutdown) {
            return;
        }
        close(new AsynchronousCloseException());
    }

    protected boolean shouldDispatchConnect() {
        return false;
    }

    protected void finishConnect() throws IOException {
    }

    public final void onConnectionReady() {

        if (connect.handler != null && connect.handler instanceof FutureCompletionHandler
                && ((FutureCompletionHandler) connect.handler).isCancelled()) {
            // FIXME: Should we report or log an error?
            connect.setReady();
            return;
        }

        if (!connect.isPending())
            return;

        try {
            // call corresponding NIO method after connection is established
            finishConnect();
            notify(connect, null);
        } catch (IOException e) {
            close(e);
        } catch (Exception e) {
            notifyError(connect, e);
        }
    }

    public final void onReadReady() {
        onEventReady(read);
    }

    public final void onWriteReady() {
        onEventReady(write);
    }

    protected void onEventReady(PendingChannelEvent evt) {

        PendingChannelEvent event = evt;

        if (event.handler != null && event.handler instanceof FutureCompletionHandler
                && ((FutureCompletionHandler) event.handler).isCancelled()) {
            // FIXME: Should we report or log an error?
            event.setReady();
            return;
        }

        if (!event.isPending())
            return;

        try {
            notify(event, event.whenReady());
        } catch (IOException e) {
            close(e);
        } catch (Exception e) {
            notifyError(event, e);
        }
    }

    public final void onConnectionClosed() {
        AsynchronousCloseException e = new AsynchronousCloseException();

        notifyError(connect, e);
        notifyError(read, e);
        notifyError(write, e);

        cleanup();
    }

    protected <V, A> void connect0(Callable<V> function, final A attachment, final CompletionHandler<V, ? super A> handler) {

        if (!dispatchable(InterestOps.OP_CONNECT, connect, function, attachment, handler))
            return;

        try {
            V res = function.call();

            if (shouldDispatchConnect()) {
                dispatch(InterestOps.OP_CONNECT);
            } else {
                notify(connect, res);
            }
        } catch (Exception e) {
            notifyError(connect, e);
        }
    }

    protected <A> void read0(Callable function, long timeout, TimeUnit unit, A attachment, CompletionHandler handler, boolean returnImmediately) {

        if (!dispatchable(InterestOps.OP_READ, read, function, attachment, handler))
            return;

        if (returnImmediately) {
            notify(read, 0);
            return;
        }

        if (timeout > 0) {
            dispatch(InterestOps.OP_READ, read, timeout, unit);
        } else {
            dispatch(InterestOps.OP_READ);
        }
    }

    protected <A> void write0(Callable function, long timeout, TimeUnit unit, A attachment, CompletionHandler handler) {

        if (!dispatchable(InterestOps.OP_WRITE, write, function, attachment, handler))
            return;

        if (timeout > 0) {
            dispatch(InterestOps.OP_WRITE, write, timeout, unit);
        } else {
            dispatch(InterestOps.OP_WRITE);
        }
    }

    // Highly Experimental socket to socket transfers. Do not use if you
    // don't know what you are doing...
    protected int transferTo0(WritableByteChannel target, int position, int count) throws IOException {

        int rc = 0, wc = 0;
        try {
            ByteBuffer data = ByteBuffer.allocate(count);
            rc = ((SocketChannel) channel).read(data);

            while (wc < rc) {
                wc += target.write(data);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wc;
    }

    // Highly Experimental socket to socket transfers. Do not use if you
    // don't know what you are doing...
    protected int transferFrom0(ReadableByteChannel target, int position, int count) throws IOException {

        int rc = 0, wc = 0;
        try {
            ByteBuffer data = ByteBuffer.allocate(count);
            rc = target.read(data);

            while (wc < rc) {
                wc += ((SocketChannel) channel).write(data);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wc;
    }

    protected void close(Exception ex) {
        synchronized (shutdown) {
            if (shutdown) {
                return;
            }
            shutdown = true;

            dispatch(interests = InterestOps.OP_CLOSE);
        }
    }

    protected void cleanup() {
    }

    protected boolean dispatchable(int op, PendingChannelEvent event, Callable function, Object attachment, CompletionHandler handler) {

        if (handler == null)
            throw new NullPointerException();

        if (event.isTimedout())
            throw new IllegalStateException("Operation not allowed due to timeout or cancellation");

        if (!event.compareAndSet(State.READY, State.PENDING)) {
            switch (op) {
                case InterestOps.OP_CONNECT:
                    throw new ConnectionPendingException();
                case InterestOps.OP_READ:
                    throw new ReadPendingException();
                case InterestOps.OP_WRITE:
                    throw new WritePendingException();
                default:
                    throw new IllegalArgumentException("Invalid operation: " + op);
            }
        }

        event.handler = handler;
        event.attachment = attachment;
        event.function = function;

        if (shutdown) {
            notifyError(event, new ClosedChannelException());
            return false;
        }

        return true;
    }


    /**
     * Channel Dispatch Helper Methods
     */
    protected void dispatch(int op) {
        interests = interests | op;
        dispatcher.registerInterestFor(this);
    }

    protected void dispatch(int op, PendingChannelEvent event, long timeout, TimeUnit unit) {
        interests = interests | op;
        dispatcher.registerInterestFor(this);
        dispatchTimeout(event, timeout, unit);
    }

    protected void dispatchTimeout(final PendingChannelEvent event, final long timeout, final TimeUnit unit) {
        if (event == null || timeout <= 0) {
            return;
        }
        group.schedule(new TimerTask() {

            @Override
            public void run() {
                synchronized (event) {

                    if (!event.isPending()) {
                        return;
                    }

                    LOG.debug(event.getClass().getSimpleName() + ": Request timed out." + "[Timeout = " + timeout + " "
                            + unit.name() + ", " + "Thread = " + Thread.currentThread().getName() + "], ");

                    event.setTimedout();
                    group.notifyError(event, new InterruptedByTimeoutException());

                }
            }
        }, timeout, unit);
    }

    /**
     * Channel Listener Notification methods
     */
    protected void notify(PendingChannelEvent event, Object res) {
        if (event == null || !event.isPending()) {
            return;
        }
        event.setState(State.NOTIFICATION_READY);
        group.notify(event, res);
    }

    protected void notifyError(PendingChannelEvent event, Throwable err) {
        if (event == null || !event.isPending()) {
            return;
        }
        event.setState(State.NOTIFICATION_READY);
        group.notifyError(event, err);
    }

}
