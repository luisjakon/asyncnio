package async.nio.util;

import async.nio.net.SocketOption;
import async.nio.net.StandardSocketOptions;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Net {

    public static class NetAddress {

        public static class IPv4 {

            public static int toInt(byte[] ipa) {
                return (ipa[0] << 24) + ((ipa[1] & 0xFF) << 16) + ((ipa[2] & 0xFF) << 8) + ((ipa[3] & 0xFF));

            }

            public static int toInt(String ipa) {
                String[] addr = ipa.split("\\.");
                if (addr.length != 4) {
                    throw new IllegalArgumentException("Unrecognized ip address format: " + ipa);
                }
                long num = 0;
                num += (Long.parseLong(addr[0]) & 0xFF) << 24;
                num += (Long.parseLong(addr[1]) & 0xFF) << 16;
                num += (Long.parseLong(addr[2]) & 0xFF) << 8;
                num += (Long.parseLong(addr[3]) & 0xFF);
                return (int) num;
            }

            public static String toString(int ipa) {
                return new StringBuilder(15).append(((ipa >> 24) & 0xFF)).append(".").append(((ipa >> 16) & 0xFF))
                        .append(".").append(((ipa >> 8) & 0xFF)).append(".").append((ipa & 0xFF)).toString();
            }

            public static String toString(byte[] ipa) {
                return new StringBuilder(15).append(ipa[0]).append(".").append(ipa[1]).append(".").append(ipa[2])
                        .append(".").append(ipa[3]).toString();
            }

            public static byte[] toByteArray(String ipa) {
                int addr = toInt(ipa);
                return toByteArray(addr);
            }

            public static byte[] toByteArray(int ipa) {
                return new byte[]{(byte) (ipa >>> 24), (byte) (ipa >>> 16), (byte) (ipa >>> 8), (byte) ipa};
            }
        }

    }

    public static class NetworkInterfaces {

        private static Logger LOG = Logger.getLogger(NetworkInterfaces.class);

        /**
         * Taken from Apache HostNameUtil.java
         */
        public static String getLocalHostAddress() throws UnknownHostException {
            try {
                String hostAddress = getLocalHostLANAddress().getHostAddress();
                LOG.debug("hostAddress = [" + hostAddress + "]");

                return hostAddress;
            } catch (UnknownHostException e1) {
                LOG.error("Couldn't get localhost address", e1);
                throw e1;
            }
        }

        /**
         * Taken from Apache HostNameUtil.java
         */
        private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
            try {
                InetAddress candidateAddress = null;
                // Iterate all NICs (network interface cards)...
                for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces
                        .hasMoreElements(); ) {
                    NetworkInterface iface = ifaces.nextElement();
                    // Iterate all IP addresses assigned to
                    // each card...
                    for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                        InetAddress inetAddr = inetAddrs.nextElement();
                        if (!inetAddr.isLoopbackAddress()) {
                            if (inetAddr.isSiteLocalAddress()) {
                                // Found non-loopback site-local address.
                                // Return it immediately...
                                return inetAddr;
                            } else if (candidateAddress == null) {
                                // Found non-loopback address, but not necessarily site-local.
                                // Store it as a candidate to be returned if site-local address is
                                // not subsequently found...
                                candidateAddress = inetAddr;
                                // Note that we don't repeatedly assign non-loopback
                                // non-site-local addresses as candidates, only the first.
                                // For subsequent iterations, candidate will be non-null.
                            }
                        }
                    }
                }
                if (candidateAddress != null) {
                    // We did not find a site-local address, but we found some other non-loopback address.
                    // Server might have a non-site-local address assigned to its NIC (or it might be running
                    // IPv6 which deprecates the "site-local" concept).
                    // Return this non-loopback candidate address...
                    return candidateAddress;
                }
                // At this point, we did not find a non-loopback address.
                // Fall back to returning whatever InetAddress.getLocalHost() returns...
                InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
                if (jdkSuppliedAddress == null) {
                    throw new UnknownHostException(
                            "The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
                }
                return jdkSuppliedAddress;
            } catch (Exception e) {
                UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: "
                        + e);
                unknownHostException.initCause(e);
                throw unknownHostException;
            }
        }

        /**
         * Modified version taken from Apache HostNameUtil.java
         */
        private static InetAddress[] getAllIPv4Addresses() throws UnknownHostException {
            List<InetAddress> addresses = new ArrayList<InetAddress>();
            try {
                for (Enumeration<NetworkInterface> netIntf = NetworkInterface.getNetworkInterfaces(); netIntf
                        .hasMoreElements(); ) {
                    NetworkInterface iface = netIntf.nextElement();
                    for (InterfaceAddress iaddress : iface.getInterfaceAddresses()) {
                        InetAddress address = iaddress.getAddress();
                        if (!address.isLoopbackAddress()) {
                            if (address.isSiteLocalAddress()) {
                                addresses.add(address);
                            }
                        }
                    }
                }
                if (addresses.size() > 0) {
                    return addresses.toArray(new InetAddress[0]);
                }

                InetAddress address = InetAddress.getLocalHost();
                if (address == null) {
                    throw new UnknownHostException(
                            "InetAddress lookup failed.  No addresses appear to be configured on localhost.");
                }
                return new InetAddress[]{address};
            } catch (Exception e) {
                UnknownHostException error = new UnknownHostException("Failed to determine LAN addresses: " + e);
                error.initCause(e);
                throw error;
            }
        }

        static void print() {
            try {
                System.out.println("Output of Network Interrogation:");
                System.out.println("********************************\n");

                InetAddress theLocalhost = InetAddress.getLocalHost();
                System.out.println(" LOCALHOST INFO");
                if (theLocalhost != null) {
                    System.out.println("          host: " + theLocalhost.getHostName());
                    System.out.println("         class: " + theLocalhost.getClass().getSimpleName());
                    System.out.println("            ip: " + theLocalhost.getHostAddress());
                    System.out.println("         chost: " + theLocalhost.getCanonicalHostName());
                    System.out.println("      byteaddr: " + toMACAddrString(theLocalhost.getAddress()));
                    System.out.println("    sitelocal?: " + theLocalhost.isSiteLocalAddress());
                    System.out.println("");
                } else {
                    System.out.println(" localhost was null");
                }

                Enumeration<NetworkInterface> netInterfaceList = NetworkInterface.getNetworkInterfaces();
                List<InterfaceAddress> theAddrList = null;
                NetworkInterface theIntf = null;
                InetAddress theAddr = null;

                while (netInterfaceList.hasMoreElements()) {
                    theIntf = netInterfaceList.nextElement();

                    System.out.println("--------------------");
                    System.out.println(" " + theIntf.getDisplayName());
                    System.out.println("          name: " + theIntf.getName());
                    System.out.println("           mac: " + toMACAddrString(theIntf.getHardwareAddress()));
                    System.out.println("           mtu: " + theIntf.getMTU());
                    System.out.println("        mcast?: " + theIntf.supportsMulticast());
                    System.out.println("     loopback?: " + theIntf.isLoopback());
                    System.out.println("          ptp?: " + theIntf.isPointToPoint());
                    System.out.println("      virtual?: " + theIntf.isVirtual());
                    System.out.println("           up?: " + theIntf.isUp());

                    theAddrList = theIntf.getInterfaceAddresses();
                    System.out.println("     int addrs: " + theAddrList.size() + " total.");
                    int addrindex = 0;
                    for (InterfaceAddress intAddr : theAddrList) {
                        addrindex++;
                        theAddr = intAddr.getAddress();
                        System.out.println("            " + addrindex + ").");
                        System.out.println("            host: " + theAddr.getHostName());
                        System.out.println("           class: " + theAddr.getClass().getSimpleName());
                        System.out.println("              ip: " + theAddr.getHostAddress() + "/"
                                + intAddr.getNetworkPrefixLength());
                        System.out.println("           bcast: " + intAddr.getBroadcast());// .getHostAddress());
                        int maskInt = Integer.MIN_VALUE >> (intAddr.getNetworkPrefixLength() - 1);
                        System.out.println("            mask: " + toIPAddrString(maskInt));
                        System.out.println("           chost: " + theAddr.getCanonicalHostName());
                        System.out.println("        byteaddr: " + toMACAddrString(theAddr.getAddress()));
                        System.out.println("      sitelocal?: " + theAddr.isSiteLocalAddress());
                        System.out.println("");
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        public static String toMACAddrString(byte[] a) {
            if (a == null) {
                return "null";
            }
            int iMax = a.length - 1;

            if (iMax == -1) {
                return "[]";
            }

            StringBuilder b = new StringBuilder();
            b.append('[');
            for (int i = 0; ; i++) {
                b.append(String.format("%1$02x", a[i]));

                if (i == iMax) {
                    return b.append(']').toString();
                }
                b.append(":");
            }
        }

        @SuppressWarnings("unused")
        private static byte[] ipAddrToByteArray(String ipa) throws UnknownHostException {
            InetAddress address = InetAddress.getByName(ipa);
            return address.getAddress();
        }

        private static String toIPAddrString(int ipa) {
            return new StringBuilder().append(Integer.toString(0xff & (ipa >> 24))).append(".")
                    .append(Integer.toString(0xff & (ipa >> 16))).append(".")
                    .append(Integer.toString(0xff & (ipa >> 8))).append(".").append(Integer.toString(0xff & (ipa)))
                    .toString();
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static class DatagramChannels {

        public static class TrafficClass {

            public static final int LOW_COST = 0x02;
            public static final int HIGH_RELIABILITY = 0x04;
            public static final int MAXIMUM_THROUGHPUT = 0x08;
            public static final int MINIMUM_DELAY = 0x10;
        }

        private static final ThreadLocal<DatagramChannels> threadLocal = new ThreadLocal<DatagramChannels>() {

            @Override
            public DatagramChannels initialValue() {
                return new DatagramChannels();
            }
        };

        private static DatagramChannels threadSafe() {
            return threadLocal.get();
        }

        public static void joinGroup(DatagramChannel channel, InetAddress group, NetworkInterface netIf) throws IOException {
            invokeIO(MulticastMethod.join.name(), channel, group, netIf);
        }

        public static void leaveGroup(DatagramChannel channel, InetAddress group, NetworkInterface netIf) throws IOException {
            invokeIO(MulticastMethod.leave.name(), channel, group, netIf);
        }

        public static Integer getTimeToLive(DatagramChannel channel) throws Exception {
            return invokeIO(MulticastMethod.getTimeToLive.name(), channel);
        }

        public static void setTimeToLive(DatagramChannel channel, Integer ttl) throws IOException {
            invokeIO(MulticastMethod.setTimeToLive.name(), channel, ttl);
        }

        public static Integer getSoTimeout(DatagramChannel channel) throws Exception {
            return invokeSocket(MulticastMethod.getOption.name(), channel, StandardSocketOptions.SO_TIMEOUT);
        }

        public static void setSoTimeout(DatagramChannel channel, Integer timeout) throws IOException {
            invokeSocket(MulticastMethod.setOption.name(), channel, StandardSocketOptions.SO_TIMEOUT, timeout);
        }

        public static NetworkInterface getNetworkInterface(DatagramChannel channel) throws SocketException {
            return invokeSocket(MulticastMethod.getOption.name(), channel, StandardSocketOptions.IP_MULTICAST_IF2);
        }

        public static void setNetworkInterface(DatagramChannel channel, NetworkInterface netIf) throws SocketException {
            invokeSocket(MulticastMethod.setOption.name(), channel, StandardSocketOptions.IP_MULTICAST_IF2, netIf);
        }

        public static InetAddress getInterface(DatagramChannel channel) throws SocketException {
            return invokeSocket(MulticastMethod.getOption.name(), channel, StandardSocketOptions.IP_MULTICAST_IF);
        }

        public static void setInterface(DatagramChannel channel, InetAddress netIf) throws SocketException {
            invokeSocket(MulticastMethod.setOption.name(), channel, StandardSocketOptions.IP_MULTICAST_IF, netIf);
        }

        public static Boolean getBroadcast(DatagramChannel channel) throws SocketException {
            return invokeSocket(MulticastMethod.getOption.name(), channel, StandardSocketOptions.SO_BROADCAST);
        }

        public static void setBroadcast(DatagramChannel channel, Boolean mode) throws SocketException {
            invokeSocket(MulticastMethod.setOption.name(), channel, StandardSocketOptions.SO_BROADCAST, mode);
        }

        public static Boolean getLoopbackMode(DatagramChannel channel) throws SocketException {
            return invokeSocket(MulticastMethod.getOption.name(), channel, StandardSocketOptions.IP_MULTICAST_LOOP);
        }

        public static void setLoopbackMode(DatagramChannel channel, Boolean mode) throws SocketException {
            invokeSocket(MulticastMethod.setOption.name(), channel, StandardSocketOptions.IP_MULTICAST_LOOP, mode);
        }

        public static Integer getTrafficClass(DatagramChannel channel) throws SocketException {
            return invokeSocket(MulticastMethod.getOption.name(), channel, StandardSocketOptions.IP_TOS);
        }

        public static void setTrafficClass(DatagramChannel channel, Integer value) throws SocketException {
            invokeSocket(MulticastMethod.setOption.name(), channel, StandardSocketOptions.IP_TOS, value);
        }

        public static <T> T getOption(DatagramChannel channel, SocketOption<T> option) throws SocketException {
            return (T) invokeSocket(MulticastMethod.getOption.name(), channel, ((StandardSocketOptions) option).value());
        }

        public static <T> void setOption(DatagramChannel channel, SocketOption<T> option, T value) throws SocketException {
            invokeSocket(MulticastMethod.setOption.name(), channel, option, value);
        }

        private static <T> T invokeSocket(String method, DatagramChannel channel, Object... args) throws SocketException {
            try {
                return (T) threadSafe().invoke(method, channel, args);
            } catch (SocketException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                throw new SocketException(e.getMessage());
            }
        }

        private static <T> T invokeIO(String method, DatagramChannel channel, Object... args) throws IOException {
            try {
                return (T) threadSafe().invoke(method, channel, args);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                // e.printStackTrace();
                throw new IOException(e.getMessage());
            }
        }

        /**
         * Class variables
         */
        public static final String DATAGRAM_SOCKET_IMPL = "_DatagramSocket.impl";
        public static final String DATAGRAM_CHANNEL_IMPL = "_DatagramChannel.impl";
        public static final String DATAGRAM_SOCKET_IMPL_FD = "_DatagramSocket.fd";
        public static final String DATAGRAM_CHANNEL_IMPL_FD = "_DatagramChannel.fd";
        private Object tempSocket;
        private Field tempSocketFd;
        private Field tempChannelFd;

        private enum MulticastMethod {

            join, leave, socketSetOption, setOption, setInterface, setNetworkInterface, setTimeToLive, setSoTimeout, setBroadcast, setLoopbackMode, setTrafficClass, socketGetOption, getOption, getInterface, getNetworkInterface, getTimeToLive, getSoTimeout, getBroadcast, getLoopbackMode, getTrafficClass
        }

        private DatagramChannels() {
            // java.net.PlainDatagramSocketImpl z;
            System.setProperty(DATAGRAM_SOCKET_IMPL, "java.net.PlainDatagramSocketImpl");
            System.setProperty(DATAGRAM_SOCKET_IMPL_FD, "fd");
            System.setProperty(DATAGRAM_CHANNEL_IMPL, "sun.nio.ch.DatagramChannelImpl");
            System.setProperty(DATAGRAM_CHANNEL_IMPL_FD, "fd");
        }

        private <T, A, B> T invoke(String method, DatagramChannel channel, Object... args) throws IOException {

            // http://www.mernst.org/blog/archives/12-01-2006_12-31-2006.html
            // UGLY UGLY HACK: multicast support for NIO
            // create a temporary instanceof PlainDatagramSocket, set its fd and configure it
            if (tempSocket == null) {

                // Enable access to DatagramSocketImpl class
                Constructor c;
                try {
                    // Create temporary DatagramSocket instance
                    c = Class.forName(System.getProperty(DATAGRAM_SOCKET_IMPL)).getDeclaredConstructor();
                    c.setAccessible(true);
                    tempSocket = c.newInstance();

                    // for(Field field :
                    // tempSocket.getClass().getSuperclass().getSuperclass().getDeclaredFields()){
                    // Print field info
                    // System.out.println(field);
                    // }

                    // Enable access to Socket.fd
                    // tempSocketFd = Class.forName(System.getProperty(DATAGRAM_SOCKET_IMPL)).getSuperclass().getDeclaredField(System.getProperty(DATAGRAM_SOCKET_IMPL_FD));
                    Class clz = Class.forName(System.getProperty(DATAGRAM_SOCKET_IMPL));
                    System.out.println(clz);

                    tempSocketFd = clz.getSuperclass().getSuperclass()
                            .getDeclaredField(System.getProperty(DATAGRAM_SOCKET_IMPL_FD));
                    tempSocketFd.setAccessible(true);

                    // Enable access to Channel.fd
                    tempChannelFd = Class.forName(System.getProperty(DATAGRAM_CHANNEL_IMPL)).getDeclaredField(
                            System.getProperty(DATAGRAM_CHANNEL_IMPL_FD));
                    tempChannelFd.setAccessible(true);
                } catch (RuntimeException e) {
                    tempSocket = null;
                    throw e;
                } catch (Exception e) {
                    tempSocket = null;
                    e.printStackTrace();
                    throw new SocketException(e.getMessage());
                }
            }

            try {

                // Bridge temporary file descriptors -
                // DatagramSocket.fd = DatagramChannel.fd;
                tempSocketFd.set(tempSocket, tempChannelFd.get(channel));

                // Calling - DatagramSocket.methodX(...);
                switch (args.length) {
                    case 0:
                        // calling channel.getX( ... );
                        Method m = tempSocket.getClass().getDeclaredMethod(method);
                        m.setAccessible(true);
                        synchronized (channel) {
                            return (T) m.invoke(tempSocket);
                        }
                    case 1:
                        // calling channel.setX( ... ) || channel.getOption( ... );
                        if (args[0] instanceof Integer) {
                            // Downcasting to int
                            // primitive
                            m = tempSocket.getClass().getSuperclass().getDeclaredMethod(method, Integer.TYPE);
                            m.setAccessible(true);
                            synchronized (channel) {
                                return (T) m.invoke(tempSocket, args);
                            }
                        }
                        // calling channel.setX( ... ) || channel.getOption( ... );
                        if (args[0] instanceof Boolean) {
                            // Downcasting to
                            // boolean primitive
                            m = tempSocket.getClass().getDeclaredMethod(method, Boolean.TYPE);
                            m.setAccessible(true);
                            synchronized (channel) {
                                return (T) m.invoke(tempSocket, args);
                            }
                        }
                        break;
                    case 2:
                        // calling channel.setOption(args[0], args[1]);
                        if (args[0] instanceof SocketOption) {
                            m = tempSocket.getClass().getSuperclass()
                                    .getDeclaredMethod(method, Integer.TYPE, Object.class);
                            m.setAccessible(true);
                            synchronized (channel) {
                                return (T) m.invoke(tempSocket, ((StandardSocketOptions) args[0]).value(),
                                        args[1]);
                            }
                        }
                        // calling channel.bind(args[0]. args[1])
                        if (args[1] instanceof InetAddress) {
                            m = tempSocket.getClass()
                                    .getDeclaredMethod(method, Integer.TYPE, InetAddress.class);
                            m.setAccessible(true);
                            synchronized (channel) {
                                return (T) m.invoke(tempSocket, args);
                            }
                        }
                        break;
                }

                Class<?>[] classes = new Class<?>[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof InetAddress) {
                        // channel.connect(args[0], args[1]) requires InetAddress vs Inet4Address
                        classes[i] = InetAddress.class;
                        continue;
                    }
                    classes[i] = args[i].getClass();
                }

                Method m = tempSocket.getClass().getDeclaredMethod(method, classes);
                m.setAccessible(true);

                // Make sure we synchronize with the real
                // DatagramChannel fd before we make the call
                synchronized (channel) {
                    return (T) m.invoke(tempSocket, args);
                }
            } catch (RuntimeException e) {
                tempSocket = null;
                throw e;
            } catch (Exception e) {
                tempSocket = null;
                // Rethrow SocketException
                if (e.getCause() instanceof SocketException) {
                    throw (SocketException) e.getCause();
                }
                // Rethrow IOException
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                }
                // Re-wrap error into an IOException
                e.printStackTrace();
                throw new IOException(e.getMessage());
            } finally {
                try {
                    // Nullify real DatagramChannel's fd from temporary socket instance,
                    // otherwise the fake socket's finalizer will nuke the real channel's fd
                    tempSocketFd.set(tempSocket, null);
                } catch (Exception ex) {
                }
            }
        }

        public static void main(String[] args) throws Exception {

            // Prevents JRE from throwing weird SocketExceptions
            // when setting NetworkInterface params
            System.setProperty("java.net.preferIPv4Stack", "true");

            DatagramChannel dc = SelectorProvider.provider().openDatagramChannel();

            // Test DatagramChannel methods
            DatagramChannels.setInterface(dc, InetAddress.getLocalHost());
            System.out.println("DatagramSocket.Interface: " + DatagramChannels.getInterface(dc));

            DatagramChannels.setNetworkInterface(dc, NetworkInterface.getByName("eth0"));
            System.out.println("DatagramSocket.NetInterface: " + DatagramChannels.getNetworkInterface(dc));

            DatagramChannels.setLoopbackMode(dc, true);
            System.out.println("DatagramSocket.LoopbackMode: " + DatagramChannels.getLoopbackMode(dc));

            DatagramChannels.setTimeToLive(dc, 5);
            System.out.println("DatagramSocket.TimeToLive: " + DatagramChannels.getTimeToLive(dc));

            DatagramChannels.setTrafficClass(dc, TrafficClass.HIGH_RELIABILITY | TrafficClass.MAXIMUM_THROUGHPUT
                    | TrafficClass.MINIMUM_DELAY);
            System.out.println("DatagramSocket.TrafficClass: " + DatagramChannels.getTrafficClass(dc));

            // Test DatagramChannel Multicast methods
            DatagramChannels.joinGroup(dc, InetAddress.getByName("224.224.1.1"), NetworkInterface.getNetworkInterfaces()
                    .nextElement());
            DatagramChannels.leaveGroup(dc, InetAddress.getByName("224.224.1.1"), NetworkInterface.getNetworkInterfaces()
                    .nextElement());

            // Test DatagramChannel.SocketOptions
            DatagramChannels.setOption(dc, StandardSocketOptions.IP_MULTICAST_IF, InetAddress.getLocalHost());
            System.out.println("SocketOption.IP_MULTICAST_IF: "
                    + DatagramChannels.getOption(dc, StandardSocketOptions.IP_MULTICAST_IF));

            DatagramChannels.setOption(dc, StandardSocketOptions.IP_MULTICAST_IF2, NetworkInterface.getNetworkInterfaces()
                    .nextElement());
            System.out.println("SocketOption.IP_MULTICAST_IF2: "
                    + DatagramChannels.getOption(dc, StandardSocketOptions.IP_MULTICAST_IF2));

            DatagramChannels.setOption(dc, StandardSocketOptions.SO_REUSEADDR, true);
            System.out.println("SocketOption.SO_REUSEADDR: "
                    + DatagramChannels.getOption(dc, StandardSocketOptions.SO_REUSEADDR));

            DatagramChannels.setOption(dc, StandardSocketOptions.SO_TIMEOUT, 5);
            System.out.println("SocketOption.SO_TIMEOUT: " + DatagramChannels.getOption(dc, StandardSocketOptions.SO_TIMEOUT));

            DatagramChannels.setOption(dc, StandardSocketOptions.IP_TOS, 8);
            System.out.println("SocketOption.IP_TOS: " + DatagramChannels.getOption(dc, StandardSocketOptions.IP_TOS));

            DatagramChannels.setOption(dc, StandardSocketOptions.IP_MULTICAST_LOOP, true);
            System.out.println("SocketOption.IP_MULTICAST_LOOP: "
                    + DatagramChannels.getOption(dc, StandardSocketOptions.IP_MULTICAST_LOOP));

            dc.close();
        }
    }

    private Net() {
    } // Singleton
}
