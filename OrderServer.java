import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class OrderServer extends JFrame {
    private JTextArea orderArea;
    private ServerSocket serverSocket;
    private ConcurrentHashMap<Integer, Integer> tableTotalAmounts;
    private static final int LINE_LENGTH = 29;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new OrderServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public OrderServer() throws IOException {
        super("Order Server");

        tableTotalAmounts = new ConcurrentHashMap<>();
        orderArea = new JTextArea();
        orderArea.setEditable(false);
        orderArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(orderArea);

        JButton stopButton = new JButton("Stop Server");
        stopButton.addActionListener(e -> stopServer());

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(stopButton, BorderLayout.SOUTH);

        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port number:"));
        serverSocket = new ServerSocket(port);
        orderArea.append("Server started on port: " + port + "\n");

        new Thread(this::acceptClients).start();
    }

    private void acceptClients() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(new OrderHandler(clientSocket)).start();
            } catch (IOException e) {
                orderArea.append("Error accepting client: " + e.getMessage() + "\n");
            }
        }
    }

    private void stopServer() {
        try {
            serverSocket.close();
            orderArea.append("Server stopped.\n");
            System.exit(0);
        } catch (IOException e) {
            orderArea.append("Error stopping server: " + e.getMessage() + "\n");
        }
    }

    private String formatLine(String text) {
        if (text.length() > LINE_LENGTH) {
            return text.substring(0, LINE_LENGTH - 1) + "│";
        }
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < LINE_LENGTH) {
            sb.append(" ");
        }
        sb.append("│");
        return sb.toString();
    }

    private class OrderHandler implements Runnable {
        private Socket clientSocket;

        public OrderHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

                int tableNumber = dis.readInt();
                orderArea.append("New client connected: Table " + tableNumber + "\n");

                while (true) {
                    String message = dis.readUTF();
                    if (message.equals("disconnect")) {
                        orderArea.append("Client disconnected: Table " + tableNumber + "\n");
                        clientSocket.close();
                        break;
                    } else if (message.equals("quit")) {
                        orderArea.append("Client quit: Table " + tableNumber + "\n");
                        clientSocket.close();
                        break;
                    } else {
                        int currentTotal = tableTotalAmounts.getOrDefault(tableNumber, 0);
                        int newOrderTotal = Integer.parseInt(message.split("Total: ")[1].replace("won", "").trim());
                        int newTotal = currentTotal + newOrderTotal;
                        tableTotalAmounts.put(tableNumber, newTotal);

                        String[] orderDetails = message.split("\n");
                        StringBuilder displayMessage = new StringBuilder();
                        displayMessage.append("┌───────────────────────────┐\n");
                        displayMessage.append(formatLine("│ TABLE NO: " + tableNumber)).append("\n");
                        displayMessage.append(formatLine("│")).append("\n");
                        displayMessage.append(formatLine("│ Current order Item:")).append("\n");
                        for (String detail : orderDetails) {
                            if (detail.startsWith("TableNo.")) {
                                String[] parts = detail.split(", ");
                                displayMessage.append(formatLine("│ " + parts[1] + " " + parts[2])).append("\n");
                            }
                        }
                        displayMessage.append(formatLine("│")).append("\n");
                        displayMessage.append(formatLine("│ Current order total:")).append("\n");
                        displayMessage.append(formatLine("│ " + newOrderTotal + " won")).append("\n");
                        displayMessage.append(formatLine("│")).append("\n");
                        displayMessage.append(formatLine("│ Total Ordered:")).append("\n");
                        displayMessage.append(formatLine("│ " + newTotal + " won")).append("\n");
                        displayMessage.append("└───────────────────────────┘\n");

                        orderArea.append(displayMessage.toString());
                    }
                }
            } catch (IOException e) {
                orderArea.append("Client connection error: " + e.getMessage() + "\n");
            }
        }
    }
}
