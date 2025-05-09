import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class UserManager {
    private final HashMap<String, String> userMap = new HashMap<>();
    private final HashSet<String> activeSessions = new HashSet<>();
    private final Queue<Condition> waitingQueue = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final int maxConcurrentUsers;
    private int currentUsers = 0;

    public UserManager(int maxConcurrentUsers) {
        this.maxConcurrentUsers = maxConcurrentUsers;
    }

    public String registerUser(String username, String password) {
        lock.lock();
        try {
            if (userMap.containsKey(username)) {
                return "ERROR: User already exists.";
            }
            userMap.put(username, password);
            return "SUCCESS: User registered.";
        } finally {
            lock.unlock();
        }
    }

    public String loginUser(String username, String password, Socket socket) {
        lock.lock();
        Condition myCondition = lock.newCondition();
        boolean addedToQueue = false;

        try {
            // Adicionar à fila de espera se necessário
            while (currentUsers >= maxConcurrentUsers) {
                if (!addedToQueue) {
                    waitingQueue.add(myCondition);
                    addedToQueue = true;
                }
                myCondition.await();
            }

            // Remover da fila
            waitingQueue.remove(myCondition);

            // Validar login
            if (activeSessions.contains(username)) {

                // Dá signal ao próximo na fila em caso de login errado
                if (!waitingQueue.isEmpty()) {
                    waitingQueue.peek().signal();
                }
                return "ERROR: User already logged in.";
            }

            if (userMap.containsKey(username) && userMap.get(username).equals(password)) {
                activeSessions.add(username);
                currentUsers++;
                return "SUCCESS: Logged in.";
            }

            // Dá signal ao próximo na fila em caso de login errado
            if (!waitingQueue.isEmpty()) {
                waitingQueue.peek().signal();
            }
            return "ERROR: Invalid credentials.";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "ERROR: Login interrupted.";
        } finally {
            lock.unlock();
        }
    }

    public void logoutUser(String username) {
        lock.lock();
        try {
            if (activeSessions.remove(username)) {
                currentUsers--;
                // Notificar o próximo na fila
                if (!waitingQueue.isEmpty()) {
                    waitingQueue.peek().signal();
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
