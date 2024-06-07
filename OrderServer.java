import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

public class OrderServer {
    private ServerSocket serverSocket;
    private List<OrderHandler> clients = new ArrayList<>();
    private DefaultTableModel tableModel;
    private boolean running = true;

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
        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton stopButton = new JButton("Stop Server");
        stopButton.addActionListener(e -> stopServer(frame));
        frame.add(stopButton, BorderLayout.SOUTH);

        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                stopServer(frame);
            }
        });
        frame.setVisible(true);
    }

    public void start() {
        System.out.println("Order Server started. Waiting for clients...");
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                OrderHandler handler = new OrderHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
        stopServer(null);
    }

    public void stopServer(JFrame frame) {
        running = false;
        try {
            for (OrderHandler handler : clients) {
                handler.stop();
            }
            serverSocket.close();
            if (frame != null) {
                frame.dispose();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class OrderHandler implements Runnable {
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private boolean running = true;

        public OrderHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
                while (running) {
                    String message = dis.readUTF();
                    String[] parts = message.split(",");
                    int tableNumber = Integer.parseInt(parts[0]);
                    String item = parts[1];
                    int quantity = Integer.parseInt(parts[2]);
                    SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{tableNumber, item, quantity}));
                    dos.writeUTF("Order received: " + message);
                }
            } catch (EOFException e) {
                System.out.println("Client disconnected");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clients.remove(this);
            }
        }

        public void stop() {
            running = false;
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
