import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class OrderServer {
    private ServerSocket serverSocket;
    private List<OrderHandler> clients = new ArrayList<>();
    private DefaultTableModel tableModel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port number:"));
                new OrderServer(port).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public OrderServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        JFrame frame = new JFrame("Order Server");
        tableModel = new DefaultTableModel(new Object[]{"Table Number", "Item", "Quantity"}, 0);
        JTable table = new JTable(tableModel);
        frame.add(new JScrollPane(table));
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void start() {
        System.out.println("Order Server started. Waiting for clients...");
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                OrderHandler handler = new OrderHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class OrderHandler implements Runnable {
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;

        public OrderHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
                while (true) {
                    String message = dis.readUTF();
                    String[] parts = message.split(",");
                    int tableNumber = Integer.parseInt(parts[0]);
                    String item = parts[1];
                    int quantity = Integer.parseInt(parts[2]);
                    tableModel.addRow(new Object[]{tableNumber, item, quantity});
                    dos.writeUTF("Order received: " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

