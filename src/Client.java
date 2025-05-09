import java.io.IOException;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            ClientManager clientManager = new ClientManager("localhost", 12345);

            while (true) {
                printLoginMenu();
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1 -> clientManager.handleRegister(scanner);
                    case 2 -> {
                        if (clientManager.handleLogin(scanner)) {
                            handlePostLoginMenu(clientManager, scanner);
                            System.out.println("Goodbye!");
                            return;
                        }
                    }
                    case 3 -> {
                        System.out.println("Goodbye!");
                        clientManager.close();
                        return;
                    }
                    default -> System.out.println("Invalid choice!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handlePostLoginMenu(ClientManager clientManager, Scanner scanner) throws IOException {
        while (true) {
            printPostLoginMenu();
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1 -> clientManager.handlePut(scanner);
                case 2 -> clientManager.handleGet(scanner);
                case 3 -> clientManager.handleMultiPut(scanner);
                case 4 -> clientManager.handleMultiGet(scanner);
                case 5 -> {
                    clientManager.handleLogout();
                    return; // Voltar ao menu principal
                }
                default -> System.out.println("Invalid choice!");
            }
        }
    }

    private static void printLoginMenu() {
        System.out.println("Choose:\n[1] Register\n[2] Login\n[3] Exit");
    }

    private static void printPostLoginMenu() {
        System.out.println("Choose:\n[1] Put Data\n[2] Get Data\n[3] MultiPut\n[4] MultiGet\n[5] Logout");
    }

}
