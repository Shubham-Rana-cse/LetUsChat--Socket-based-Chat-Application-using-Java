import java.net.*;
import java.io.*;

public class Client {

    private Socket socket;
    private BufferedReader br;
    private PrintWriter out;
    private BufferedReader console;
    private File pendingFile;
    private String username;

    public static void main(String[] args) {
        new Client().start();
    }

    public void start() {
        try {
            socket = new Socket("127.0.0.1", Server.CHAT_PORT);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            console = new BufferedReader(new InputStreamReader(System.in));

            new Thread(this::listen).start();

            if (br.readLine().equals("ENTER_USERNAME")) {
                System.out.print("Enter username: ");
                username = console.readLine();
                out.println(username);
            }

            while (true) {
                String input = console.readLine();

                if (input.equalsIgnoreCase("/exit")) {
                    out.println("LOGOUT");
                    break;
                }

                if (input.startsWith("/sendfile")) {
                    String[] p = input.split(" ", 3);
                    pendingFile = new File(p[2]);
                    out.println("FILE_REQUEST|" + p[1] + "|" +
                            pendingFile.getName() + "|" + pendingFile.length());
                    continue;
                }

                if (input.startsWith("@")) {
                    int sp = input.indexOf(" ");
                    out.println("MESSAGE|" + input.substring(1, sp) + "|" +
                            input.substring(sp + 1));
                } else {
                    out.println("BROADCAST|" + input);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listen() {
        try {
            String msg;
            while ((msg = br.readLine()) != null) {
                System.out.println(msg);

                if (msg.startsWith("FILE_OFFER")) {
                    String[] p = msg.split("\\|");
                    System.out.println("Auto-accepting file from " + p[1]);
                    out.println("FILE_ACCEPT|" + p[1] + "|" + p[2]);
                }

                else if (msg.startsWith("FILE_START")) {
                    String[] p = msg.split("\\|");
                    String sender = p[1];
                    String fileName = p[3];
                    long size = Long.parseLong(p[4]);

                    if (username.equals(sender)) {
                        sendFile(pendingFile);
                        pendingFile = null;
                    } else {
                        receiveFile(fileName, size);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFile(File file) {
        new Thread(() -> {
            try (
                Socket fs = new Socket("127.0.0.1", Server.FILE_PORT);
                FileInputStream fis = new FileInputStream(file);
                OutputStream os = fs.getOutputStream()
            ) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = fis.read(buf)) != -1) {
                    os.write(buf, 0, r);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void receiveFile(String name, long size) {
        new Thread(() -> {
            try (
                Socket fs = new Socket("127.0.0.1", Server.FILE_PORT);
                FileOutputStream fos = new FileOutputStream("downloads_" + name);
                InputStream is = fs.getInputStream()
            ) {
                byte[] buf = new byte[4096];
                int read;
                while ((read = is.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
