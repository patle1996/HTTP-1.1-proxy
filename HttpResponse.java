import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class HttpResponse extends HttpMessage {
    private static final int STANDARD_BUFFER_SIZE = 8192;
    private static final String ZID = "z5414008";

    private int statusCode;
    private String reason;

    public HttpResponse(InputStream response) {
        super(response);
    }

    @Override
    public void parseMessage() throws IOException {
        parseStartLine();

        parseHeaders();

        if (!getMethod().equals("HEAD") && statusCode != 204 && statusCode != 304) {
            if (getHeaders().containsKey("transfer-encoding")) {
                readMessageBodyByStream();
            } else if (getHeaders().containsKey("content-length")) {
                readMessageBodyByLength();
            } else {
                readMessageBodyByStream();
            }
        }
    }

    @Override
    public void parseStartLine() throws IOException {
        String line = readLine();
        setStartLine(line);
        
        String[] startLineData = line.trim().split("\\s+", 3);
        setProtocolVersion(startLineData[0]);
        this.statusCode = Integer.parseInt(startLineData[1]);
        if (startLineData.length == 3) {
            this.reason = startLineData[2];
        }
    }

    public void readMessageBodyByStream() throws IOException {
        BufferedInputStream bufferedResponse = getBufferedMessage();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[STANDARD_BUFFER_SIZE];
        int bytesRead = bufferedResponse.read(temp);

        while (bytesRead != -1) {
            buffer.write(temp, 0, bytesRead);
            bytesRead = bufferedResponse.read(temp);
        }

        setMessageBody(buffer.toByteArray());
    }

    // Transforms the response to be sent to the client
    public byte[] getTransformedResponse() {
        String headersString = getStartLine() + "\r\n";
        for (Map.Entry<String, String> header : getHeaders().entrySet()) {
            // Close connection
            if (header.getKey().equals("connection")) {
                continue;
            }

            headersString += header.getKey() + ":" + header.getValue() + "\r\n";
        }

        if (getConnectionType() != null) {
            headersString += "connection:" + getConnectionType() + "\r\n";
        }
        headersString += "via: 1.1 " + ZID + "\r\n\r\n";

        // If no message body, return start line + headers
        byte[] headersStringBytes = headersString.getBytes(StandardCharsets.US_ASCII);
        if (getMessageBody() == null) {
            return headersStringBytes;
        }

        // If message body exists, make a new byte array and return all data
        int size = getMessageBody().length + headersStringBytes.length;
        byte[] transformedResponse = new byte[size];
        
        System.arraycopy(headersStringBytes, 0, transformedResponse, 0, headersStringBytes.length);
        System.arraycopy(getMessageBody(), 0, transformedResponse, headersStringBytes.length, getMessageBody().length);

        return transformedResponse;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public int getMessageBodySize() {
        if (getMessageBody() == null) {
            return 0;
        }
        return getMessageBody().length;
    }
}