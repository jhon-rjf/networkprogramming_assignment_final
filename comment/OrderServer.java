/*-----------------------------------------------------------------------------------
compile - javac -cp ".:sqlite-jdbc-3.46.0.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" OrderServer.java
run - java -cp ".:sqlite-jdbc-3.46.0.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar" OrderServer
-----------------------------------------------------------------------------------*/
import javax.swing.*; // 스윙 라이브러리  
import java.awt.*; // AWT 라이브러리  
import java.awt.event.ActionEvent; // 액션 이벤트 클래스  
import java.awt.event.ActionListener; // 액션 리스너 클래스  
import java.io.*; // 입출력 관련 클래스  
import java.net.ServerSocket; // 서버 소켓 클래스  
import java.net.Socket; // 소켓 클래스  
import java.sql.*; // SQL 관련 클래스  
import java.text.SimpleDateFormat; // 날짜 포맷 관련 클래스  
import java.util.Date; // 날짜 클래스  
import java.util.HashMap; // 해시맵 클래스  
import java.util.Map; // 맵 클래스  

public class OrderServer extends JFrame {
    private JTextArea orderArea; // 주문 내역을 표시할 텍스트 영역
    private ServerSocket serverSocket; // 서버 소켓
    private Map<Integer, Integer> tableTotalAmounts; // 테이블별 총 금액을 저장할 맵
    private static final int LINE_LENGTH = 29; // 라인의 길이 상수
    private Connection dbConnection; // 데이터베이스 연결 객체

