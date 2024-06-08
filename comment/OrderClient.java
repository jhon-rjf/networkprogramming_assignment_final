/*-----------------------------------------------------------------------------------
compile - javac OrderClient.java
run - java OrderClient
-----------------------------------------------------------------------------------*/
import javax.swing.*; // GUI를 만들기 위해 필요한 라이브러리
import java.awt.*; // 레이아웃을 설정하기 위해 필요한 라이브러리
import java.awt.event.ActionEvent; // 버튼 클릭 이벤트 처리를 위한 라이브러리
import java.awt.event.ActionListener; // 버튼 클릭 이벤트 처리를 위한 라이브러리
import java.io.*; // 입출력 관련 라이브러리
import java.net.Socket; // 소켓 통신을 위해 필요한 라이브러리
import java.util.HashMap; // 맵 자료구조를 사용하기 위해 필요한 라이브러리
import java.util.Map; // 맵 자료구조를 사용하기 위해 필요한 라이브러리

public class OrderClient {
    private Socket socket; // 서버와 통신하기 위한 소켓
    private DataInputStream dis; // 서버로부터 데이터를 읽기 위한 스트림
    private DataOutputStream dos; // 서버로 데이터를 보내기 위한 스트림
    private int tableNumber; // 테이블 번호
    private JCheckBox[] itemChecks; // 메뉴 아이템을 선택할 체크박스 배열
    private JComboBox<Integer>[] quantityBoxes; // 각 아이템의 수량을 선택할 콤보박스 배열
    private JLabel currentOrderLabel; // 현재 주문 금액을 표시할 레이블
    private JLabel totalOrderLabel; // 총 주문 금액을 표시할 레이블
    private int totalAmount = 0; // 총 주문 금액을 저장할 변수
    private static final Map<String, Integer> itemPrices; // 아이템 가격을 저장할 맵

    static {
        itemPrices = new HashMap<>();
        itemPrices.put("Jin_ramen(H) (950won)", 950); // 아이템 가격 설정
        itemPrices.put("JJapagetti (1500won)", 1500); // 아이템 가격 설정
        itemPrices.put("Shin_ramen (1300won)", 1300); // 아이템 가격 설정
    }

