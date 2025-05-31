import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.*;

public class Client extends JFrame {

    public class MyDB {
        String url = "jdbc:mysql://localhost/damabom";
        String user = "root";
        String password = "****";

        Connection conn = null;
        PreparedStatement psm = null;
        ResultSet rs = null;

        public void connectDB() {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(url, user, password);
                System.out.println("DB 연결 성공");
            } catch (Exception e) {
                System.out.println("DB 연결 실패");
                e.printStackTrace();
            }
        }

        public void closeDB() {
            try {
                if (rs != null) rs.close();
                if (psm != null) psm.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 로그인 체크
        public boolean checkLogin(String id, String pw) {
            connectDB();
            try {
                String sql = "SELECT * FROM dama WHERE id = ? AND pw = ?";
                psm = conn.prepareStatement(sql);
                psm.setString(1, id);
                psm.setString(2, pw);
                rs = psm.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                //closeDB();
            }
            return false;
        }

        // 허기, 기분, 건강 상태 가져오기
        public int[] getStatusById(String userId) {
            int[] status = new int[3];
            connectDB();
            try {
                String sql = "SELECT hungry, mood, health FROM dama WHERE id = ?";
                psm = conn.prepareStatement(sql);
                psm.setString(1, userId);
                rs = psm.executeQuery();

                if (rs.next()) {
                    status[0] = rs.getInt("hungry");
                    status[1] = rs.getInt("mood");
                    status[2] = rs.getInt("health");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                //closeDB();
            }
            return status;
        }

        // 캐릭터 이름 가져오기
        public String getDamaNameById(String userId) {
            String damaName = null;
            connectDB();
            try {
                String sql = "SELECT damaName FROM dama WHERE id = ?";
                psm = conn.prepareStatement(sql);
                psm.setString(1, userId);
                rs = psm.executeQuery();

                if (rs.next()) {
                    damaName = rs.getString("damaName");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                //closeDB();
            }
            return damaName;
        }

        // 상태 업데이트 메서드
        public void updateStatusById(String userId, int hungry, int mood, int health) {
            connectDB();
            try {
                String sql = "UPDATE dama SET hungry = ?, mood = ?, health = ? WHERE id = ?";
                psm = conn.prepareStatement(sql);
                psm.setInt(1, hungry);
                psm.setInt(2, mood);
                psm.setInt(3, health);
                psm.setString(4, userId);
                psm.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                closeDB();
            }
        }

    }


    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    public Client() throws UnknownHostException {
        setTitle("업데이트 되는 상태 값");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // 로그인 패널과 로그인 성공 패널 추가
        cardPanel.add(new LoginPanel(), "login");
        cardPanel.add(new LoginSuccess(), "loginSuccess");

        add(cardPanel);
        cardLayout.show(cardPanel, "login");

        setSize(800, 600);
        setVisible(true);
    }


    MyDB myDB = new MyDB();
    String loginUserId = "user";

    // 로그인 패널
    private class LoginPanel extends JPanel {
        public LoginPanel() {
            setLayout(new BorderLayout());

            JPanel loginWrapPanel = new JPanel(null);
            loginWrapPanel.setPreferredSize(new Dimension(400, 200));

            JTextField idField = new JTextField();
            idField.setBounds(100, 30, 200, 30);
            JPasswordField pwField = new JPasswordField();
            pwField.setBounds(100, 70, 200, 30);

            JButton loginButton = new JButton("로그인");
            loginButton.setBounds(100, 120, 200, 30);

            loginWrapPanel.add(idField);
            loginWrapPanel.add(pwField);
            loginWrapPanel.add(loginButton);

            JPanel centerWrapper = new JPanel(new GridBagLayout());
            centerWrapper.add(loginWrapPanel);

            add(centerWrapper, BorderLayout.CENTER);

            // 로그인 버튼 클릭 시
            loginButton.addActionListener(e -> {
                String id = idField.getText();
                String pw = new String(pwField.getPassword());

                if (myDB.checkLogin(id, pw)) {  // 로그인 성공
                    cardLayout.show(cardPanel, "loginSuccess");
                } else {
                    JOptionPane.showMessageDialog(this, "아이디와 비밀번호를 다시 확인해주세요.");
                }
            });
        }
    }

    DataInputStream dis = null;
    DataOutputStream dos = null;

    WideArea chat = new WideArea();
    JTextField sender = new JTextField(20);

    // 로그인 성공 화면 패널
    private class LoginSuccess extends JPanel {
        int[] status = myDB.getStatusById(loginUserId);
        int hungryNum = status[0];  // 허기
        int moodNum = status[1];  // 기분
        int healthNum = status[2];  // 건강
        String damaName = myDB.getDamaNameById(loginUserId);  // 캐릭터 이름

        public LoginSuccess() throws UnknownHostException {
            setLayout(new BorderLayout());
            setBackground(Color.white);

            JLabel hungry = new JLabel("허기 " + hungryNum);
            JLabel mood = new JLabel("기분 " + moodNum);
            JLabel health = new JLabel("건강 " + healthNum);

            JPanel statusPanel = new JPanel();
            statusPanel.setBackground(Color.pink);
            statusPanel.add(hungry);
            statusPanel.add(mood);
            statusPanel.add(health);
            add(statusPanel, BorderLayout.NORTH);

            chat.setBackground(Color.lightGray);
            chat.setEditable(false);
            chat.setFocusable(false);
            JScrollPane chatPane = new JScrollPane(chat);

            JButton feedBtn = new JButton("밥주기");
            JButton playBtn = new JButton("놀아주기");
            JButton hospitalBtn = new JButton("병원가기");

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
            buttonPanel.add(feedBtn);
            buttonPanel.add(playBtn);
            buttonPanel.add(hospitalBtn);

            JPanel chatAndButtonPanel = new JPanel();
            chatAndButtonPanel.setLayout(new BorderLayout());
            chatAndButtonPanel.add(chatPane, BorderLayout.CENTER);
            chatAndButtonPanel.add(buttonPanel, BorderLayout.SOUTH);

            add(chatAndButtonPanel, BorderLayout.CENTER);

            sender.setPreferredSize(new Dimension(0, 30));
            add(sender, BorderLayout.SOUTH);

            // 통신연결 메세지를 입출력 메서드
            connect_chatting();

            Thread th = new Thread(chat);  // 쓰레드 생성
            th.start();

            sender.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == sender) {
                        String msg = sender.getText();
                        try {
                            dos.writeUTF("(" + damaName + ")" + msg + "\n");
                            dos.flush();
                            sender.setText("");
                        } catch (IOException e1) {
                            e1.getMessage();
                        }
                    }
                }
            });

            // 밥주기 버튼 클릭 시
            feedBtn.addActionListener(e -> {
                hungryNum += 5;
                hungry.setText("허기 " + hungryNum);
                myDB.updateStatusById(loginUserId, hungryNum, moodNum, healthNum);
                try {
                    dos.writeUTF("밥주기");
                    dos.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            // 놀아주기 버튼 클릭 시
            playBtn.addActionListener(e -> {
                moodNum += 5;
                mood.setText("기분 " + moodNum);
                myDB.updateStatusById(loginUserId, hungryNum, moodNum, healthNum);
                try {
                    dos.writeUTF("놀아주기");
                    dos.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            // 병원가기 버튼 클릭 시
            hospitalBtn.addActionListener(e -> {
                healthNum += 5;
                health.setText("건강 " + healthNum);
                myDB.updateStatusById(loginUserId, hungryNum, moodNum, healthNum);
                try {
                    dos.writeUTF("병원가기");
                    dos.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            setSize(200, 270);
            setVisible(true);
        }
    }

    private class WideArea extends JTextArea implements Runnable {
        @Override
        public void run() {  // 쓰레드가 동작
            String msg = null;
            while (true) {
                try {
                    msg = dis.readUTF();
                } catch (IOException e){
                    e.printStackTrace();
                }
                append(msg);
                int pos = this.getText().length();
                setCaretPosition(pos);
            }
        }
    }

    public void connect_chatting() throws UnknownHostException {
        Socket socket = null;

        try {
            socket = new Socket(InetAddress.getByName("localhost"), 50000);
            InputStream is = socket.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            dis = new DataInputStream(bis);
            OutputStream os = socket.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(os);
            dos = new DataOutputStream(bos);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        new Client();
    }
}
