package async.nio.net;

import java.lang.reflect.ParameterizedType;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketOptions;

public class StandardSocketOptions<T> implements SocketOption<T> {

    public static final SocketOption<Boolean> SO_KEEPALIVE = new StandardSocketOptions<Boolean>("SO_KEEPALIVE", SocketOptions.SO_KEEPALIVE);

    public static final SocketOption<Integer> SO_SNDBUF = new StandardSocketOptions<Integer>("SO_SNDBUF", SocketOptions.SO_SNDBUF);

    public static final SocketOption<Integer> SO_RCVBUF = new StandardSocketOptions<Integer>("SO_RCVBUF", SocketOptions.SO_RCVBUF);

    public static final SocketOption<Boolean> SO_REUSEADDR = new StandardSocketOptions<Boolean>("SO_REUSEADDR", SocketOptions.SO_REUSEADDR);

    public static final SocketOption<Integer> SO_LINGER = new StandardSocketOptions<Integer>("SO_LINGER", SocketOptions.SO_LINGER);

    public static final SocketOption<Integer> SO_TIMEOUT = new StandardSocketOptions<Integer>("SO_TIMEOUT", SocketOptions.SO_TIMEOUT);

    public static final SocketOption<Boolean> SO_BROADCAST = new StandardSocketOptions<Boolean>("SO_BROADCAST", SocketOptions.SO_BROADCAST);

    public static final SocketOption<Boolean> TCP_NODELAY = new StandardSocketOptions<Boolean>("TCP_NODELAY", SocketOptions.TCP_NODELAY);

    public static final SocketOption<Integer> IP_TOS = new StandardSocketOptions<Integer>("IP_TOS", SocketOptions.IP_TOS);

    public static final SocketOption<InetAddress> IP_MULTICAST_IF = new StandardSocketOptions<InetAddress>("IP_MULTICAST_IF", SocketOptions.IP_MULTICAST_IF);

    public static final SocketOption<NetworkInterface> IP_MULTICAST_IF2 = new StandardSocketOptions<NetworkInterface>("IP_MULTICAST_IF2", SocketOptions.IP_MULTICAST_IF2);

    public static final SocketOption<Boolean> IP_MULTICAST_LOOP = new StandardSocketOptions<Boolean>("IP_MULTICAST_LOOP", SocketOptions.IP_MULTICAST_LOOP);

    private String name;
    private int value;

    private StandardSocketOptions(String name, int value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String name() {
        return name;
    }

    public int value() {
        return value; // Platform-dependent int value;
    }

    @Override
    public Class<T> type() {
        return ((Class) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }
}