    // 메인 메소드, 프로그램 시작 지점
    public static void main(String[] args) {
        // GUI 관련 작업을 스윙 이벤트를 스레드에서 실행
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    new OrderServer(); // OrderServer 객체 생성
                } catch (IOException | SQLException e) {
                    e.printStackTrace(); // 예외
                }
            }
        });
    }

    // 생성자, 서버 초기화 작업 수행
    public OrderServer() throws IOException, SQLException {
        super("Order Server"); // JFrame 생성자 호출, 창 제목 설정

        // SQLite JDBC 드라이버 로드
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // 예외
            // 드라이버를 찾을 수 없을 때 오류 메시지 표시
            JOptionPane.showMessageDialog(this, "SQLite JDBC driver not found. Please add the driver to your classpath.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1); // 프로그램 종료
        }

        // 데이터베이스 연결 초기화
        dbConnection = DriverManager.getConnection("jdbc:sqlite:orders.db");
        createTableIfNotExists(); // 테이블이 없으면 생성

        tableTotalAmounts = new HashMap<>(); // 테이블 총 금액을 저장할 해시맵 생성
        orderArea = new JTextArea(); // 텍스트 영역 생성
        orderArea.setEditable(false); // 텍스트 영역 수정 불가 설정
        orderArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // 폰트 설정

        JScrollPane scrollPane = new JScrollPane(orderArea); // 스크롤 패널 생성, 텍스트 영역 추가

        JButton stopButton = new JButton("Stop Server"); // 서버 정지 버튼 생성
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopServer(); // 서버 정지 메소드 호출
            }
        });

        JButton settlementButton = new JButton("Settlement"); // 정산 버튼 생성
        settlementButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                settleAccounts(); // 정산 메소드 호출
            }
        });

        JButton showDataButton = new JButton("Show Data"); // 데이터 표시 버튼 생성
        showDataButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSettlementData(); // 데이터 표시 메소드 호출
            }
        });

        setLayout(new BorderLayout()); // 프레임 레이아웃 설정
        add(scrollPane, BorderLayout.CENTER); // 중앙에 스크롤 패널 추가

        JPanel bottomPanel = new JPanel(new GridLayout(1, 3)); // 하단 패널 생성, 그리드 레이아웃 사용
        bottomPanel.add(stopButton); // 정지 버튼 추가
        bottomPanel.add(settlementButton); // 정산 버튼 추가
        bottomPanel.add(showDataButton); // 데이터 표시 버튼 추가
        add(bottomPanel, BorderLayout.SOUTH); // 하단 패널을 프레임 남쪽에 추가

        setSize(600, 400); // 프레임 크기 설정
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 닫기 버튼 클릭 시 프로그램 종료
        setVisible(true); // 프레임 보이도록 설정

        int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port number:")); // 포트 번호 입력 받기
        serverSocket = new ServerSocket(port); // 서버 소켓 생성
        orderArea.append("Server started on port: " + port + "\n"); // 서버 시작 메시지 출력

        // 클라이언트 수락을 위한 스레드 시작
        new Thread(new Runnable() {
            public void run() {
                acceptClients(); // 클라이언트 수락 메소드 호출
            }
        }).start();
    }

    // 데이터베이스에 테이블이 없으면 생성하는 메소드
    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS settlements (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT, table_number INTEGER, total_amount INTEGER)";
        PreparedStatement stmt = dbConnection.prepareStatement(sql); // SQL 문 준비
        stmt.execute(); // SQL 문 실행
        stmt.close(); // 문 닫기
    }

    // 클라이언트를 수락하는 메소드
    private void acceptClients() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept(); // 클라이언트 소켓 수락
                new Thread(new OrderHandler(clientSocket)).start(); // 새로운 스레드에서 클라이언트 처리
            } catch (IOException e) {
                orderArea.append("Error accepting client: " + e.getMessage() + "\n"); // 예외 발생 시 메시지 출력
            }
        }
    }

    // 서버를 정지하는 메소드
    private void stopServer() {
        try {
            serverSocket.close(); // 서버 소켓 닫기
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close(); // 데이터베이스 연결 닫기
            }
            orderArea.append("Server stopped.\n"); // 서버 정지 메시지 출력
            System.exit(0); // 프로그램 종료
        } catch (IOException | SQLException e) {
            orderArea.append("Error stopping server: " + e.getMessage() + "\n"); // 예외 발생 시 메시지 출력
        }
    }

    // 텍스트를 특정 길이로 포맷팅하는 메소드 -> chatgpt의 도움을 받음. server측의 textarea에 일정하게 출력하도록 한다.
    private String formatLine(String text) {
        if (text.length() > LINE_LENGTH) {
            return text.substring(0, LINE_LENGTH - 1) + "|"; // 텍스트 길이가 길면 잘라서 반환
        }
        StringBuilder sb = new StringBuilder(text); // StringBuilder 객체 생성 -> 문자열의 조작을 위해 생성됨
        while (sb.length() < LINE_LENGTH) {
            sb.append(" "); // 텍스트 길이가 충분하지 않으면 공백 추가
        }
        sb.append("|"); // 마지막에 '|' 추가
        return sb.toString(); // 포맷된 텍스트 반환
    }

    // 정산을 수행하는 메소드
    private void settleAccounts() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 날짜 포맷 설정
        String timestamp = dateFormat.format(new Date()); // 현재 날짜와 시간 문자열로 포맷

        try {
            String sql = "INSERT INTO settlements (timestamp, table_number, total_amount) VALUES (?, ?, ?)"; // SQL 문 준비
            PreparedStatement stmt = dbConnection.prepareStatement(sql);
            for (Map.Entry<Integer, Integer> entry : tableTotalAmounts.entrySet()) {
                stmt.setString(1, timestamp); // 첫 번째 인자에 타임스탬프 설정
                stmt.setInt(2, entry.getKey()); // 두 번째 인자에 테이블 번호 설정
                stmt.setInt(3, entry.getValue()); // 세 번째 인자에 총 금액 설정
                stmt.addBatch(); // 배치에 추가
            }
            stmt.executeBatch(); // 배치 실행
            stmt.close(); // 문 닫기
            orderArea.append("Settlement completed at " + timestamp + "\n"); // 정산 완료 메시지 출력
        } catch (SQLException e) {
            orderArea.append("Error during settlement: " + e.getMessage() + "\n"); // 예외 발생 시 메시지 출력
        }
    }

    // 정산 데이터를 표시하는 메소드
    private void showSettlementData() {
        try {
            Statement stmt = dbConnection.createStatement(); // 문 생성
            ResultSet rs = stmt.executeQuery("SELECT * FROM settlements"); // SQL 문 실행, 결과 집합 반환

            StringBuilder data = new StringBuilder();
            data.append("No. | Time                | TableNum | Amount\n");
            data.append("----|---------------------|----------|-------\n");

            while (rs.next()) {
                int id = rs.getInt("id"); // 결과 집합에서 ID 가져오기
                String timestamp = rs.getString("timestamp"); // 결과 집합에서 타임스탬프 가져오기
                int tableNumber = rs.getInt("table_number"); // 결과 집합에서 테이블 번호 가져오기
                int totalAmount = rs.getInt("total_amount"); // 결과 집합에서 총 금액 가져오기

                data.append(String.format("%-4d| %-20s | %-8d | %-6d\n", id, timestamp, tableNumber, totalAmount)); // 형식에 맞춰 데이터 추가
            }

            orderArea.append(data.toString()); // 텍스트 영역에 데이터 추가
            rs.close(); // 결과 집합 닫기
            stmt.close(); // 문 닫기
        } catch (SQLException e) {
            orderArea.append("Error retrieving data: " + e.getMessage() + "\n"); // 예외 발생 시 메시지 출력
        }
    }

    // 클라이언트 주문을 처리하는 내부 클래스
    private class OrderHandler implements Runnable { // runnable인터페이스 구현 -> 클라이언트 소켓통신, 주문처리, 연결 관리.
        private Socket clientSocket; // 클라이언트 소켓

        public OrderHandler(Socket socket) {
            this.clientSocket = socket; // 소켓 초기화
        }

        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); // 데이터 입력 스트림 생성
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream()); // 데이터 출력 스트림 생성

                int tableNumber = dis.readInt(); // 테이블 번호 읽기
                orderArea.append("New client connected: Table " + tableNumber + "\n"); // 새로운 클라이언트 연결 메시지 출력

                while (true) {
                    String message = dis.readUTF(); // 메시지 읽기
                    if (message.equals("disconnect") || message.equals("quit")) {
                        orderArea.append("Client disconnected: Table " + tableNumber + "\n"); // 클라이언트 연결 해제 메시지 출력
                        clientSocket.close(); // 클라이언트 소켓 닫기
                        break;
                    } else {
                        int currentTotal = tableTotalAmounts.getOrDefault(tableNumber, 0); // 현재 총 금액 가져오기
                        int newOrderTotal = Integer.parseInt(message.split("Total: ")[1].replace("won", "").trim()); // 새로운 주문 총 금액 계산
                        int newTotal = currentTotal + newOrderTotal; // 새로운 총 금액 계산
                        tableTotalAmounts.put(tableNumber, newTotal); // 테이블 총 금액 업데이트

                        String[] orderDetails = message.split("\n"); // 주문 세부 정보 분할
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

                        orderArea.append(displayMessage.toString()); // 텍스트 영역에 메시지 추가
                    }
                }
            } catch (IOException e) {
                orderArea.append("Client connection error: " + e.getMessage() + "\n"); // 예외 발생 시 메시지 출력
            }
        }
    }
}
