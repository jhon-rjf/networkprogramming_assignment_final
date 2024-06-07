import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class OrderClient {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private JTextField tableField;
    private JCheckBox[] itemChecks;
    private JComboBox<Integer>[] quantityBoxes;
    private JLabel totalLabel;
    private static final Map<String, Integer> itemPrices;

    static {
        itemPrices = new HashMap<>();
        itemPrices.put("Item1", 10);
        itemPrices.put("Item2", 15);
        itemPrices.put("Item3", 20);
    }

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
        frame.setSize(400, 300);
        frame.setLayout(new GridLayout(7, 2));

        frame.add(new JLabel("Table Number:"));
        tableField = new JTextField();
        frame.add(tableField);

        itemChecks = new JCheckBox[3];
        quantityBoxes = new JComboBox[3];
        int index = 0;
        for (String item : itemPrices.keySet()) {
            itemChecks[index] = new JCheckBox(item);
            quantityBoxes[index] = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
            itemChecks[index].addActionListener(e -> updateTotal());
            quantityBoxes[index].addActionListener(e -> updateTotal());
            frame.add(itemChecks[index]);
            frame.add(quantityBoxes[index]);
            index++;
        }

        JButton orderButton = new JButton("Order");
        frame.add(orderButton);

        totalLabel = new JLabel("Total: $0");
        frame.add(totalLabel);

        orderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int tableNumber = Integer.parseInt(tableField.getText());
                    StringBuilder message = new StringBuilder();
                    for (int i = 0; i < itemChecks.length; i++) {
                        if (itemChecks[i].isSelected()) {
                            String item = itemChecks[i].getText();
                            int quantity = (Integer) quantityBoxes[i].getSelectedItem();
                            message.append(tableNumber).append(",").append(item).append(",").append(quantity).append("\n");
                        }
                    }
                    dos.writeUTF(message.toString().trim());
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

    private void updateTotal() {
        int total = 0;
        for (int i = 0; i < itemChecks.length; i++) {
            if (itemChecks[i].isSelected()) {
                String item = itemChecks[i].getText();
                int quantity = (Integer) quantityBoxes[i].getSelectedItem();
                total += itemPrices.get(item) * quantity;
            }
        }
        totalLabel.setText("Total: $" + total);
    }
}
