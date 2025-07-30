import java.net.*;
import java.io.*;
import java.util.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Proxy {
    private static int port;
    private static int timeout;
    private static int max_object_size;
    private static int max_cache_size;

    public static void main(String[] args) throws IOException {
        port = Integer.parseInt(args[0]);
        timeout = Integer.parseInt(args[1]);
        max_object_size = Integer.parseInt(args[2]);
        max_cache_size = Integer.parseInt(args[3]);
        
        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            String host = clientSocket.getLocalAddress().getHostAddress();
            InputStream clientInput = clientSocket.getInputStream();
            OutputStream clientOutput = clientSocket.getOutputStream();
            
            HttpRequest request = new HttpRequest(clientInput);
            request.parseMessage();
            String requestStartLine = request.getStartLine();

            Socket originSocket = new Socket(request.getHostname(), request.getPort());
            InputStream originInput = originSocket.getInputStream();
            OutputStream originOutput = originSocket.getOutputStream();

            byte[] transformedRequest = request.getTransformedRequest();
            originOutput.write(transformedRequest);
            originOutput.flush();

            HttpResponse response = new HttpResponse(originInput);
            response.setMethod(request.getMethod());
            response.parseMessage();
            Integer statusCode = response.getStatusCode();
            int messageBodySize = response.getMessageBodySize();
            originSocket.close();

            byte[] transformedResponse = response.getTransformedResponse();
            clientOutput.write(transformedResponse);
            clientOutput.flush();
            clientSocket.close();

            ZonedDateTime date = ZonedDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
            String formattedDate = "[" + date.format(formatter) + " ]";

            String cacheResult = "-";

            // Logging
            System.out.println(host + " " + port + " " + cacheResult + " " + formattedDate + " \"" + requestStartLine + "\" " + statusCode + " " + messageBodySize);
        }
    }
}