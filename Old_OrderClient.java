import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class OrderClient {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private JTextField tableField;
    private JComboBox<String> itemBox;
    private JSpinner quantitySpinner;
    private JLabel totalLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String serverAddress = JOptionPane.showInputDialog("Enter server address:");
                int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port number:"));
                new OrderClient(serverAddress, port).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public OrderClient(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }

    public void start() {
        JFrame frame = new JFrame("Order Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(new GridLayout(5, 2));

        frame.add(new JLabel("Table Number:"));
        tableField = new JTextField();
        frame.add(tableField);

        frame.add(new JLabel("Item:"));
        itemBox = new JComboBox<>(new String[]{"Item1 - $10", "Item2 - $15", "Item3 - $20"});
        frame.add(itemBox);

        frame.add(new JLabel("Quantity:"));
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        frame.add(quantitySpinner);

        JButton orderButton = new JButton("Order");
        frame.add(orderButton);

        totalLabel = new JLabel("Total: $0");
        frame.add(totalLabel);

        orderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int tableNumber = Integer.parseInt(tableField.getText());
                    String item = (String) itemBox.getSelectedItem();
                    int quantity = (Integer) quantitySpinner.getValue();
                    int price = getPrice(item);
                    int total = price * quantity;
                    totalLabel.setText("Total: $" + total);
                    String message = tableNumber + "," + item.split(" - ")[0] + "," + quantity;
                    dos.writeUTF(message);
                    dos.flush();
                    String response = dis.readUTF();
                    JOptionPane.showMessageDialog(frame, response);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        frame.setVisible(true);
    }

    private int getPrice(String item) {
        switch (item.split(" - ")[0]) {
            case "Item1":
                return 10;
            case "Item2":
                return 15;
            case "Item3":
                return 20;
            default:
                return 0;
        }
    }
}

