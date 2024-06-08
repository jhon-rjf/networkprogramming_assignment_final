import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent; // 추가
import java.awt.event.ActionListener; // 추가
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OrderServer extends JFrame {
    private JTextArea orderArea;
    private ServerSocket serverSocket;
    private Map<Integer, Integer> tableTotalAmounts;
    private static final int LINE_LENGTH = 29;
    private Connection dbConnection;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    new OrderServer();
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public OrderServer() throws IOException, SQLException {
        super("Order Server");

        // SQLite JDBC 드라이버 로드
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "SQLite JDBC driver not found. Please add the driver to your classpath.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // 데이터베이스 연결 초기화
        dbConnection = DriverManager.getConnection("jdbc:sqlite:orders.db");
        createTableIfNotExists();

        tableTotalAmounts = new HashMap<>();
        orderArea = new JTextArea();
        orderArea.setEditable(false);
        orderArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(orderArea);

        JButton stopButton = new JButton("Stop Server");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });

        JButton settlementButton = new JButton("Settlement");
        settlementButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                settleAccounts();
            }
        });

        JButton showDataButton = new JButton("Show Data");
        showDataButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSettlementData();
            }
        });

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridLayout(1, 3));
        bottomPanel.add(stopButton);
        bottomPanel.add(settlementButton);
        bottomPanel.add(showDataButton);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port number:"));
        serverSocket = new ServerSocket(port);
        orderArea.append("Server started on port: " + port + "\n");

        new Thread(new Runnable() {
            public void run() {
                acceptClients();
            }
        }).start();
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS settlements (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT, table_number INTEGER, total_amount INTEGER)";
        PreparedStatement stmt = dbConnection.prepareStatement(sql);
        stmt.execute();
        stmt.close();
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
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
            orderArea.append("Server stopped.\n");
            System.exit(0);
        } catch (IOException | SQLException e) {
            orderArea.append("Error stopping server: " + e.getMessage() + "\n");
        }
    }

    private String formatLine(String text) {
        if (text.length() > LINE_LENGTH) {
            return text.substring(0, LINE_LENGTH - 1) + "|";
        }
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < LINE_LENGTH) {
            sb.append(" ");
        }
        sb.append("|");
        return sb.toString();
    }

    private void settleAccounts() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());

        try {
            String sql = "INSERT INTO settlements (timestamp, table_number, total_amount) VALUES (?, ?, ?)";
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            for (Map.Entry<Integer, Integer> entry : tableTotalAmounts.entrySet()) {
                stmt.setString(1, timestamp);
                stmt.setInt(2, entry.getKey());
                stmt.setInt(3, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
            stmt.close();
            orderArea.append("Settlement completed at " + timestamp + "\n");
        } catch (SQLException e) {
            orderArea.append("Error during settlement: " + e.getMessage() + "\n");
        }
    }

    private void showSettlementData() {
        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM settlements");

            StringBuilder data = new StringBuilder();
            data.append("No. | Time                | TableNum | Amount\n");
            data.append("----|---------------------|----------|-------\n");

            while (rs.next()) {
                int id = rs.getInt("id");
                String timestamp = rs.getString("timestamp");
                int tableNumber = rs.getInt("table_number");
                int totalAmount = rs.getInt("total_amount");

                data.append(String.format("%-4d| %-20s | %-8d | %-6d\n", id, timestamp, tableNumber, totalAmount));
            }

            orderArea.append(data.toString());
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            orderArea.append("Error retrieving data: " + e.getMessage() + "\n");
        }
    }

    private class OrderHandler implements Runnable {
        private Socket clientSocket;

        public OrderHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

                int tableNumber = dis.readInt();
                orderArea.append("New client connected: Table " + tableNumber + "\n");

                while (true) {
                    String message = dis.readUTF();
                    if (message.equals("disconnect") || message.equals("quit")) {
                        orderArea.append("Client disconnected: Table " + tableNumber + "\n");
                        clientSocket.close();
                        break;
                    } else {
                        int currentTotal = tableTotalAmounts.getOrDefault(tableNumber, 0);
                        int newOrderTotal = Integer.parseInt(message.split("Total: ")[1].replace("won", "").trim());
                        int newTotal = currentTotal + newOrderTotal;
                        tableTotalAmounts.put(tableNumber, newTotal);

                        String[] orderDetails = message.split("\n");
                        StringBuilder displayMessage = new StringBuilder();
                        displayMessage.append("+-----------------------------+\n");
                        displayMessage.append(formatLine("| TABLE NO: " + tableNumber)).append("\n");
                        displayMessage.append(formatLine("|")).append("\n");
                        displayMessage.append(formatLine("| Current order Item:")).append("\n");
                        for (String detail : orderDetails) {
                            if (detail.startsWith("TableNo.")) {
                                String[] parts = detail.split(", ");
                                displayMessage.append(formatLine("| " + parts[1] + " " + parts[2])).append("\n");
                            }
                        }
                        displayMessage.append(formatLine("|")).append("\n");
                        displayMessage.append(formatLine("| Current order total:")).append("\n");
                        displayMessage.append(formatLine("| " + newOrderTotal + " won")).append("\n");
                        displayMessage.append(formatLine("|")).append("\n");
                        displayMessage.append(formatLine("| Total Ordered:")).append("\n");
                        displayMessage.append(formatLine("| " + newTotal + " won")).append("\n");
                        displayMessage.append("+-----------------------------+\n");

                        orderArea.append(displayMessage.toString());
                    }
                }
            } catch (IOException e) {
                orderArea.append("Client connection error: " + e.getMessage() + "\n");
            }
        }
    }
}
