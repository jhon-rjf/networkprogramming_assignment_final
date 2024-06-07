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
    private int tableNumber;
    private JCheckBox[] itemChecks;
    private JComboBox<Integer>[] quantityBoxes;
    private JLabel totalLabel;
    private static final Map<String, Integer> itemPrices;

    static {
        itemPrices = new HashMap<>();
        itemPrices.put("Item1 ($10)", 10);
        itemPrices.put("Item2 ($15)", 15);
        itemPrices.put("Item3 ($20)", 20);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String serverAddress = JOptionPane.showInputDialog("Enter server address:");
                int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port number:"));
                int tableNumber = Integer.parseInt(JOptionPane.showInputDialog("Enter table number:"));
                new OrderClient(serverAddress, port, tableNumber).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public OrderClient(String serverAddress, int port, int tableNumber) throws IOException {
        this.tableNumber = tableNumber;
        socket = new Socket(serverAddress, port);
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        dos.writeInt(tableNumber); // 테이블 번호를 서버로 전송
        dos.flush();
    }

    public void start() {
        JFrame frame = new JFrame("Order Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new GridLayout(7, 2));

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
                    StringBuilder message = new StringBuilder();
                    StringBuilder alertMessage = new StringBuilder("TableNo. " + tableNumber);
                    int orderTotal = 0;
                    for (int i = 0; i < itemChecks.length; i++) {
                        if (itemChecks[i].isSelected()) {
                            String item = itemChecks[i].getText();
                            int quantity = (Integer) quantityBoxes[i].getSelectedItem();
                            int price = itemPrices.get(item);
                            orderTotal += price * quantity;
                            message.append("TableNo. ").append(tableNumber).append(", ").append(item).append(", ").append(quantity).append("EA, $").append(price * quantity).append("\n");
                            alertMessage.append(", ").append(item).append(" ").append(quantity).append("EA");
                        }
                    }
                    message.append("Total: $").append(orderTotal);
                    dos.writeUTF(message.toString().trim());
                    dos.flush();
                    JOptionPane.showMessageDialog(frame, alertMessage.toString()); // 알림창에 주문 내역 표시
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
