
import java.util.*;
import java.net.*;

public class Cache {
    private static Cache cacheInstance;

    private int maxObjectSize;
    private int maxCacheSize;
    private Map<String, CacheEntry> store;
    private int spaceRemaining;

    private Cache(int maxObjectSize, int maxCacheSize) {
        this.maxObjectSize = maxObjectSize;
        this.maxCacheSize = maxCacheSize;
        this.spaceRemaining = maxCacheSize;

        int initialCapacity = (int) Math.ceil((double) maxCacheSize / maxObjectSize);
        this.store = new LinkedHashMap<String, CacheEntry>(initialCapacity, 0.75f, true);
    }

    public static synchronized void initialize(int maxObjectSize, int maxCacheSize) {
        if (cacheInstance == null) {
            cacheInstance = new Cache(maxObjectSize, maxCacheSize);
        } else {
            throw new IllegalStateException("Cache already initialized");
        }
    }

    public static Cache getInstance() {
        if (cacheInstance == null) {
            throw new IllegalStateException("Cache not initialized yet");
        }
        return cacheInstance;
    }

    private String normaliseUrl(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        String scheme = url.getProtocol().toLowerCase();
        String host = url.getHost().toLowerCase();
        int port = url.getPort();
        String path = url.getFile();
        String reference = url.getRef();

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(scheme + "://" + host);

        if (port != -1) {
            urlBuilder.append(":" + port);
        }

        if (!path.isEmpty()) {
            urlBuilder.append("/" + path);
        }

        if (reference != null) {
            urlBuilder.append("#" + reference);
        }

        return urlBuilder.toString();
    }

    private void removeResponse() {
    Iterator<Map.Entry<String, CacheEntry>> it = store.entrySet().iterator();
    if (it.hasNext()) {
        Map.Entry<String, CacheEntry> eldest = it.next();
        spaceRemaining += eldest.getValue().getSize();
        it.remove();
    }
}

    public synchronized void storeResponse(byte[] response, int size, String url) throws MalformedURLException {
        if (size > maxObjectSize) {
            return;
        }

        while (size > spaceRemaining) {
            removeResponse();
        }
        store.put(normaliseUrl(url), new CacheEntry(response, size));
        spaceRemaining -= size;
    }

    public synchronized CacheEntry getResponse(String urlString) throws MalformedURLException {
        return store.get(normaliseUrl(urlString));
    }

    public boolean isCached(String urlString) throws MalformedURLException {
        return store.containsKey(normaliseUrl(urlString));
    }
}