import java.io.*;
import java.net.*;
import java.util.*;

public class OrderServer {
    private ServerSocket serverSocket;
    private List<OrderHandler> clients = new ArrayList<>();
    private boolean running = true;

    public static void main(String[] args) {
        try {
            int port = getPortFromUser();
            new OrderServer(port).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getPortFromUser() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter port number: ");
        return scanner.nextInt();
    }

    public OrderServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port: " + port);
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
        stopServer();
    }

    public void stopServer() {
        running = false;
        try {
            for (OrderHandler handler : clients) {
                handler.stop();
            }
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class OrderHandler implements Runnable {
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private boolean running = true;
        private int tableNumber;
        private int totalAmount = 0;

        public OrderHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                tableNumber = dis.readInt(); // 클라이언트로부터 테이블 번호를 받음
                System.out.println("Client connected: Table " + tableNumber);

                while (running) {
                    String message = dis.readUTF();
                    System.out.println("Received order from Table " + tableNumber + ": " + message); // 터미널에 주문 내역 출력
                    dos.writeUTF("Order received: " + message);

                    String[] parts = message.split("\n");
                    int orderAmount = 0;
                    for (String part : parts) {
                        if (part.startsWith("TableNo. ")) {
                            String[] orderParts = part.split(", ");
                            orderAmount += Integer.parseInt(orderParts[3].trim().substring(1)); // 금액 추출
                        }
                    }
                    totalAmount += orderAmount;
                    System.out.println("Total for Table " + tableNumber + ": $" + totalAmount);
                }
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
