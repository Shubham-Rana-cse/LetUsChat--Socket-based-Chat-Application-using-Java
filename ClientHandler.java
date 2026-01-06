import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader br;
    private PrintWriter out;
    private String username;

    private long pendingFileSize;
    private String pendingFileName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("ENTER_USERNAME");
            username = br.readLine();

            synchronized (Server.clients) {
                if (Server.clients.containsKey(username)) {
                    out.println("ERROR|Username taken");
                    socket.close();
                    return;
                }
                Server.clients.put(username, this);
            }

            System.out.println(username + " connected");
            Server.broadcast("USER_ONLINE|" + username);
            send("ONLINE_USERS|" + String.join(",", Server.clients.keySet()));

            String input;
            while ((input = br.readLine()) != null) {
                handleMessage(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private void handleMessage(String input) {
        String[] parts = input.split("\\|", 4);

        switch (parts[0]) {

            case "MESSAGE":
                sendPrivate(parts[1], parts[2]);
                break;

            case "BROADCAST":
                Server.broadcast("MESSAGE|" + username + "|" + parts[1]);
                break;

            case "FILE_REQUEST":
                handleFileRequest(parts[1], parts[2], Long.parseLong(parts[3]));
                break;

            case "FILE_ACCEPT":
                handleFileAccept(parts[1], parts[2]);
                break;

            case "LOGOUT":
                disconnect();
                break;
        }
    }

    private void handleFileRequest(String toUser, String fileName, long size) {
        if (size > Server.MAX_FILE_SIZE) {
            send("ERROR|File too large");
            return;
        }

        fileName = fileName.replaceAll("[/\\\\]", "_");

        ClientHandler target = Server.clients.get(toUser);
        if (target != null) {
            pendingFileSize = size;
            pendingFileName = fileName;
            target.send("FILE_OFFER|" + username + "|" + fileName + "|" + size);
        } else {
            send("ERROR|User not online");
        }
    }

    private void handleFileAccept(String senderName, String fileName) {
        ClientHandler sender = Server.clients.get(senderName);
        if (sender != null) {
            long size = sender.pendingFileSize;
            sender.send("FILE_START|" + senderName + "|" + username + "|" + fileName + "|" + size);
            send("FILE_START|" + senderName + "|" + username + "|" + fileName + "|" + size);
        }
    }

    private void sendPrivate(String toUser, String msg) {
        ClientHandler target = Server.clients.get(toUser);
        if (target != null) {
            target.send("PRIVATE|" + username + "|" + msg);
        }
    }

    public void send(String msg) {
        out.println(msg);
    }

    private void disconnect() {
        if (username != null) {
            Server.clients.remove(username);
            Server.broadcast("USER_OFFLINE|" + username);
            System.out.println(username + " disconnected");
        }
        try { socket.close(); } catch (Exception ignored) {}
    }
}
