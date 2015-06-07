package async.nio.channels.impl;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class Defaults {

    public static AsyncChannelProvider defaultProvider() {
        if (DEFAULT_PROVIDER == null) {
            DEFAULT_PROVIDER = AsyncChannelProvider.DEFAULT_PROVIDER;
        }
        return DEFAULT_PROVIDER;
    }

    public static synchronized AsyncChannelGroup defaultGroup() throws IOException {
        if (DEFAULT_GROUP == null) {
            DEFAULT_GROUP = new AsyncChannelGroup(Defaults.defaultProvider(), Defaults.defaultNotifierService(),
                    Defaults.defaultNotifierThreads(), Defaults.defaultDispatchers());
            DEFAULT_GROUP.isSystemGroup = true;

            Defaults.addShutdownHook();  // <= IMPORTANT: do not remove unless you know what you're doing
        }
        return DEFAULT_GROUP;
    }

    public static ThreadFactory defaultThreadGroupFactory(String name) {
        return new AsyncThreadFactory(name);
    }

    private static ExecutorService defaultNotifierService() {
        return Executors.newFixedThreadPool(Defaults.defaultNotifierThreads(),
                new AsyncThreadFactory(values[3]));
    }

    private static int defaultNotifierThreads() {
        return getSystemValue(values[1]);
    }

    private static int defaultDispatchers() {
        return getSystemValue(values[0]);
    }

    private static int defaultTimeout() {
        return getSystemValue(values[2]);
    }

    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                DEFAULT_GROUP.shutdown1(Defaults.defaultTimeout(), TimeUnit.MILLISECONDS);
                try {
                    Thread.sleep(2 * Defaults.defaultTimeout()); // <= Give time for things to settle
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static int getSystemValue(String key) {
        return Integer.valueOf(System.getProperty(key));
    }

    private static void setSystemValue(String key, int value) {
        System.setProperty(key, Integer.toString(value));
    }


    private static String[] values = new String[]{
            "async.nio.defaults.DEFAULT_CHANNEL_DISPATCHERS",
            "async.nio.defaults.DEFAULT_CHANNEL_NOTIFIER_THREADS",
            "async.nio.defaults.DEFAULT_TIMEOUT_MS",
            "async.nio.defaultGroup"
    };

    static {
        setSystemValue(values[0], Runtime.getRuntime().availableProcessors() * 2);
        setSystemValue(values[1], Runtime.getRuntime().availableProcessors() * 2);
        setSystemValue(values[2], 1000);
    }

    private static AsyncChannelProvider DEFAULT_PROVIDER;
    private static AsyncChannelGroup DEFAULT_GROUP;

    private Defaults() {
    } // Singleton
}
