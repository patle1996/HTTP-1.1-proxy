import java.io.*;
import java.net.*;
import java.util.*;

public abstract class HttpMessage {
    private String method;
    private String protocolVersion;
    private Map<String, String> headers = new HashMap<>();
    private byte[] messageBody;

    public void parseHeaders(BufferedReader bufferedHeaders) throws IOException{
        String line = bufferedHeaders.readLine();
        int colonIdx;
        while (line != null && !line.isEmpty()) {
            colonIdx = line.indexOf(':');
            headers.put(line.substring(0, colonIdx).trim().toLowerCase(), line.substring(colonIdx + 1));
            line = bufferedHeaders.readLine();
        }
    }

    public abstract void parseStartLine(String startLine);

    public void readMessageBodyByLength(InputStream inputStream) throws IOException {
        int length = Integer.parseInt(getHeaders().get("content-length"));
        this.messageBody = inputStream.readNBytes(length);
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getMessageBody() {
        return messageBody;
    }

    public int getMessageBodySize() {
        return messageBody.length;
    }

    public void setMessageBody(InputStream messageBody) {
        this.messageBody = messageBody;
    }
}