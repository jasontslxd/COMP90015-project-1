package chatserver;

import chatclient.Client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Server {
    public static final int DEFAULT_PORT = 4444;

    @Option(name="-p", usage="Specify port number")
    private int port;
    private boolean alive;
    private ArrayList<Integer> inUse = new ArrayList<>();
    private ArrayList<Room> rooms = new ArrayList<Room>(); // Placeholder for Room class
    private ArrayList<String> roomNames = new ArrayList<String>();
    private HashSet<ClientThread> allClients = new HashSet<>(); // Placeholder for ClientThread class

    public static void main(String[] args){
        Server server = new Server();
        CmdLineParser parser = new CmdLineParser(server);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            e.printStackTrace();
        }
        server.handle();
    }

    /**
     * Constructor for server
     */
    public Server() {
        this.port = DEFAULT_PORT;
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
                roomNames.add(mainHall.getRoomId());
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

    public String roomList(){
        String roomList = "";
        String type = "type:roomlist";
        String roomDict = "";
        for (Room r: rooms){
            String roomId = "roomid:" + r.getRoomId();
            String count = "count:" + r.getClients().size();
            roomDict += roomId + "," + count;
        }
        String rooms = "rooms:"+roomDict;
        roomList = type + "," + roomDict;
        return roomList;
    }

    public String createRoom(String roomName, String owner){
        Pattern p = Pattern.compile("\\w{3,32}");
        Matcher m = p.matcher(roomName);
        boolean validName = m.matches();
        // Check if already in use;
        Boolean roomExists = roomNames.contains(roomName);
        if (validName && !roomExists){
            Room newRoom = new Room(roomName, owner);
            rooms.add(newRoom);
            roomNames.add(roomName);
            return "Room "+roomName+ " created.";
        } else {
            return "Room "+roomName+ " is invalid or already in use.";
        }
    }

    public void updateOwner(Identity client){
        int len=rooms.size();
        for(int i=0; i<len; i++) {
            if (rooms.get(i).getOwner().equals(client.getFormer())) {
                rooms.get(i).setOwner(client.getIdentity());
            }
        }
    }

    public ArrayList<Room> getRooms() {
        return rooms;
    }

    public Room getRoom(String roomName){ //Move to server class?
        int len=rooms.size();
        for(int i=0; i<len; i++) {
            if (rooms.get(i).getRoomId().equals(roomName)) {
                return rooms.get(i);
            }
        }
        return null;
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
