import java.io.*;
import java.net.*;
import java.util.*;

public class ClientManager {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public ClientManager(String serverAddress, int serverPort) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }


    public void handleRegister(Scanner scanner) throws IOException {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        out.println("REGISTER");
        out.println(username);
        out.println(password);
        System.out.println("Server response: " + in.readLine());
    }


    public boolean handleLogin(Scanner scanner) throws IOException {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        out.println("LOGIN");
        out.println(username);
        out.println(password);
        System.out.println("Waiting to Login ...");
        String response = in.readLine();
        System.out.println("Server response: " + response);
        return response.startsWith("SUCCESS");
    }


    public void handlePut(Scanner scanner) throws IOException {
        System.out.print("Key: ");
        String key = scanner.nextLine();
        System.out.print("Value: ");
        String value = scanner.nextLine();

        out.println("PUT");
        out.println(key);
        System.out.println("Key sent");
        out.println(value);
        System.out.println("value sent");

        System.out.println("Server response: " + in.readLine());
    }


    public void handleMultiPut(Scanner scanner) throws IOException {
        System.out.print("Enter the number of key-value pairs to insert: ");
        int numPairs = scanner.nextInt();
        scanner.nextLine();

        Map<String, String> pairs = new HashMap<>();

        for (int i = 0; i < numPairs; i++) {
            System.out.print("Enter key " + (i + 1) + ": ");
            String key = scanner.nextLine();
            System.out.print("Enter value for key " + (i + 1) + ": ");
            String value = scanner.nextLine();
            pairs.put(key, value);
        }

        out.println("MULTIPUT");
        out.println(pairs.size()); // Número de pares

        // Enviar os pares chave-valor
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
            out.println(entry.getKey());
            out.println(entry.getValue());
        }

        System.out.println("MultiPut data sent to server.");
        System.out.println("Server response: " + in.readLine());
    }


    public void handleGet(Scanner scanner) throws IOException {
        System.out.print("Key: ");
        String key = scanner.nextLine();

        out.println("GET");
        out.println(key);
        System.out.println("key sent");

        System.out.println("Server response: " + in.readLine());
    }


    public void handleMultiGet(Scanner scanner) throws IOException {
        System.out.print("Enter the number of keys to retrieve: ");
        int numKeys = scanner.nextInt();
        scanner.nextLine();

        // Ler as chaves do utilizador
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < numKeys; i++) {
            System.out.print("Enter key " + (i + 1) + ": ");
            keys.add(scanner.nextLine());
        }

        out.println("MULTIGET");
        out.println(numKeys);

        // Enviar as chaves
        for (String key : keys) {
            out.println(key);
        }

        // Receber o número de pares encontrados
        int numResults = Integer.parseInt(in.readLine());
        System.out.println("Number of results: " + numResults);

        // Ler os pares chave-valor
        for (int i = 0; i < numResults; i++) {
            String key = in.readLine();
            String value = in.readLine();
            if(value.equals("The map does not contain this key"))
                System.out.println("The map does not contain key " + key);
            else
                System.out.println("Key: " + key + ", Value: " + value);
        }
    }


    public void handleLogout() throws IOException {
        out.println("LOGOUT");
        System.out.println("Server response: " + in.readLine());
    }


    public void close() throws IOException {
        socket.close();
    }
}
