import java.net.*;
import java.io.*;
import java.util.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;

public class Proxy {
    private static int port;
    private static int timeout;
    private static int maxObjectSize;
    private static int maxCacheSize;
    private static final int STANDARD_BUFFER_SIZE = 8192;

    private static void forwardData(InputStream input, OutputStream output) throws IOException {
        BufferedInputStream bufferedInput = new BufferedInputStream(input);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(output);

        byte[] buffer = new byte[STANDARD_BUFFER_SIZE];
        int bytesRead = bufferedInput.read(buffer);
        while (bytesRead != -1) {
            bufferedOutput.write(buffer, 0, bytesRead);
            bufferedOutput.flush();
            bytesRead = bufferedInput.read(buffer);
        }
    }

    private static String getTimestamp() {
        ZonedDateTime date = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
        return date.format(formatter);
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        port = Integer.parseInt(args[0]);
        timeout = Integer.parseInt(args[1]);
        maxObjectSize = Integer.parseInt(args[2]);
        maxCacheSize = Integer.parseInt(args[3]);

        Cache.initialize(maxObjectSize, maxCacheSize);

        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            // Open connection with client
            Socket clientSocket = serverSocket.accept();
            clientSocket.setSoTimeout(timeout * 1000);
            String host = clientSocket.getLocalAddress().getHostAddress();   

            InputStream clientInput = clientSocket.getInputStream();
            OutputStream clientOutput = clientSocket.getOutputStream();

            Socket originSocket = null;
            InputStream originInput = null;
            OutputStream originOutput = null;

            try {
                boolean keepAlive = true;   
                while (keepAlive) {
                    String cacheResult = "-";

                    // Receive request from client
                    HttpRequest request = new HttpRequest(clientInput);

                    request.parseStartLine();
                    String requestStartLine = request.getStartLine().trim();

                    String method = request.getMethod();
                    if (method.equals("GET")) {
                        Cache cache = Cache.getInstance();
                        String targetUrl = request.getTarget();
                        if (cache.isCached(targetUrl)) {
                            CacheEntry cacheEntry = cache.getResponse(targetUrl);
                            byte[] cachedResponse = cacheEntry.getMessage();
                            clientOutput.write(cachedResponse);
                            clientOutput.flush();

                            cacheResult = "H";
                            // Logging for cache hit
                            System.out.println(host + " " + port + " " + cacheResult +
                                " [" + getTimestamp() + "] \"" + requestStartLine +
                                "\" 200 "+ cacheEntry.getSize());
                            
                            continue;
                        }
                    }

                    request.parseMessage();

                    // Open connection with server
                    originSocket = new Socket(request.getHostname(), request.getPort());
                    originSocket.setSoTimeout(timeout * 1000);
                    originInput = originSocket.getInputStream();
                    originOutput = originSocket.getOutputStream();

                    if (method.equals("CONNECT")) {
                        clientOutput.write(("HTTP/1.1 200 Connection Established\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                        clientOutput.flush();

                        final InputStream finalClientInput = clientInput;
                        final OutputStream finalOriginOutput = originOutput;
                        final InputStream finalOriginInput = originInput;
                        final OutputStream finalClientOutput = clientOutput;

                        Thread clientToServer = new Thread(() -> {
                            try {
                                forwardData(finalClientInput, finalOriginOutput);
                            } catch (IOException e) {
                                // Exceeption does not need to be handled
                            }
                        });

                        Thread serverToClient = new Thread(() -> {
                            try {
                                forwardData(finalOriginInput, finalClientOutput);                
                            } catch (IOException e) {
                                // Exceeption does not need to be handled
                            }
                        });

                        serverToClient.start();
                        clientToServer.start();

                        serverToClient.join();
                        clientToServer.join();
                        
                        // Logging for CONNECT
                        System.out.println(host + " " + port +" - [" +getTimestamp() +
                            "] \"" + requestStartLine + "\" 200 0");
                    } else {
                        String connection = request.getConnectionType();

                        // Transform request and send to server
                        byte[] transformedRequest = request.getTransformedRequest();
                        originOutput.write(transformedRequest);
                        originOutput.flush();

                        // Receive response from server
                        HttpResponse response = new HttpResponse(originInput);
                        response.setMethod(method);
                        response.parseMessage();
                        response.setConnectionType(connection);

                        // Close connection with server
                        originSocket.close();

                        // Transform response and send to client
                        byte[] transformedResponse = response.getTransformedResponse();

                        int messageBodySize = response.getMessageBodySize();
                        if (method.equals("GET")) {
                            cacheResult = "M";
                            Cache cache = Cache.getInstance();
                            String targetUrl = request.getTarget();
                            cache.storeResponse(transformedResponse, messageBodySize, targetUrl);
                        }
                        clientOutput.write(transformedResponse);
                        clientOutput.flush();

                        Integer statusCode = response.getStatusCode();

                        // Logging upon compeltion of HTTP transaction
                        System.out.println(host + " " + port + " " + cacheResult +
                            " [" + getTimestamp() + "] \"" + requestStartLine + "\" " +
                            statusCode + " " + messageBodySize);

                        // Close connection if not persistent
                        if (!request.isPersistent()) {
                            keepAlive = false;
                        }
                    }
                }
            } catch (SocketTimeoutException e) {
                if (originInput != null) {
                    originInput.close();
                }

                if (originOutput != null) {
                    originOutput.close();
                }
                
                if (originSocket != null) {
                    originSocket.close();
                }

                if (clientInput != null) {
                    clientInput.close();
                }
                
                if (clientOutput != null) {
                    clientOutput.close();
                }

                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                continue;
            } finally {
                clientInput.close();
                clientOutput.close();
                clientSocket.close();
            }
        }
    }
}