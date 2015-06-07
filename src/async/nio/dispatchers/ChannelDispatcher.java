package async.nio.dispatchers;

public interface ChannelDispatcher<T> {

    boolean registerInterestFor(T channel);

    void wakeup();

    void shutdown();
}
