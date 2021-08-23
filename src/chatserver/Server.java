package chatserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Server {
    private static int port = 4444;
    private boolean alive;
    private ArrayList<Integer> inUse = new ArrayList<>();
    private ArrayList<Room> rooms = new ArrayList<Room>(); // Placeholder for Room class
    private HashSet<ClientThread> allClients = new HashSet<>(); // Placeholder for ClientThread class

    public static void main(String[] args){
        int port = Integer.parseInt(args[0]); // Note: Placeholder for CLI option parsing (e.g. args4j)
        Server server = new Server(port);
        server.handle();
    }

    /**
     * Constructor for server
     * @param port
     */
    public Server(int port) {
        this.port = port;
        this.rooms = null; // Note: Should have MainHall by default
        this.allClients = null;
    }

    /**
     * Begin listening for incoming connections and start each client as they come in
     */
    public void handle() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
            System.out.printf("Listening on port %d\n", port);
            alive = true;
            while (true) {
                Socket socket = serverSocket.accept();
                ClientThread client = new ClientThread(socket, this);
                Room mainHall = new Room("MainHall", "");
                rooms.add(mainHall);
                rooms.get(0).join(client);
                client.start();
            }
        } catch (IOException e) {
            alive = false;
            e.printStackTrace();
        }
    }

    /**
     * Broadcast message out to clients
     * Note: To incorporate JSON marshalling
     * @param json
     * @param ignore
     */
    public void broadcastAll(String json, ClientThread ignore) {
        synchronized (allClients) { // Note: Check if synchronisation is necessary
            for (ClientThread c: allClients) {
                if (ignore == null || !ignore.equals(c)) {
                    c.sendMessage(json);
                }
            }
        }
    }

    public void broadcastRoom(String json, Room room, ClientThread ignore) {
        synchronized (room.getClients()) { // Note: Check if synchronisation is necessary
            for (ClientThread c: room.getClients()) {
                if (ignore == null || !ignore.equals(c)) {
                    c.sendMessage(json);
                }
            }
        }
    }

    /**
     * Server reply to single client
     * Note: To incorporate JSON marshalling
     * @param message
     * @param c
     */
    public void reply(String message, ClientThread c) {
        synchronized (allClients) { // Note: Check if synchronisation is necessary
                    c.sendMessage(message);
        }
    }

    public void leave(ClientThread client) {
        synchronized (allClients) {
            allClients.remove(client);
            inUse.remove(client.getIdentity().getIdNum());
        }
        broadcastAll(String.format("%d has left the chat\n", client.getSocket().getPort()), client);
    }

    public void roomList(){

    }

    public void roomChange(){

    }

    public ArrayList<Room> getRooms() {
        return rooms;
    }

    public int getSmallestInt(){
        int smallest = 0;
        if (inUse.contains(null)){
            for (int i =0; i<inUse.size(); i++){
                if (inUse.get(i)==null){
                    smallest = i+1;
                    inUse.set(i,smallest);
                    break;
                }
            }
        } else {
            inUse.add(inUse.size()+1);
            smallest = inUse.size();
        }
        return smallest;
    }

    public ArrayList<Integer> getInUse() {
        return inUse;
    }

    public void setInUse(ArrayList<Integer> inUse) {
        this.inUse = inUse;
    }
}
