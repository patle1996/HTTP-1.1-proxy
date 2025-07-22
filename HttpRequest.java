import java.io.*;
import java.net.*;
import java.util.*;

create public class HttpRequest extends HttpMessage {
    String method;
    String target;
    String protocolVersion;
    String hostname;
    int port;
    String path;

    private static void parseTarget() {
        URL targetUrl = new URL(target);
        this.hostname = targetUrl.getHost();
        this.port = targetUrl.getPort() == -1 ? targetUrl.getDefaultPort() : targetUrl.getPort();
        this.path = targetUrl.getFile();
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

    public HttpRequest(InputStream request) {
        InputStreamReader requestReader = new InputStreamReader(request)
        BufferedReader bufferedRequest = new BufferedReader(requestReader);
        Map<String, String> parsedRequest = new HashMap<>();
        String line = bufferedRequest.readLine();

        parseStartLine(line);
        super.parseHeaders(bufferedRequest);
        if (headers.containsKey("content-length")) {
            readMessageBody(request);
        }
    }
}