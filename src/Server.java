import java.io.*;
import java.net.*;
import java.util.HashMap;


public class Server {
    private static final HashMap<String, byte[]> dataMap = new HashMap<>(); // Mapa compartilhado
    private static final CustomReadWriteLock mapLock = new CustomReadWriteLock(); // Lock para leitura/escrita
    private static final int MAX_CONCURRENT_USERS = 2;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12345);
        UserManager userManager = new UserManager(MAX_CONCURRENT_USERS);
        System.out.println("Server listening on port 12345...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected");
            Thread worker = new Thread(new ServerWorker(clientSocket, userManager, dataMap, mapLock));
            worker.start();
        }
    }
}

