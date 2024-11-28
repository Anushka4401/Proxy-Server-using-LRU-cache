
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.ImageIO;

public class RequestHandler implements Runnable {

    // Socket and stream definitions
    Socket clientSocket;
    BufferedReader proxyToClientBr;
    BufferedWriter proxyToClientBw;

    // LRU Cache to store responses (URLs mapped to content)
    private static final Map<String, Object> cache = new LinkedHashMap<>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            return size() > 100; // Cache size limit: 100 entries
        }
    };

    public RequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            this.clientSocket.setSoTimeout(10000);
            proxyToClientBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String requestString;
        try {
            requestString = proxyToClientBr.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error reading request from client");
            return;
        }

        System.out.println("Request Received: " + requestString);
        String request = requestString.substring(0, requestString.indexOf(' '));
        String urlString = requestString.substring(requestString.indexOf(' ') + 1);

        urlString = urlString.substring(0, urlString.indexOf(' '));
        if (!urlString.substring(0, 4).equals("http")) {
            urlString = "http://" + urlString;
        }

        if (request.equals("GET")) {
            System.out.println("HTTP GET for: " + urlString + "\n");
            sendToClient(urlString);
        } else {
            System.out.println("Unsupported request: " + request);
        }
    }

    private void sendToClient(String urlString) {
        try {
            // Check cache for the requested URL
            if (cache.containsKey(urlString)) {
                System.out.println("Cache hit for: " + urlString);
                Object cachedContent = cache.get(urlString);

                if (cachedContent instanceof BufferedImage) {
                    sendImageToClient((BufferedImage) cachedContent, getFileExtension(urlString));
                } else if (cachedContent instanceof String) {
                    sendTextToClient((String) cachedContent);
                }
                return;
            }

            // Identify the file type (image or HTML)
            String fileExtension = getFileExtension(urlString);

            if (fileExtension.matches("\\.(png|jpg|jpeg|gif)")) {
                // Fetch and cache image
                URL remoteURL = new URL(urlString);
                BufferedImage image = ImageIO.read(remoteURL);

                if (image != null) {
                    cache.put(urlString, image); // Add to cache
                    sendImageToClient(image, fileExtension);
                } else {
                    sendNotFound();
                }
            } else {
                // Fetch and cache HTML
                URL remoteURL = new URL(urlString);
                HttpURLConnection proxyToServerCon = (HttpURLConnection) remoteURL.openConnection();
                proxyToServerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                proxyToServerCon.setRequestProperty("Content-Language", "en-US");
                proxyToServerCon.setUseCaches(false);
                proxyToServerCon.setDoOutput(true);

                BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();

                String line;
                while ((line = proxyToServerBR.readLine()) != null) {
                    responseBuilder.append(line).append("\n");
                }
                proxyToServerBR.close();

                String response = responseBuilder.toString();
                cache.put(urlString, response); // Add to cache
                sendTextToClient(response);
            }

            proxyToClientBw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendImageToClient(BufferedImage image, String fileExtension) throws IOException {
        String response = "HTTP/1.0 200 OK\n" +
                "Proxy-agent: ProxyServer/1.0\n" +
                "\r\n";
        proxyToClientBw.write(response);
        proxyToClientBw.flush();
        ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());
    }

    private void sendTextToClient(String response) throws IOException {
        String header = "HTTP/1.0 200 OK\n" +
                "Proxy-agent: ProxyServer/1.0\n" +
                "\r\n";
        proxyToClientBw.write(header);
        proxyToClientBw.write(response);
        proxyToClientBw.flush();
    }

    private String getFileExtension(String urlString) {
        int fileExtensionIndex = urlString.lastIndexOf(".");
        String fileExtension = urlString.substring(fileExtensionIndex);
        if (fileExtension.contains("/") || fileExtension.isEmpty()) {
            return ".html";
        }
        return fileExtension;
    }

    private void sendNotFound() throws IOException {
        String error = "HTTP/1.0 404 NOT FOUND\n" +
                "Proxy-agent: ProxyServer/1.0\n" +
                "\r\n";
        proxyToClientBw.write(error);
        proxyToClientBw.flush();
    }
}
