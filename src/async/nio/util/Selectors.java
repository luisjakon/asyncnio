package async.nio.util;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collections;
import java.util.Set;

public class Selectors {

    protected static final Logger LOG = Logger.getLogger(Selectors.class);// .setDebug(true);

    public static Selector replaceSelector(Selector sel) throws IOException {
        LOG.warning("Replacing Selector: " + sel);

        Selector oldSelector = sel;
        Selector newSelector = null;

        synchronized (sel) {
            newSelector = Selector.open();

            if (oldSelector == null)
                return newSelector;

            for (SelectionKey key : getKeys(oldSelector)) {
                try {
                    key.channel().register(newSelector, key.interestOps(), key.attachment());
                } catch (CancelledKeyException ignore) {
                }
            }

            try {
                oldSelector.close();
            } catch (IOException ignore) {
            }

            return newSelector;
        }
    }

    private static Set<SelectionKey> getKeys(Selector sel) {
        try {
            return sel.keys();
        } catch (ClosedSelectorException ignore) {
            return Collections.EMPTY_SET;
        }
    }

    private Selectors() {
    }   // Singleton
}
