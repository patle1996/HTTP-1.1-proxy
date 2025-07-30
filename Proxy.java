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
            // Open connection with client
            Socket clientSocket = serverSocket.accept();
            String host = clientSocket.getLocalAddress().getHostAddress();
            InputStream clientInput = clientSocket.getInputStream();
            OutputStream clientOutput = clientSocket.getOutputStream();
            
            // Receive request from client
            HttpRequest request = new HttpRequest(clientInput);
            // System.out.println("Parsing message...");
            request.parseMessage();
            // System.out.println("Immediately after parsing, connection type is: " + request.getConnectionType());

            String requestStartLine = request.getStartLine().trim();
            String connection = request.getConnectionType();

            Socket originSocket = new Socket(request.getHostname(), request.getPort());
            originSocket.setSoTimeout(timeout * 1000);
            InputStream originInput = originSocket.getInputStream();
            OutputStream originOutput = originSocket.getOutputStream();

            // Transform request and send to server
            byte[] transformedRequest = request.getTransformedRequest();
            originOutput.write(transformedRequest);
            originOutput.flush();

            // Receive response from server
            HttpResponse response = new HttpResponse(originInput);
            response.setMethod(request.getMethod());
            response.parseMessage();
            response.setConnectionType(connection);

            Integer statusCode = response.getStatusCode();
            int messageBodySize = response.getMessageBodySize();

            // Close connection with server
            originSocket.close();

            // Transform response and send to client
            byte[] transformedResponse = response.getTransformedResponse();
            clientOutput.write(transformedResponse);
            clientOutput.flush();

            ZonedDateTime date = ZonedDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
            String formattedDate = "[" + date.format(formatter) + "]";

            String cacheResult = "-";

            // Logging upon compeltion of HTTP transaction
            System.out.println(host + " " + port + " " + cacheResult + " " + formattedDate + " \" " + requestStartLine + "\" " + statusCode + " " + messageBodySize);

            // Close connection if not persistent
            if (!request.isPersistent()) {
                clientSocket.close();
            }
        }
    }
}