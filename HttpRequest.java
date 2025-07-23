import java.io.*;
import java.net.*;
import java.util.*;

create public class HttpRequest extends HttpMessage {
    private static final String ZID = "z5414008";

    String method;
    String target;
    String protocolVersion;
    String hostname;
    int port;
    String path;

    public HttpRequest(InputStream request) {
        BufferedReader bufferedRequest = new BufferedReader(new InputStreamReader(request));
        Map<String, String> parsedRequest = new HashMap<>();
        String line = bufferedRequest.readLine();

        parseStartLine(line);
        super.parseHeaders(bufferedRequest);
        if (headers.containsKey("content-length")) {
            readMessageBody(request);
        }
    }

    private static void parseTarget() {
        URL targetUrl = new URL(target);
        this.hostname = targetUrl.getHost();
        this.port = targetUrl.getPort() == -1 ? targetUrl.getDefaultPort() : targetUrl.getPort();
        this.path = targetUrl.getFile() == null ? "/" : targetUrl.getFile();
    }

    private static void parseStartLine(String startLine) {
        String[] startLineData = startLine.trim().split("\\s+");
        this.method = startLineData[0];
        this.target = startLineData[1];
        this.protocolVersion = startLineData[2];

        parseTarget();
    }

    @Override
    public void readMessageBody(InputStream inputStream) {
        int length = parseInt(headers.get("content-length"));
        this.messageBody = inputStream.readNBytes(length);
    }

    public BufferedWriter getTransformedRequest() {
        String headersString = method + " " + path + " " + protocolVersion + "\r\n"
        for (Map.Entry<String, String> header : headers) {
            // Close connection and remove proxy-connection header
            if (header.getKey().isEqual("connection") || header.getKey().isEqual("proxy-connection")) {
                continue;
            }

            headersString += header.getKey() + ":" + header.getValue() + "/r/n";
        }

        headersString += "connection: close\r\n" + "via: 1.1 " + ZID + "\r\n";

        byte[] headersStringBytes = headersString.getBytes(StandardCharsets.US_ASCII);
        int size = messageBody.length + headersStringBytes.length;
        byte[] transformedRequest = new byte[size];
        
        System.arraycopy(headersStringBytes, 0, transformedRequest, 0, headersStringBytes.length);
        System.arraycopy(messageBody, 0, transformedRequest, headersStringBytes.length, messageBody.length);

        return transformedRequest;
    }
}