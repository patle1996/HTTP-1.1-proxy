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
        BufferedReader bufferedRequest = new BufferedReader(new InputStreamReader(new BufferedInputStream(getMessageInputStream())));
        String line = bufferedRequest.readLine();
        System.out.println(line);
        setStartLine(line);

        parseStartLine();

        parseHeaders(bufferedRequest);

        if (getHeaders().containsKey("content-length")) {
            readMessageBodyByLength(getMessageInputStream());
        }
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    private void parseTarget() throws MalformedURLException {
        URL targetUrl = new URL(target);
        this.hostname = targetUrl.getHost();
        this.port = targetUrl.getPort() == -1 ? targetUrl.getDefaultPort() : targetUrl.getPort();
        this.path = targetUrl.getFile() == null ? "/" : targetUrl.getFile();
    }

    @Override
    public void parseStartLine() throws MalformedURLException {
        String startLine = getStartLine();
        String[] startLineData = startLine.trim().split("\\s+");
        setMethod(startLineData[0]);
        this.target = startLineData[1];
        setProtocolVersion(startLineData[2]);

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