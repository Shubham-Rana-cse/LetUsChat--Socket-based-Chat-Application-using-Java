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
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startFileRelayServer() {
        try (ServerSocket fileServer = new ServerSocket(FILE_PORT)) {
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

    private static void relay(Socket a, Socket b) {
        new Thread(() -> pipe(a, b)).start();
        new Thread(() -> pipe(b, a)).start();
    }

    private static void pipe(Socket from, Socket to) {
        try (
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream()
        ) {
            byte[] buf = new byte[4096];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
        } catch (Exception ignored) {}
    }


    public static void broadcast(String msg) {
        synchronized (clients) {
            for (ClientHandler ch : clients.values()) {
                ch.send(msg);
            }
        }
    }
}
