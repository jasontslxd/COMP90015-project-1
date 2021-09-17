package chatserver;

import util.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

    private final ArrayList<Integer> inUse = new ArrayList<>();
    private final HashMap<String, Room> roomMap = new HashMap<>();
    private final HashSet<ClientThread> allClients = new HashSet<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(100);

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
        Room mainHall = new Room("MainHall", null);
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
            while (true) {
                Socket socket = serverSocket.accept();
                ClientThread client = new ClientThread(socket, this);
                pool.execute(client);
                System.out.printf("Accepted new connection from %s:%d\n", socket.getLocalAddress(), socket.getPort());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Broadcast message out to clients
     * Note: To incorporate JSON marshalling
     */
    public synchronized void broadcastAll(Map<String, Object> map) {
        for (ClientThread c: allClients) {
            c.sendMessage(JsonHandler.constructJsonMessage(map));
        }
    }

    public synchronized void broadcastRoom(Map<String, Object> map, Room room) {
        for (ClientThread client: room.getClients()) {
            client.sendMessage(JsonHandler.constructJsonMessage(map));
        }
    }

    /**
     * Server reply to single client
     * Note: To incorporate JSON marshalling
     * @param c
     */
    public void reply(Map<String, Object> map, ClientThread c) {
        c.sendMessage(JsonHandler.constructJsonMessage(map));
    }

    public synchronized void quit(ClientThread client) {
        allClients.remove(client);
        Room clientRoom = client.getCurrentRoom();
        for (Room room : roomMap.values()) {
            if (room.getOwner() != null && room.getOwner().equals(client)) {
                room.setOwner(null);
            }
        }
        broadcastRoom(client.roomChange(clientRoom.getRoomId(),""), clientRoom);
        clientRoom.quit(client);
        autoDeleteEmpty();
    }

    public synchronized void joinRoom(String roomId, ClientThread client) throws KeyNotFoundException {
        if (roomMap.containsKey(roomId)) {
            Room newRoom = roomMap.get(roomId);
            Room oldRoom = client.getCurrentRoom();

            // Notify other clients in room
            HashMap<String, Object> roomChange = new HashMap<>();
            roomChange.put("type", "roomchange");
            roomChange.put("identity", client.getIdentity());
            roomChange.put("former", oldRoom == null ? "" : oldRoom.getRoomId());
            roomChange.put("roomid", roomId);

            if (oldRoom != newRoom) {
                client.setCurrentRoom(newRoom);
                if (oldRoom != null) {
                    oldRoom.quit(client);
                    broadcastRoom(roomChange, oldRoom);
                }
                newRoom.join(client);
                broadcastRoom(roomChange, newRoom);
                autoDeleteEmpty();
                if (roomId.equals("MainHall")&&(oldRoom==null||!oldRoom.getRoomId().equals("MainHall"))) {
                    reply(roomMap.get("MainHall").roomContents(), client);
                    reply(roomList(), client);
                }
            }
            else {
                reply(roomChange, client);
            }
        }
        else {
            throw new KeyNotFoundException("Room does not exist: ".concat(roomId));
        }
    }

    public synchronized Map<String, Object> roomList(){
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

    public synchronized Map<String, Object> roomList(String roomName){
        HashMap<String, Object> roomList = new HashMap<>();
        roomList.put("type","roomlist");
        ArrayList<HashMap<String, Object>> roomDict = new ArrayList<>();
        for (Room r: roomMap.values()){
            if (!r.getRoomId().equals(roomName)) {
                HashMap<String, Object> roomData = new HashMap<>();
                roomData.put("roomid", r.getRoomId());
                roomData.put("count", r.getClients().size());
                roomDict.add(roomData);
            }
        }
        roomList.put("rooms", roomDict);
        return roomList;
    }

    public synchronized boolean createRoom(String roomName, ClientThread owner){
        Pattern p = Pattern.compile("\\w{3,32}");
        Matcher m = p.matcher(roomName);
        boolean validName = m.matches();
        // Check if already in use;
        boolean roomExists = roomMap.containsKey(roomName);
        if (validName && !roomExists){
            Room newRoom = new Room(roomName, owner);
            roomMap.put(roomName, newRoom);
            return true;
        }
        return false;
    }

    public synchronized void deleteRoom(String roomName, ClientThread client) throws KeyNotFoundException {
        if (roomMap.containsKey(roomName) && client.equals(roomMap.get(roomName).getOwner())) {
            ArrayList<ClientThread> toBeMoved = new ArrayList<>(roomMap.get(roomName).getClients());
            for (ClientThread c : toBeMoved) {
                joinRoom("MainHall", c);
            }
            roomMap.remove(roomName);
        }
    }

    public synchronized void autoDeleteEmpty() {
        ArrayList<String> toBeDeleted = new ArrayList<>();
        for (Room room : roomMap.values()) {
            if (room.getClients().isEmpty() && room.getOwner() == null && !room.getRoomId().equals("MainHall")) {
                toBeDeleted.add(room.getRoomId());
            }
        }
        for (String roomId : toBeDeleted) {
            roomMap.remove(roomId);
        }
    }

    public synchronized int getAndChangeSmallestInt(){
        int smallest = 1;
        Set<Integer> inUseSet = new HashSet<>();
        for (int id : inUse) {
            if (id > 0) {
                inUseSet.add(id);
            }
        }
        for (int i = 1; i <= inUse.size() + 1; i++){
            if (!inUseSet.contains(i)) {
                smallest = i;
                break;
            }
        }
        inUse.add(smallest);
        return smallest;
    }

    public synchronized ArrayList<Integer> getInUse() {
        return inUse;
    }

    public synchronized HashMap<String, Room> getRoomMap() {
        return roomMap;
    }

    public synchronized HashSet<ClientThread> getAllClients() {
        return allClients;
    }
}
