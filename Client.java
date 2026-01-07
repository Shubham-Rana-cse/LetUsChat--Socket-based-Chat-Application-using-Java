import java.net.*;
import java.io.*;

public class Client {

    private Socket socket;
    private BufferedReader br;
    private PrintWriter out;
    private BufferedReader console;
    private File pendingFile;
    private String username;

    private volatile String pendingSender;
    private volatile String pendingFileName;
    private volatile long pendingFileSize;


    public static void main(String[] args) {
        new Client().start();
    }

    public void start() {
        try {
            socket = new Socket("127.0.0.1", Server.CHAT_PORT);

            //I/O stream created
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            console = new BufferedReader(new InputStreamReader(System.in));

            //Listener thread
            new Thread(this::listen).start();

            //Username handshake
            if (br.readLine().equals("ENTER_USERNAME")) {
                System.out.print("Enter username: ");
                username = console.readLine();
                out.println(username);
            }

            //Sending messages
            while (true) {
                String input = console.readLine();

                if (input.equalsIgnoreCase("/exit")) {
                    out.println("LOGOUT");
                    break;
                }

                if(input.equalsIgnoreCase("/users")){   //get online users' list
                    out.println("GET_USERS");
                    continue;
                }

                if (input.startsWith("/sendfile")) {
                    String[] p = input.split(" ", 3);
                    if(p.length != 3)
                        out.println("ERROR");
                    else{
                        pendingFile = new File(p[2]);
                        out.println("FILE_REQUEST|" + p[1] + "|" + pendingFile.getName() + "|" + pendingFile.length());
                        //sendFile(f);      //removed due to Protocol-Enforced Pairing
                    }
                    continue;
                }

                //private: @user hello
                if (input.startsWith("@")) {
                    int sp = input.indexOf(" ");
                    if(sp != -1)
                        out.println("MESSAGE|" + input.substring(1, sp) + "|" + input.substring(sp + 1));
                    else
                        out.println("ERROR");
                }
                else {
                    out.println("BROADCAST|" + input);
                }

                if (input.equalsIgnoreCase("/accept")) {
                    if (pendingSender != null) {
                        out.println("FILE_ACCEPT|" + pendingSender + "|" + pendingFileName);
                        System.out.println("File accepted.");
                        pendingSender = null;
                    }
                    else {
                        System.out.println("No file request pending.");
                    }
                    continue;
                }

                if (input.equalsIgnoreCase("/reject")) {
                    if (pendingSender != null) {
                        System.out.println("File rejected.");
                        pendingSender = null;
                    } 
                    else {
                        System.out.println("No file request pending.");
                    }
                    continue;
                }

            }

        }
        catch (Exception e) {
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

                    pendingSender = p[1];
                    pendingFileName = p[2];
                    pendingFileSize = Long.parseLong(p[3]);

                    System.out.println(
                        "Incoming file from " + pendingSender +
                        " : " + pendingFileName +
                        " (" + pendingFileSize + " bytes)"
                    );
                    System.out.println("Type /accept or /reject");
                }

                else if (msg.startsWith("FILE_START")) {
                    String[] p = msg.split("\\|");
                    String sender = p[1];
                    String fileName = p[3];
                    long size = Long.parseLong(p[4]);

                    // If I am sender
                    if (username.equals(sender)) {
                        sendFile(pendingFile);
                        pendingFile = null;
                    }
                    // If I am receiver
                    else {
                        receiveFile(fileName, size);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFile(File file) {      //file transfer must never run on the main thread
        new Thread(() -> {
            try (
                Socket fs = new Socket("127.0.0.1", Server.FILE_PORT);
                FileInputStream fis = new FileInputStream(file);    //reads the file byte by byte, does not load whole file into memory
                OutputStream os = fs.getOutputStream(); //This is a pipe ot the server
            ) {
                byte[] buf = new byte[4096];
                int read;  //bytes actually read
                while ((read = fis.read(buf)) != -1) {   //reads bytes from file into buffer
                    os.write(buf, 0, read);     //sent the bytes just read from '0 to read'
                }
            }
            catch (Exception e) {
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
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
