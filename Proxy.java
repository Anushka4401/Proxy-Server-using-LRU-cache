import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Proxy creates a Server Socket which will wait for connections on the specified port.
 * Once a connection arrives and a socket is accepted, the Proxy creates a RequestHandler object
 * to handle it, using an ExecutorService to manage threads.
 */
public class Proxy implements Runnable {

    // Main method for the program
    public static void main(String[] args) {
        // Create an instance of Proxy and begin listening for connections
        Proxy myProxy = new Proxy(8085);
        myProxy.listen();
    }

    private ServerSocket serverSocket;

    /**
     * Semaphore for Proxy and Console Management System.
     */
    private volatile boolean running = true;

    /**
     * ExecutorService to handle threads more efficiently.
     */
    private final ExecutorService threadPool;

    /**
     * ArrayList of threads that are currently running and servicing requests.
     * This list is required in order to join all threads on closing the server.
     */
    static ArrayList<Thread> servicingThreads;

    /**
     * Create the Proxy Server
     * @param port Port number to run proxy server from.
     */
    public Proxy(int port) {
        // Create an ArrayList to track threads (if needed)
        servicingThreads = new ArrayList<>();

        // Use a fixed thread pool for servicing requests
        threadPool = Executors.newFixedThreadPool(10); // Pool size of 10 threads (adjust as needed)

        // Start dynamic manager on a separate thread.
        new Thread(this).start(); // Starts overridden run() method at bottom

        try {
            // Create the Server Socket for the Proxy
            serverSocket = new ServerSocket(port);
            System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "..");
            running = true;
        } catch (IOException io) {
            System.out.println("IO exception when connecting to client");
            io.printStackTrace();
        }
    }

    /**
     * Listens to port and accepts new socket connections.
     * Passes each connection to the thread pool for handling.
     */
    public void listen() {
        while (running) {
            try {
                // serverSocket.accept() Blocks until a connection is made
                Socket socket = serverSocket.accept();

                // Submit the request handler to the thread pool
                threadPool.submit(new RequestHandler(socket));
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                } else {
                    System.out.println("Server closed");
                }
            }
        }
    }

    /**
     * Shuts down the thread pool and closes the server socket.
     */
    private void closeServer() {
        System.out.println("\nClosing Server..");
        running = false;

        // Gracefully shut down the thread pool
        threadPool.shutdown();

        // Close the ServerSocket
        try {
            System.out.println("Terminating Connection");
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Exception closing proxy's server socket");
            e.printStackTrace();
        }
    }

    /**
     * Creates a management interface for proxy administration.
     * The only available command is "close" to shut down the server.
     */
    @Override
    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            String command;
            while (running) {
                System.out.println("Enter \"close\" to close server.");
                command = scanner.nextLine();

                if ("close".equalsIgnoreCase(command)) {
                    running = false;
                    closeServer();
                } else {
                    System.out.println("Unknown command. Please enter \"close\" to shut down the server.");
                }
            }
        }
    }
}
