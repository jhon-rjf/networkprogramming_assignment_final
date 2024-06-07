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
    private JLabel currentOrderLabel;
    private JLabel totalOrderLabel;
    private int totalAmount = 0;
    private static final Map<String, Integer> itemPrices;

    static {
        itemPrices = new HashMap<>();
        itemPrices.put("Jin_ramen(H) (950won)", 950);
        itemPrices.put("JJapagetti (1500won)", 1500);
        itemPrices.put("Shin_ramen (1300won)", 1300);
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
        connect(serverAddress, port);
    }

    public void start() {
        JFrame frame = new JFrame("Order Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 800);  // Adjusted size to accommodate images
        frame.setLayout(new GridLayout(10, 2));

        itemChecks = new JCheckBox[3];
        quantityBoxes = new JComboBox[3];
        String[] itemImages = { "./3.png", "./2.png", "./1.png" }; // 이미지 파일 경로
        Dimension[] imageSizes = { new Dimension(74, 62), new Dimension(74, 62), new Dimension(62, 74) }; // 이미지 크기
        int index = 0;
        for (String item : itemPrices.keySet()) {
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemChecks[index] = new JCheckBox(item);
            quantityBoxes[index] = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
            itemChecks[index].addActionListener(e -> updateCurrentOrder());
            quantityBoxes[index].addActionListener(e -> updateCurrentOrder());

            try {
                ImageIcon icon = new ImageIcon(new ImageIcon(itemImages[index]).getImage().getScaledInstance(imageSizes[index].width, imageSizes[index].height, Image.SCALE_SMOOTH));
                JLabel imageLabel = new JLabel(icon);
                itemPanel.add(imageLabel, BorderLayout.WEST);
            } catch (Exception e) {
                e.printStackTrace();
            }

            itemPanel.add(itemChecks[index], BorderLayout.CENTER);
            itemPanel.add(quantityBoxes[index], BorderLayout.EAST);
            frame.add(itemPanel);
            index++;
        }

        JButton orderButton = new JButton("Order");
        frame.add(orderButton);

        currentOrderLabel = new JLabel("Current Order: 0won");
        frame.add(currentOrderLabel);

        totalOrderLabel = new JLabel("Total Ordered: 0won");
        frame.add(totalOrderLabel);

        JButton disconnectButton = new JButton("Disconnect");
        frame.add(disconnectButton);

        JButton quitButton = new JButton("Quit");
        frame.add(quitButton);

        orderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    StringBuilder message = new StringBuilder();
                    StringBuilder alertMessage = new StringBuilder("TableNo. " + tableNumber);
                    int currentOrderTotal = 0;
                    for (int i = 0; i < itemChecks.length; i++) {
                        if (itemChecks[i].isSelected()) {
                            String item = itemChecks[i].getText();
                            int quantity = (Integer) quantityBoxes[i].getSelectedItem();
                            int price = itemPrices.get(item);
                            currentOrderTotal += price * quantity;
                            message.append("TableNo. ").append(tableNumber).append(", ").append(item).append(", ").append(quantity).append("EA, ").append(price * quantity).append("won\n");
                            alertMessage.append(", ").append(item).append(" ").append(quantity).append("EA");
                        }
                    }
                    totalAmount += currentOrderTotal;
                    message.append("Total: ").append(currentOrderTotal).append("won");
                    dos.writeUTF(message.toString().trim());
                    dos.flush();
                    JOptionPane.showMessageDialog(frame, alertMessage.toString()); // 알림창에 주문 내역 표시
                    totalOrderLabel.setText("Total Ordered: " + totalAmount + "won");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dos.writeUTF("disconnect");
                    dos.flush();
                    JOptionPane.showMessageDialog(frame, "Disconnected. Total amount: " + totalAmount + "won");
                    socket.close();
                    frame.dispose();
                    SwingUtilities.invokeLater(() -> {
                        try {
                            String serverAddress = JOptionPane.showInputDialog("Enter server address:");
                            int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port number:"));
                            new OrderClient(serverAddress, port).start();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dos.writeUTF("quit");
                    dos.flush();
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                frame.dispose();
            }
        });

        frame.setVisible(true);
    }

    private void connect(String serverAddress, int port) throws IOException {
        tableNumber = Integer.parseInt(JOptionPane.showInputDialog("Enter table number:"));
        socket = new Socket(serverAddress, port);
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        dos.writeInt(tableNumber); // 테이블 번호를 서버로 전송
        dos.flush();
    }

    private void updateCurrentOrder() {
        int currentOrderTotal = 0;
        for (int i = 0; i < itemChecks.length; i++) {
            if (itemChecks[i].isSelected()) {
                String item = itemChecks[i].getText();
                int quantity = (Integer) quantityBoxes[i].getSelectedItem();
                currentOrderTotal += itemPrices.get(item) * quantity;
            }
        }
        currentOrderLabel.setText("Current Order: " + currentOrderTotal + "won");
    }
}
