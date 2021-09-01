package chatserver;

import util.JsonHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import util.KeyNotFoundException;

public class Server {
    public static final int DEFAULT_PORT = 4444;

    @Option(name="-p", usage="Specify port number")
    private int port = DEFAULT_PORT;

    private boolean alive;
    private ArrayList<Integer> inUse = new ArrayList<>();
    private final HashMap<String, Room> roomMap = new HashMap<>();
    private ExecutorService pool = Executors.newFixedThreadPool(8);

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
        Room mainHall = new Room("MainHall", "");
        roomMap.put(mainHall.getRoomId(), mainHall);
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
//                client.start();
                pool.execute(client);
                System.out.printf("Accepted new connection from %s:%d\n", socket.getLocalAddress(), socket.getPort());
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
        synchronized (roomMap) { // Note: Check if synchronisation is necessary
            for (Room room : roomMap.values()) {
                for (ClientThread client : room.getClients()) {
                    client.sendMessage(utfEncoder(JsonHandler.constructJsonMessage(map)));
                }
            }
        }
    }

    public void broadcastRoom(Map<String, Object> map, Room room) {
        synchronized (room.getClients()) { // Note: Check if synchronisation is necessary
            for (ClientThread client: room.getClients()) {
                client.sendMessage(utfEncoder(JsonHandler.constructJsonMessage(map)));
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
            // Exception in thread "pool-1-thread-2" java.lang.IndexOutOfBoundsException: Index 2 out of bounds for length 2
            inUse.remove(client.getIdentity().getIdNum());
        }
    }

    public void joinRoom(ClientThread client, String roomId) throws KeyNotFoundException {
        if (roomMap.containsKey(roomId)) {
            Room newRoom = roomMap.get(roomId);
            Room oldRoom = client.getCurrentRoom();

            // Notify other clients in room
            HashMap<String, Object> roomChange = new HashMap<>();
            roomChange.put("type", "roomchange");
            roomChange.put("identity", client.getIdentity().getIdentity());
            roomChange.put("former", oldRoom == null ? "" : oldRoom.getRoomId());
            roomChange.put("roomid", roomId);

            client.setCurrentRoom(newRoom);
            if (oldRoom != null) {
                oldRoom.quit(client);
                broadcastRoom(roomChange, oldRoom);
            }
            newRoom.join(client);
            broadcastRoom(roomChange, newRoom);
        }
        else {
            throw new KeyNotFoundException("Room does not exist: ".concat(roomId));
        }
    }

    public Map<String, Object> roomList(){
        HashMap<String, Object> roomList = new HashMap<>();
        roomList.put("type","roomlist");
        ArrayList<HashMap<String, Object>> roomDict = new ArrayList<>();
        for (Room r: roomMap.values()){
            HashMap<String, Object> roomData = new HashMap<>();
            roomData.put("roomid" ,r.getRoomId());
            roomData.put("count",r.getClients().size());
            roomDict.add(roomData);
        }
        roomList.put("rooms", roomDict);
        return roomList;
    }

    public void createRoom(String roomName, String owner){
        Pattern p = Pattern.compile("\\w{3,32}");
        Matcher m = p.matcher(roomName);
        boolean validName = m.matches();
        // Check if already in use;
        boolean roomExists = roomNames.contains(roomName);
        if (validName && !roomExists){
            Room newRoom = new Room(roomName, owner);
            roomMap.put(roomName, newRoom);
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