    // 프로그램의 시작점, GUI 스레드실행
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String serverAddress = JOptionPane.showInputDialog("Enter server address:"); // 서버 주소 입력받기
                int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port number:")); // 포트 번호 입력받기
                new OrderClient(serverAddress, port).start(); // OrderClient 인스턴스 생성 및 시작
            } catch (IOException e) {
                e.printStackTrace(); // 예외 발생 시 출력
            }
        });
    }

    // OrderClient 생성자
    public OrderClient(String serverAddress, int port) throws IOException {
        connect(serverAddress, port); // 서버와 연결
    }

    // 클라이언트 프로그램 시작
    public void start() {
        JFrame frame = new JFrame("Order Client - TABLE NO: " + tableNumber); // 프레임 생성 및 제목 설정
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 창 닫기 동작 설정
        frame.setSize(600, 800); // 프레임 크기 설정
        frame.setLayout(new GridLayout(10, 2)); // 레이아웃 설정

        itemChecks = new JCheckBox[3]; // 체크박스 배열 초기화
        quantityBoxes = new JComboBox[3]; // 콤보박스 배열 초기화
        final String[] itemImages = { "./3.png", "./2.png", "./1.png" }; // 이미지 파일 경로
        final Dimension[] imageSizes = { new Dimension(74, 62), new Dimension(74, 62), new Dimension(62, 74) }; // 이미지 크기

        int index = 0;
        for (String item : itemPrices.keySet()) {
            JPanel itemPanel = new JPanel(new BorderLayout()); // 아이템 패널 생성
            itemChecks[index] = new JCheckBox(item); // 체크박스 생성 및 아이템 설정
            quantityBoxes[index] = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}); // 콤보박스 생성 및 수량 설정
            itemChecks[index].addActionListener(e -> updateCurrentOrder()); // 체크박스 클릭 시 현재 주문 업데이트
            quantityBoxes[index].addActionListener(e -> updateCurrentOrder()); // 콤보박스 선택 시 현재 주문 업데이트

            try {
                ImageIcon icon = new ImageIcon(new ImageIcon(itemImages[index]).getImage().getScaledInstance(imageSizes[index].width, imageSizes[index].height, Image.SCALE_SMOOTH)); // 이미지 아이콘 생성 및 크기 설정
                JLabel imageLabel = new JLabel(icon); // 이미지 레이블 생성
                itemPanel.add(imageLabel, BorderLayout.WEST); // 아이템 패널의 서쪽에 이미지 추가
            } catch (Exception e) {
                e.printStackTrace(); // 예외
            }

            itemPanel.add(itemChecks[index], BorderLayout.CENTER); // 아이템 패널의 중앙에 체크박스 추가
            itemPanel.add(quantityBoxes[index], BorderLayout.EAST); // 아이템 패널의 동쪽에 콤보박스 추가
            frame.add(itemPanel); // 프레임에 아이템 패널 추가
            index++;
        }

        JButton orderButton = new JButton("Order"); // 주문 버튼 생성
        frame.add(orderButton); // 프레임에 주문 버튼 추가

        currentOrderLabel = new JLabel("Current Order: 0won"); // 현재 주문 금액 레이블 생성
        frame.add(currentOrderLabel); // 프레임에 현재 주문 금액 레이블 추가

        totalOrderLabel = new JLabel("Total Ordered: 0won"); // 총 주문 금액 레이블 생성
        frame.add(totalOrderLabel); // 프레임에 총 주문 금액 레이블 추가

        JButton disconnectButton = new JButton("Disconnect"); // 연결 끊기 버튼 생성
        frame.add(disconnectButton); // 프레임에 연결 끊기 버튼 추가

        JButton quitButton = new JButton("Quit"); // 종료 버튼 생성
        frame.add(quitButton); // 프레임에 종료 버튼 추가

        // 주문 버튼 클릭 시
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
                    JOptionPane.showMessageDialog(frame, alertMessage.toString()); // 주문 내역을 알림 창으로 표시
                    totalOrderLabel.setText("Total Ordered: " + totalAmount + "won");
                } catch (IOException ex) {
                    ex.printStackTrace(); // 예외 발생 시 출력
                }
            }
        });

        // 연결 끊기 버튼 클릭 시
        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dos.writeUTF("disconnect");
                    dos.flush();
                    JOptionPane.showMessageDialog(frame, "Disconnected. Total amount: " + totalAmount + "won");
                    socket.close();
                    frame.dispose(); // 현재 창 닫기
                    SwingUtilities.invokeLater(() -> {
                        try {
                            String serverAddress = JOptionPane.showInputDialog("Enter server address:");
                            int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port number:"));
                            new OrderClient(serverAddress, port).start(); // 새로운 클라이언트 인스턴스 생성 및 시작
                        } catch (IOException ex) {
                            ex.printStackTrace(); // 예외 발생 시 출력
                        }
                    });
                } catch (IOException ex) {
                    ex.printStackTrace(); // 예외 발생 시 출력
                }
            }
        });

        // 종료 버튼 클릭 시
        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dos.writeUTF("quit");
                    dos.flush();
                    socket.close(); // 소켓 닫기
                } catch (IOException ex) {
                    ex.printStackTrace(); // 예외 발생 시 출력
                }
                frame.dispose(); // 창 닫기
            }
        });

        frame.setVisible(true); // 프레임을 화면에 표시
    }

    // 서버와 연결
    private void connect(String serverAddress, int port) throws IOException {
        tableNumber = Integer.parseInt(JOptionPane.showInputDialog("Enter table number:")); // 테이블 번호 입력받기
        socket = new Socket(serverAddress, port); // 서버와 소켓 연결
        dis = new DataInputStream(socket.getInputStream()); // 입력 스트림 생성
        dos = new DataOutputStream(socket.getOutputStream()); // 출력 스트림 생성
        dos.writeInt(tableNumber); // 테이블 번호 전송
        dos.flush();
    }

    // 현재 주문 업데이트
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
