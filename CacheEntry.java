public class CacheEntry {
    private byte[] message;
    private int size;

    public CacheEntry(byte[] message, int size) {
        this.message = message;
        this.size = size;
    }

    public byte[] getMessage() {
        return message;
    }

    public int getSize() {
        return size;
    }
}
