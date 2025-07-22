import java.io.*;
import java.net.*;
import java.util.*;

public abstract class HttpMessage {
    Map<String, String> headers = new HashMap<>();
    byte[] messageBody;

    public void parseHeaders(BufferedReader bufferedHeaders) {
        String line = bufferedHeaders.readLine();
        int colonIdx;
        while (line != null && !line.isEmpty()) {
            colonIdx = line.indexOf(':');
            headers.put(line.substring(0, colonIdx).trim().toLowerCase(), line.substring(colonIdx + 1));
            line = bufferedHeaders.readLine();
        }
    }

    abstract public void readMessageBody(InputStream inputStream);
}