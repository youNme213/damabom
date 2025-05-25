import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server extends JFrame {

    ServerSocket serverSocket = null;
    DataInputStream dis = null;
    DataOutputStream dos = null;

    // JTextArea를 receiver
    Receiver receiver = new Receiver();
    JTextField sender = new JTextField();

    int hungryNum = 0;
    int moodNum = 0;
    int healthNum = 40;

    public  Server() {
        setTitle("담아봄");
        setLayout(new BorderLayout());

        JPanel statusPanel = new JPanel(new GridBagLayout());

        JLabel hungry = new JLabel("허기 " + hungryNum);
        JLabel mood = new JLabel("기분 " + moodNum);
        JLabel health = new JLabel("건강 " + healthNum);
        statusPanel.setBackground(Color.pink);

        JPanel innerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        innerPanel.setBackground(Color.pink);
        innerPanel.add(hungry);
        innerPanel.add(mood);
        innerPanel.add(health);

        statusPanel.add(innerPanel);
        add(statusPanel, BorderLayout.CENTER);

        setSize(200, 270);
        setVisible(true);

        // 통신연결 메세지를 입출력 메서드
        connect_chatting();

        Thread th = new Thread(receiver);  // 쓰레드 생성
        th.start();  // 쓰레드 실행

        // 이벤트 처리
        sender.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == sender) {
                    String msg = sender.getText();
                    try {
                        dos.writeUTF("(서버)" + msg + "\n");
                        dos.flush();
                        sender.setText("");
                    } catch (IOException e1) {
                        e1.getMessage();
                    }
                }
            }
        });
    }

    public void connect_chatting() {
        try {
            serverSocket = new ServerSocket(50000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Socket socket = serverSocket.accept();
            InputStream is = socket.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            dis = new DataInputStream(bis);
            OutputStream os = socket.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(os);
            dos = new DataOutputStream(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Receiver extends JTextArea implements Runnable {

        @Override
        public void run() {
            String hungryMent = (hungryNum < 50) ? "너무 배고파" : "안 배고파";
            String moodMent = (moodNum < 30) ? "너무 심심해" : "괜찮아";
            String healthMent = (healthNum < 30) ? "나 아파" : "멀쩡해";

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("배고파?", hungryMent);
            responseMap.put("심심해?", moodMent);
            responseMap.put("아파?", healthMent);
            responseMap.put("밥주기" , "덕분에 배가 불러!");
            responseMap.put("놀아주기" , "같이 놀아줘서 행복해!!");
            responseMap.put("병원가기" , "병원 같이 가줘서 고마워ㅜㅜ");

            String msg;
            while (true) {
                try {
                    msg = dis.readUTF();
                    append(msg);  // 받은 메시지를 JTextArea에 표시
                    int pos = this.getText().length();
                    setCaretPosition(pos);

                    String trimmedMsg = msg.trim();
                    boolean matched = false;

                    for (Map.Entry<String, String> entry : responseMap.entrySet()) {
                        if (trimmedMsg.contains(entry.getKey())) {
                            dos.writeUTF("(캐릭터 이름) " + entry.getValue() + "\n");
                            dos.flush();
                            matched = true;
                            break;
                        }
                    }

                    if (!matched) {
                        dos.writeUTF("(캐릭터 이름) 죄송합니다. 이해하지 못했어요.\n");
                        dos.flush();
                    }

                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }



    public static void main(String[] args) {
        new Server();
    }
}
