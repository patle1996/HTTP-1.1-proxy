import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public abstract class HttpMessage {
    private BufferedInputStream bufferedMessage;
    private String startLine;
    private String method;
    private String protocolVersion;
    private Map<String, String> headers = new HashMap<>();
    private byte[] messageBody;
    private String connectionType;

    public HttpMessage(InputStream message) {
        this.bufferedMessage = new BufferedInputStream(message);
    }

    public abstract void parseMessage() throws IOException;


    public String readLine() throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        BufferedInputStream bufferedMessage = getBufferedMessage();
        int currByte = bufferedMessage.read();
        boolean seenCr = false;

        // Continue to read bytes until CRLF is reached
        while (currByte != -1) {
            if (seenCr) {
                if (currByte == '\n') {
                    break;
                }
                lineBuffer.write('\r');
                seenCr = false;
            }

            if (currByte == '\r') {
                seenCr = true;
            } else {
                lineBuffer.write(currByte);
            }
            currByte = bufferedMessage.read();
        }
        return new String(lineBuffer.toByteArray(), StandardCharsets.US_ASCII);
    }

    public void parseHeaders() throws IOException{
        String line = readLine();
        int colonIdx;

        // Continue to read lines until an empty line is reached signalling the end of the headers
        while (line != null && !line.isEmpty()) {
            colonIdx = line.indexOf(':');

            String key = line.substring(0, colonIdx).trim().toLowerCase();
            String value = line.substring(colonIdx + 1);
            headers.put(key, value);
            
            line = readLine();
        }
    }

    public abstract void parseStartLine() throws MalformedURLException, IOException;

    public void readMessageBodyByLength() throws IOException {
        BufferedInputStream bufferedMessage = getBufferedMessage();
        Integer length = Integer.parseInt(getHeaders().get("content-length").trim());
        byte[] messageBody = new byte[length];
        int totalRead = 0;

        // Continue to read bytes into messageBody until required length is reached
        while (totalRead < length) {
            int bytesRead = bufferedMessage.read(messageBody, totalRead, length - totalRead);

            if (bytesRead == -1) {
                if (totalRead < length) {
                    throw new IOException("Expected " + length + " bytes, but only read " + totalRead);
                }
                break;
            }
            totalRead += bytesRead;
        }
        this.messageBody = messageBody;
    }

    public void setStartLine(String startLine) {
        this.startLine = startLine;
    }

    public String getStartLine() {
        return startLine;
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

    public void setMessageBody(byte[] messageBody) {
        this.messageBody = messageBody;
    }

    public BufferedInputStream getBufferedMessage() {
        return bufferedMessage;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public boolean isPersistent() {
        if (getConnectionType() == null) {
            return true;
        }
        return !connectionType.trim().equals("close");
    }
}