package java.net;

import java.io.Serializable;

/**
 * Supplied for the wasm build only. Gson's TypeAdapters static initialiser
 * registers an InetAddress adapter, so the class must resolve for any Gson
 * entry point to link — including JsonParser, which the NPC plan parser uses.
 *
 * A browser has no host resolution, so nothing here is callable; the type only
 * needs to exist. Any actual use fails loudly rather than silently misbehaving.
 */
public class InetAddress implements Serializable {

    protected InetAddress() {
    }

    public static InetAddress getByName(String host) throws UnknownHostException {
        throw new UnknownHostException("no name resolution in the browser build: " + host);
    }

    public static InetAddress getLocalHost() throws UnknownHostException {
        throw new UnknownHostException("no name resolution in the browser build");
    }

    public String getHostAddress() {
        throw new UnsupportedOperationException("InetAddress is unavailable in the browser build");
    }

    public String getHostName() {
        throw new UnsupportedOperationException("InetAddress is unavailable in the browser build");
    }
}
