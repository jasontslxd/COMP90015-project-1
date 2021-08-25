package chatserver;

import chatclient.Client;
import util.JsonHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    private static int port = 4444;
    private boolean alive;
    private ArrayList<Integer> inUse = new ArrayList<>();
    private ArrayList<Room> rooms = new ArrayList<Room>(); // Placeholder for Room class
    private ArrayList<String> roomNames = new ArrayList<String>();
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
     * @param ignore
     */
    public void broadcastAll(Map<String, Object> map, ClientThread ignore) {
        synchronized (allClients) { // Note: Check if synchronisation is necessary
            for (ClientThread c: allClients) {
                if (ignore == null || !ignore.equals(c)) {
                    c.sendMessage(utfEncoder(JsonHandler.constructJsonMessage(map)));
                }
            }
        }
    }

    public void broadcastRoom(Map<String, Object> map, Room room, ClientThread ignore) {
        synchronized (room.getClients()) { // Note: Check if synchronisation is necessary
            for (ClientThread c: room.getClients()) {
                if (ignore == null || !ignore.equals(c)) {
                    c.sendMessage(utfEncoder(JsonHandler.constructJsonMessage(map)));
                }
            }
        }
    }

    /**
     * Server reply to single client
     * Note: To incorporate JSON marshalling
     * @param c
     */
    public void reply(Map<String, Object> map, ClientThread c) {
        synchronized (allClients) { // Note: Check if synchronisation is necessary
                    c.sendMessage(utfEncoder(JsonHandler.constructJsonMessage(map)));
        }
    }

    public void quit(ClientThread client) {
        synchronized (allClients) {
            allClients.remove(client);
            inUse.remove(client.getIdentity().getIdNum());
        }
    }

    public Map<String, Object> roomList(){
        HashMap<String, Object> roomList = new HashMap<>();
        roomList.put("type","roomlist");
        ArrayList<HashMap<String, Object>> roomDict = new ArrayList<>();
        HashMap<String, Object> roomTemp = new HashMap<>();
        for (Room r: rooms){
            roomTemp.put("roomid:" ,r.getRoomId());
            roomTemp.put("count:",r.getClients().size());
            roomDict.add(roomTemp);
            roomTemp.clear();
        }
        roomList.put("rooms", roomDict);
        return roomList;
    }

    public void createRoom(String roomName, String owner){
        Pattern p = Pattern.compile("\\w{3,32}");
        Matcher m = p.matcher(roomName);
        boolean validName = m.matches();
        // Check if already in use;
        Boolean roomExists = roomNames.contains(roomName);
        if (validName && !roomExists){
            Room newRoom = new Room(roomName, owner);
            rooms.add(newRoom);
            roomNames.add(roomName);
        }
    }

    public void deleteRoom(String roomName){
        for (ClientThread c:getRoom(roomName).getClients()){
            getRoom("MainHall").join(c);
        }
        rooms.remove(getRoom(roomName));
    }

    public void autoDeleteEmpty(){
        int len=rooms.size();
        for(int i=0; i<len; i++) {
            if (rooms.get(i).getClients().isEmpty()) {
                deleteRoom(rooms.get(i).getRoomId());
            }
        }
    }

    public void updateOwner(Identity client, String newName){
        int len=rooms.size();
        for(int i=0; i<len; i++) {
            if (rooms.get(i).getOwner().equals(client.getFormer())) {
                rooms.get(i).setOwner(newName);
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

    private String utfEncoder(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        String output = new String(bytes, StandardCharsets.UTF_8);

        return output;
    }

    public ArrayList<Integer> getInUse() {
        return inUse;
    }

    public void setInUse(ArrayList<Integer> inUse) {
        this.inUse = inUse;
    }
}
