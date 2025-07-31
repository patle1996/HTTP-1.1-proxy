import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class HttpRequest extends HttpMessage {
    private static final String ZID = "z5414008";

    private String target;
    private String hostname;
    private int port;
    private String path;

    public HttpRequest(InputStream request) {
        super(request);
    }

    @Override
    public void parseMessage() throws IOException {
        parseHeaders();
        String connectionType = getHeaders().get("connection");
        setConnectionType(connectionType);
        if (getHeaders().containsKey("content-length")) {
            readMessageBodyByLength();
        }
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getTarget() {
        return target;
    }

    private void parseUrl(URL targetUrl) {
        this.hostname = targetUrl.getHost();
        this.port = targetUrl.getPort() == -1 ? targetUrl.getDefaultPort() : targetUrl.getPort();
        this.path = targetUrl.getFile().isEmpty() ? "/" : targetUrl.getFile();
    }

    private void parseTarget() throws MalformedURLException {
        if (target.startsWith("http://") || target.startsWith("https://")) {
            parseUrl(new URL(target));
            return;
        }

        String host = getHeaders().get("host");
        if (host != null) {
            host = host.trim();
            String fullUrl = "http://" + host + target;
            parseUrl(new URL(fullUrl));
        } else {
            this.hostname = null;
            this.port = 80;
            this.path = target.isEmpty() ? "/" : target;
        }
    }

    @Override
    public void parseStartLine() throws MalformedURLException, IOException {
        String line = readLine();
        if (line == null || line.isEmpty()) {
            throw new IOException("Empty or null start line");
        }

        setStartLine(line);

        String[] startLineData = line.trim().split("\\s+");
        setMethod(startLineData[0]);
        this.target = startLineData[1];
        setProtocolVersion(startLineData[2]);
        if (startLineData[0].equals("CONNECT")) {
            int colonIdx = target.indexOf(':');
            if (colonIdx == - 1) {
                // Malformed CONNECT request
            }
            this.hostname = target.substring(0, colonIdx);
            this.port = Integer.parseInt(target.substring(colonIdx + 1));
            return;
        }
        parseTarget();
    }

    // Transforms the request to be sent to the origin server 
    public byte[] getTransformedRequest() {
        String headersString = getMethod() + " " + path + " " + getProtocolVersion() + "\r\n";
        for (Map.Entry<String, String> header : getHeaders().entrySet()) {
            // Close connection and remove proxy-connection header
            if (header.getKey().equals("connection") || header.getKey().equals("proxy-connection")) {
                continue;
            }

            headersString += header.getKey() + ":" + header.getValue() + "\r\n";
        }

        headersString += "connection: close\r\n" + "via: 1.1 " + ZID + "\r\n\r\n";
        // If no message body, return start line + headers
        byte[] headersStringBytes = headersString.getBytes(StandardCharsets.US_ASCII);
        if (getMessageBody() == null) {
            return headersStringBytes;
        }

        // If message body exists, make a new byte array and return all data
        int size = getMessageBodySize() + headersStringBytes.length;
        byte[] transformedRequest = new byte[size];
        
        System.arraycopy(headersStringBytes, 0, transformedRequest, 0, headersStringBytes.length);
        System.arraycopy(getMessageBody(), 0, transformedRequest, headersStringBytes.length, getMessageBody().length);

        return transformedRequest;
    }
}