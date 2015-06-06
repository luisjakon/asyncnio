package async.nio.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SocketOptions {

    private enum OptionsMap {

        SO_KEEPALIVE, SO_SNDBUF, SO_RCVBUF,
        TCP_NODELAY, SO_LINGER, SO_REUSEADDR, SO_TIMEOUT,
        SO_BROADCAST, IP_TOS, IP_MULTICAST_IF, IP_MULTICAST_LOOP, UNKNOWN;

        public static <T> OptionsMap get(SocketOption<T> option) {
            if (option == StandardSocketOptions.SO_KEEPALIVE)
                return SO_KEEPALIVE;
            if (option == StandardSocketOptions.SO_SNDBUF)
                return SO_SNDBUF;
            if (option == StandardSocketOptions.SO_RCVBUF)
                return SO_RCVBUF;
            if (option == StandardSocketOptions.TCP_NODELAY)
                return TCP_NODELAY;
            if (option == StandardSocketOptions.SO_LINGER)
                return SO_LINGER;
            if (option == StandardSocketOptions.SO_REUSEADDR)
                return SO_REUSEADDR;
            if (option == StandardSocketOptions.SO_TIMEOUT)
                return SO_TIMEOUT;
            if (option == StandardSocketOptions.SO_BROADCAST)
                return SO_BROADCAST;
            if (option == StandardSocketOptions.IP_TOS)
                return IP_TOS;
            if (option == StandardSocketOptions.IP_MULTICAST_IF)
                return IP_MULTICAST_IF;
            if (option == StandardSocketOptions.IP_MULTICAST_LOOP)
                return IP_MULTICAST_LOOP;
            return UNKNOWN;
        }
    }

    public synchronized static <T> void setOption(SocketChannel channel, SocketOption<T> name, T value) throws IOException {
        setOption(channel.socket(), name, value);
    }

    public synchronized static <T> void setOption(Socket socket, SocketOption<T> name, T value) throws IOException {
        if (socket == null)
            throw new IOException("Invalid Socket: null");

        switch (OptionsMap.get(name)) {
            case SO_KEEPALIVE:
                socket.setKeepAlive((Boolean) value);
                return;

            case SO_SNDBUF:
                socket.setSendBufferSize((Integer) value);
                return;

            case SO_RCVBUF:
                socket.setReceiveBufferSize((Integer) value);
                return;

            case TCP_NODELAY:
                socket.setTcpNoDelay((Boolean) value);
                return;

            case SO_LINGER:
                socket.setSoLinger(true, (Integer) value);
                return;

            case SO_REUSEADDR:
                socket.setReuseAddress((Boolean) value);
                return;

            case SO_TIMEOUT:
                socket.setSoTimeout((Integer) value);
                return;

            default:
                throw new IOException("Unknown SocketOption: " + name);
        }
    }

    public static <T> void setServerOption(ServerSocketChannel channel, SocketOption<T> name, T value) throws IOException {
        setServerOption(channel.socket(), name, value);
    }

    public static <T> void setServerOption(ServerSocket socket, SocketOption<T> name, T value) throws IOException {
        if (socket == null)
            throw new IOException("Invalid Socket: null");

        switch (OptionsMap.get(name)) {
            case SO_RCVBUF:
                socket.setReceiveBufferSize((Integer) value);
                return;

            case SO_REUSEADDR:
                socket.setReuseAddress((Boolean) value);
                return;

            case SO_TIMEOUT:
                socket.setSoTimeout((Integer) value);
                return;

            default:
                throw new IOException("Unknown SocketOption: " + name);
        }
    }

    public synchronized static <T> T getOption(SocketChannel channel, SocketOption<T> name) throws IOException {
        return getOption(channel.socket(), name);
    }

    @SuppressWarnings("unchecked")
    public synchronized static <T> T getOption(Socket socket, SocketOption<T> name) throws IOException {
        if (socket == null)
            throw new IOException("Invalid Socket: null");

        switch (OptionsMap.get(name)) {
            case SO_KEEPALIVE:
                return (T) Boolean.valueOf(socket.getKeepAlive());

            case SO_SNDBUF:
                return (T) Integer.valueOf(socket.getSendBufferSize());

            case SO_RCVBUF:
                return (T) Integer.valueOf(socket.getReceiveBufferSize());

            case TCP_NODELAY:
                return (T) Boolean.valueOf(socket.getTcpNoDelay());

            case SO_LINGER:
                return (T) Integer.valueOf(socket.getSoLinger());

            case SO_REUSEADDR:
                return (T) Boolean.valueOf(socket.getReuseAddress());

            case SO_TIMEOUT:
                return (T) Integer.valueOf(socket.getSoTimeout());

            default:
                throw new IOException("Invalid Option: " + name);
        }
    }

    public static <T> T getServerOption(ServerSocketChannel channel, SocketOption<T> name) throws IOException {
        return getServerOption(channel.socket(), name);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getServerOption(ServerSocket socket, SocketOption<T> name) throws IOException {
        if (socket == null)
            throw new IOException("Invalid Socket: null");

        switch (OptionsMap.get(name)) {
            case SO_RCVBUF:
                return (T) Integer.valueOf(socket.getReceiveBufferSize());

            case SO_REUSEADDR:
                return (T) Boolean.valueOf(socket.getReuseAddress());

            case SO_TIMEOUT:
                return (T) Integer.valueOf(socket.getSoTimeout());

            default:
                throw new IOException("Invalid Option: " + name);
        }
    }

    private SocketOptions() {
    } // Singleton
}
