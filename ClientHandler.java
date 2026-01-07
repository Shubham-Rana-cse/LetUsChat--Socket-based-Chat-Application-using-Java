import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader br;
    private PrintWriter out;
    private String username;

    // stores last requested file size (needed for FILE_START)
    private long pendingFileSize;
    private String pendingFileName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {     //Java internally calls run() in a new thread
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            //USERNAME HANDSHAKE
            out.println("ENTER_USERNAME");
            username = br.readLine();

            synchronized (Server.clients) {     //acquire lock on Server.clients
                if (Server.clients.containsKey(username)) {
                    out.println("ERROR|Username taken");
                    socket.close();
                    return;
                }
                Server.clients.put(username, this);
            }

            System.out.println(username + " connected");
            Server.broadcast("USER_ONLINE|" + username);
            //send online users list
            send("ONLINE_USERS|" + String.join(",", Server.clients.keySet()));

            //MESSAGE LOOP
            String input;
            while ((input = br.readLine()) != null) {
                handleMessage(input);
            }
        } catch (Exception e) {
            //e.printStackTrace();
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

            case "GET_USERS":
                getUsersList();
                break;

            case "FILE_REQUEST":
                handleFileRequest(parts[1], parts[2], Long.parseLong(parts[3]));
                break;

            case "FILE_ACCEPT":
                handleFileAccept(parts[1], parts[2]);
                break;

            case "ERROR":
                Server.clients.get(username).send("Error while sending file or message. Try again !!");
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

        ClientHandler target = Server.clients.get(toUser);  //find the sender
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
            sender.send("FILE_START|" + senderName + "|" + username + "|" + fileName + "|" + size); //send FILE_START to sender
            send("FILE_START|" + senderName + "|" + username + "|" + fileName + "|" + size);    //send FILE_START to receiver
        }
    }
    /*
    Sender      Server       Receiver
    |             |            |
    | FILE_REQUEST|            |
    |------------>|            |
    |             | FILE_OFFER |
    |             |----------->|
    |             |            |
    |             | FILE_ACCEPT|
    |             |<-----------|
    |             |            |
    | FILE_START  | FILE_START |
    |<------------|----------->|
    |             |            |
    | connect FILE_PORT        |
    |------------>             |
    |             | connect FILE_PORT
    |             |<-----------|
    |             |            |
    |===== FILE BYTES RELAY =====|
    */


    private void sendPrivate(String toUser, String msg) {
        ClientHandler target = Server.clients.get(toUser);
        if (target != null) {
            target.send("PRIVATE|" + username + "|" + msg);
        }
    }

    public void send(String msg) {
        out.println(msg);
    }

    private void getUsersList(){
        ClientHandler target = Server.clients.get(username);
        if(target != null)
            target.send("ONLINE_USERS|" + String.join(",", Server.clients.keySet()));
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
