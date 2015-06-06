package async.nio.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class Logger extends java.util.logging.Logger {

    private static final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();

    /**
     * Public methods
     */
    public static Logger getLogger(Class<?> cls) {
        return getLogger(cls.getName());
    }

    public static Logger getLogger(String cls) {
        final Logger value = loggers.get(cls);
        if (value != null) {
            return value;
        }
        Logger newValue = new Logger(cls); // May discard.
        Logger oldValue = loggers.putIfAbsent(cls, newValue);
        return oldValue == null ? newValue : oldValue;
    }

    public static String getHashCode(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }

    public static void clear() {
        loggers.clear();
    }

    /**
     * Private members
     */
    private enum LogType {

        ASSERT, DEBUG, ERROR, INFO, VERBOSE, WARN, CONFIG, NONE
    }

    private static class Formatter extends java.util.logging.Formatter {
        // For thread-safety and speed issues...
        ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {

            @Override
            protected DateFormat initialValue() {
                return new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");// "MM/dd/yyyy hh:mm:ss.SSS"
            }

        };

        volatile LogType formatAs = LogType.INFO;
        StringBuilder sb = new StringBuilder(256);

        @Override
        public synchronized String format(LogRecord record) {
            String clazz = record.getSourceClassName();
            try {
                sb.append(df.get().format(new Date(record.getMillis())));
                sb.append(" - [").append(formatAs.name()).append("] ");
                sb.append(formatMessage(record));
                sb.append("- { m: ").append(clazz.substring(clazz.lastIndexOf('.') + 1, clazz.length())).append('.')
                        .append(record.getSourceMethodName());
                sb.append(", t: ").append(record.getThreadID()).append(" }");
                sb.append("\n");
                return sb.toString();
            } finally {
                sb.setLength(0);
            }
        }
    }

    private static final String EMPTY_STR = "";

    /**
     * Instance members & methods
     */
    Formatter logFormatter;
    String netDebug;

    volatile boolean debug;
    volatile boolean error;

    private Logger(String name) {
        this(name, null, false);
    }

    private Logger(String name, boolean debug) {
        this(name, null, debug);
    }

    private Logger(String name, String resourceBundle, boolean debug) {
        super(name, resourceBundle);
        setDebug(debug);
        setFormatHandler();
    }

    public Logger setDebug(boolean dbg) {
        if (dbg) {
            netDebug = System.getProperty("javax.net.debug");
            System.setProperty("javax.net.debug", "all");
        } else {
            if (netDebug == null)
                netDebug = EMPTY_STR;
            System.setProperty("javax.net.debug", netDebug);
        }
        debug = dbg;
        return this;
    }

    public void setFormatHandler() {
        setUseParentHandlers(false);
        addHandler(new ConsoleHandler());
        getHandlers()[0].setFormatter(logFormatter = new Formatter());
    }

    public synchronized void config(String msg, Object... args) {
        logFormatter.formatAs = LogType.CONFIG;
        log(Level.CONFIG, msg, args);
    }

    public synchronized void trace(String msg, Object... args) {
        logFormatter.formatAs = LogType.VERBOSE;
        log(Level.FINEST, msg, args);
    }

    public synchronized void detail(String msg, Object... args) {
        logFormatter.formatAs = LogType.VERBOSE;
        log(Level.FINE, msg, args);
    }

    public synchronized void info(String msg, Object... args) {
        logFormatter.formatAs = LogType.VERBOSE;
        log(Level.INFO, msg, args);
    }

    public synchronized void warn(String msg, Object... args) {
        logFormatter.formatAs = LogType.WARN;
        log(Level.WARNING, msg, args);
    }

    public synchronized void severe(String msg, Object... args) {
        logFormatter.formatAs = LogType.ERROR;
        log(Level.SEVERE, msg, args);
    }

    public synchronized void error(String msg, Object... args) {
        logFormatter.formatAs = LogType.ERROR;
        log(Level.SEVERE, msg, args);
    }

    public synchronized void error(String msg) {
        logFormatter.formatAs = LogType.ERROR;
        log(Level.SEVERE, msg);
    }

    public synchronized void debug(String msg, Object... args) {
        logFormatter.formatAs = LogType.DEBUG;
        log(Level.INFO, msg, args);
    }

    public synchronized void debug(String msg) {

        logFormatter.formatAs = LogType.DEBUG;
        log(Level.INFO, msg);
    }

    public static void main(String[] args) {
        Logger logger = new Logger("h").setDebug(true);
        logger.info("Example of creating custom formatter.");
        logger.debug("debug message.");
        logger.warning("A warning message.");
        logger.severe("A severe message.");
    }
}
