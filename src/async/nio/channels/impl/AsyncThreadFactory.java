package async.nio.channels.impl;

import async.nio.util.Logger;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class AsyncThreadFactory implements ThreadFactory {

    AtomicInteger count;
    String gid;

    public AsyncThreadFactory(String gid) {
        this.gid = gid;
        this.count = new AtomicInteger();
    }

    public String name() {
        return gid;
    }

    public AsyncThreadFactory setName(String name) {
        gid = name;
        return this;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("AsyncChannelGroup[" + gid + "].worker[" + count.getAndIncrement() + "]");
        t.setDaemon(true);
        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                Logger.getLogger(t.getName()).log(Level.WARNING, e.getMessage(), e);
            }
        });
        return t;
    }
}