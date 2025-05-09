import java.io.*;
import java.net.*;
import java.util.*;


class ServerWorker implements Runnable {

    private final Socket socket;
    private final UserManager userManager;
    private final HashMap<String, byte[]> dataMap;
    private final CustomReadWriteLock mapLock;
    private String loggedInUser = null;


    public ServerWorker(Socket socket, UserManager userManager, HashMap<String, byte[]> dataMap, CustomReadWriteLock mapLock) {
        this.socket = socket;
        this.userManager = userManager;
        this.dataMap = dataMap;
        this.mapLock = mapLock;
    }


    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            while (loggedInUser == null) {
                String command = in.readLine();
                if (command == null) break;

                switch (command.toUpperCase()) {
                    case "REGISTER" -> handleRegister(in, out);
                    case "LOGIN" -> handleLogin(in, out);
                    default -> out.println("ERROR: You must REGISTER or LOGIN first.");
                }
            }

            while (loggedInUser != null) {
                String command = in.readLine();
                if (command == null) break;

                switch (command.toUpperCase()) {
                    case "PUT" -> handlePut(in, out);
                    case "GET" -> handleGet(in, out);
                    case "MULTIPUT" -> handleMultiPut(in, out);
                    case "MULTIGET" -> handleMultiGet(in, out);
                    case "LOGOUT" -> {
                        handleLogout(out);
                        return;
                    }
                    default -> out.println("ERROR: Unknown command.");
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (loggedInUser != null) {
                userManager.logoutUser(loggedInUser);
            }
        }
    }


    private void handleLogin(BufferedReader in, PrintWriter out) throws IOException {
        String username = in.readLine();
        String password = in.readLine();
        String response = userManager.loginUser(username, password, socket);

        if (response.startsWith("SUCCESS")) {
            loggedInUser = username; // Armazena o utilizador autenticado
            System.out.println(username + " logged in.");
        }
        out.println(response);
    }


    private void handleLogout(PrintWriter out) {

        userManager.logoutUser(loggedInUser);
        System.out.println(loggedInUser + " logged out.");
        loggedInUser = null;
        out.println("SUCCESS: Logged out.");
    }


    private void handleRegister(BufferedReader in, PrintWriter out) throws IOException {
        String username = in.readLine();
        String password = in.readLine();
        String response = userManager.registerUser(username, password);
        out.println(response);
        System.out.println("Registered: " + username);
    }


    private void handlePut(BufferedReader in, PrintWriter out) throws IOException, InterruptedException {
        String key = in.readLine();
        System.out.println("Put key received");
        String valueString = in.readLine();
        System.out.println("value received");

        // Passar de String a byte[]
        byte[] value = valueString.getBytes();

        mapLock.writeLock();
        System.out.println(loggedInUser + " got read lock");
        try {
            dataMap.put(key, value);
            System.out.println(loggedInUser + " put " + Arrays.toString(dataMap.get(key)));
            out.println("SUCCESS: Value stored.");
        } catch (Exception e) {
            out.println("ERROR: Failed to store value.");
            e.printStackTrace();
        } finally {
            mapLock.writeUnlock();
            System.out.println(loggedInUser + " released write lock");
        }
    }


    private void handleMultiPut(BufferedReader in, PrintWriter out) throws IOException, InterruptedException {
        System.out.println("MultiPut command received from " + loggedInUser);

        // Receber o número de pares chave-valor
        int numPairs = Integer.parseInt(in.readLine());
        System.out.println("Number of pairs to insert: " + numPairs);

        // Criar um mapa para armazenar os pares recebidos
        HashMap<String, byte[]> pairs = new HashMap<>();

        // Receber os pares chave-valor
        for (int i = 0; i < numPairs; i++) {
            String key = in.readLine();
            String valueString = in.readLine();
            System.out.println("Received pair: " + key + " -> " + valueString);

            // Converter o valor de String para byte[]
            byte[] value = valueString.getBytes();
            pairs.put(key, value);
        }

        mapLock.writeLock();
        try {
            dataMap.putAll(pairs);
            System.out.println(loggedInUser + " stored multiple pairs: " + pairs.keySet());
            out.println("SUCCESS: All values stored.");
        } catch (Exception e) {
            out.println("ERROR: Failed to store values.");
            e.printStackTrace();
        } finally {
            mapLock.writeUnlock();
        }
    }


    private void handleGet(BufferedReader in, PrintWriter out) throws IOException, InterruptedException {
        String key = in.readLine();
        System.out.println("Get key received");

        if(!dataMap.containsKey(key)) {
            out.println("ERROR: Key not found.");
            return;
        }

        mapLock.readLock();
        System.out.println(loggedInUser + " got read lock");
        try {
            byte[] value = dataMap.get(key);
            System.out.println(loggedInUser + " got " + Arrays.toString(value));
            if (value == null) {
                out.println("null");
            } else {
                out.println("SUCCESS: " + new String(value));
            }
        } finally {
            mapLock.readUnlock();
            System.out.println(loggedInUser + " released read lock");
        }
    }


    private void handleMultiGet(BufferedReader in, PrintWriter out) throws IOException, InterruptedException {
        System.out.println("MultiGet command received.");

        // Receber o número de chaves
        int numKeys = Integer.parseInt(in.readLine());
        System.out.println("Number of keys to retrieve: " + numKeys);

        // Ler as chaves enviadas pelo cliente
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < numKeys; i++) {
            String key = in.readLine();
            keys.add(key);
            System.out.println("Received key: " + key);
        }

        // Criar um mapa para armazenar os valores correspondentes
        Map<String, String> result = new HashMap<>();

        // Bloqueio de leitura para acessar o mapa
        mapLock.readLock();
        try {
            for (String key : keys) {
                byte[] value = dataMap.get(key);
                if (value != null) {
                    result.put(key, new String(value));
                }
                else {
                    result.put(key, "The map does not contain this key");
                }
            }
        } finally {
            mapLock.readUnlock();
        }

        // Enviar os resultados de volta ao cliente
        out.println(result.size()); // Número de pares encontrados
        for (Map.Entry<String, String> entry : result.entrySet()) {
            out.println(entry.getKey());
            out.println(entry.getValue());
        }
        System.out.println("MultiGet response sent.");
    }
}
