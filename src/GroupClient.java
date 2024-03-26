import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GroupClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final String serverIP;
    private final int serverPort;

    public GroupClient(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void start() {
        try {
            socket = new Socket(serverIP, serverPort);
            System.out.println("Connected to the server. Type your messages.");
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(this::listenToServer).start();

            Scanner userInputScanner = new Scanner(System.in);
            while (true) {
                if (userInputScanner.hasNextLine()) {
                    String userInput = userInputScanner.nextLine();
                    sendMessage(userInput);
                    if ("/quit".equalsIgnoreCase(userInput.trim())) {
                        break; // Exit loop to close client after sending quit command
                    }
                } else {
                    Thread.sleep(100); // Wait a bit before checking again to reduce CPU usage
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            closeEverything();
        }
    }

    private void listenToServer() {
        String fromServer;
        try {
            while ((fromServer = in.readLine()) != null) {
                System.out.println("Server: " + fromServer);
            }
        } catch (IOException e) {
            System.err.println("Server connection closed.");
        }
    }

    private void sendMessage(String message) {
        out.println(message);
    }

    private void closeEverything() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            System.out.println("Client resources closed.");
        } catch (IOException e) {
            System.err.println("Error closing client resources: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java GroupClient <server IP> <server port>");
            return;
        }
        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);

        new GroupClient(serverIP, serverPort).start();
    }
}