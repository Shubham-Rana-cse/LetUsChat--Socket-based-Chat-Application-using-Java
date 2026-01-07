import java.net.*;
import java.io.*;
import java.util.*;

public class Server {

    public static final int CHAT_PORT = 7777;
    public static final int FILE_PORT = 8888;
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    public static Map<String, ClientHandler> clients =
            Collections.synchronizedMap(new HashMap<>());

    private static final Queue<Socket> waiting = new LinkedList<>();

    public static void main(String[] args) {
        System.out.println("Chat server started...");

        new Thread(Server::startChatServer).start();
        new Thread(Server::startFileRelayServer).start();
    }

    private static void startChatServer() {
        try (ServerSocket server = new ServerSocket(CHAT_PORT)) {
            while (true) {
                Socket socket = server.accept();
                new Thread(new ClientHandler(socket)).start();  //handles a new client now
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startFileRelayServer() {
        try (ServerSocket fileServer = new ServerSocket(FILE_PORT)) {   //try-with-resources;;; Java will automatically close the server socket once program ends or any exception occurs
            while (true) {
                Socket socket = fileServer.accept();
                synchronized (waiting) {
                    waiting.add(socket);
                    if (waiting.size() >= 2) {
                        Socket s1 = waiting.poll();
                        Socket s2 = waiting.poll();
                        relay(s1, s2);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*
        A problem of file begin received from other computer but of always size 0 striked.

        Server relay was one-directional means if If receiver connects first then no data flows

        Assumption it made

            “The first socket is always the sender
            The second socket is always the receiver”

        Why this failed across PCs

            Network timing is unpredictable
            Sometimes receiver connects first
            Server then listens to the wrong direction
            Result: 0-byte files
    */

    private static void relay(Socket a, Socket b) {
        new Thread(() -> pipe(a, b)).start();
        new Thread(() -> pipe(b, a)).start();
    }

    private static void pipe(Socket from, Socket to) {
        try (   //try-with-resources ensures resouces are closed automatically after they are used
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream()
        ) {
            byte[] buffer = new byte[4096];    //this is a temporary byte container(4KB), used to move data in chunks, not one byte at a time
            int read;   //stores how many bytes were actually read from the sender, read can be less be less than 4096 especially near the EOF
            while((read = in.read(buffer)) != -1){    //read() reads bytes from sender's socket and stores them in buffer, RETURNS "no. of bytes read" OR "-1 when sender closes stream (EOF)"
                out.write(buffer, 0, read);     //writes exactly the bytes just read from index 0 upto 'read' bytes (we must not write the whole buffer blindly)
            }
        } catch (Exception ignored) {}
    }


    public static void broadcast(String msg) {
        synchronized (clients) {    //acquire lock on clients
            for (ClientHandler ch : clients.values()) {
                ch.send(msg);
            }
        }
    }
}
