import java.net.*;
import java.io.*;
import java.util.*;

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

            InputStream clientInput = clientSocket.getInputStream();
            OutputStream clientOutput = clientSocket.getOutputStream();
            
            HttpRequest request = new HttpRequest(clientInput);
            byte[] transformedRequest = request.getTransformedRequest();
            clientOutput.write(transformedRequest);
            clientOutput.flush();
        }
    }
}