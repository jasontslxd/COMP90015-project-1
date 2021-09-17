package chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.google.gson.JsonObject;
import util.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientThread extends Thread{
    private final Socket socket;
    private final Server server;
    private String identity;
    private int idNum; // Becomes -1 when the client changes name to something other than default
    private Room currentRoom;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connectionAlive = false;

    public ClientThread(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        idNum = server.getAndChangeSmallestInt();
        identity = "guest" + idNum;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(socket.getOutputStream());
        currentRoom = null;
    }

    @Override
    public void run() {
        connectionAlive = true;
        initialSetup();
        while (connectionAlive) {
            try {
                // Alter according to how JSON objects are read in
                String in  = reader.readLine();
                JsonObject command = JsonHandler.stringToJson(in);
                String cmdType = command.get("type").getAsString();
                switch(cmdType){
                    case "identitychange":
                        handleIdentityChange(command);
                        break;
                    case "join":
                        handleJoin(command);
                        break;
                    case "who":
                        handleWho(command);
                        break;
                    case "list":
                        handleList();
                        break;
                    case "createroom":
                        handleCreateRoom(command);
                        break;
                    case "delete":
                        handleDelete(command);
                        break;
                    case "message":
                        handleMessage(command);
                        break;
                    case "quit":
                        connectionAlive = false;
                        break;
                }
            } catch (IOException | KeyNotFoundException e) {
                connectionAlive = false;
            }
        }
        close();
    }

    public void close(){
        try {
            server.quit(this);
            if (idNum > 0){
                server.getInUse().remove((Integer) idNum);
            }
            server.getInUse().remove((Integer) idNum);
            socket.close();
            reader.close();
            writer.close();
            System.out.printf("Disconnected from %s:%d\n", socket.getLocalAddress(), socket.getPort());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Send JSON message somehow
     * @param message
     */
    public void sendMessage(String message) {
        writer.print(message);
        writer.flush();
    }

    private void initialSetup(){
        server.reply(newIdentity("", identity), this);
        server.getAllClients().add(this);
        joinRoom("MainHall");
    }

    private Map<String, Object> newIdentity(String former, String identity){
        HashMap<String, Object> newIdentity = new HashMap<>();
        newIdentity.put("type","newidentity");
        newIdentity.put("former",former);
        newIdentity.put("identity",identity);
        return newIdentity;
    }

    public Map<String, Object> relayedMessage(JsonObject command){
        HashMap<String, Object> relayMessage = new HashMap<>();
        relayMessage.put("type",command.get("type").getAsString());
        relayMessage.put("identity",identity);
        relayMessage.put("content",command.get("content").getAsString());
        return relayMessage;
    }

    // Move to server?
    public Map<String, Object> roomChange(String formerRoom, String currentRoom){
        HashMap<String, Object> roomChange = new HashMap<>();
        roomChange.put("type","roomchange");
        roomChange.put("identity",identity);
        roomChange.put("former",formerRoom);
        roomChange.put("roomid",currentRoom);
        return roomChange;
    }

    public String getIdentity() {
        return identity;
    }

    private void handleMessage(JsonObject jsonMessage) {
        server.broadcastRoom(relayedMessage(jsonMessage), currentRoom);
    }

    private void handleWho(JsonObject jsonMessage) {
        String roomId = jsonMessage.get("roomid").getAsString();
        if (server.getRoomMap().containsKey(roomId)){
            server.reply(server.getRoomMap().get(roomId).roomContents(), this);
        }
    }

    private void handleList() {
        server.reply(server.roomList(), this);
    }

    private void handleIdentityChange(JsonObject jsonMessage) {
        String newIdentity = jsonMessage.get("identity").getAsString();
        Map<String, Object> newIdentityMessage = new HashMap<>();
        newIdentityMessage.put("type", "newidentity");
        newIdentityMessage.put("former", identity);
        boolean identityAvailable = true;
        for (ClientThread client : server.getAllClients()) {
            if (client.identity.equals(newIdentity)) {
                identityAvailable = false;
                break;
            }
        }
        if (checkValidIdentity(newIdentity) && identityAvailable) {
            newIdentityMessage.put("identity", newIdentity);
            if (idNum > 0) {
                server.getInUse().remove((Integer) idNum);
                idNum = -1;
            }
            server.broadcastAll(newIdentityMessage);
            identity = newIdentity;
        }
        else {
            newIdentityMessage.put("identity", identity);
            server.reply(newIdentityMessage, this);
        }
    }

    private boolean checkValidIdentity(String identity) {
        Pattern p = Pattern.compile("\\w{3,16}");
        Matcher m = p.matcher(identity);
        return m.matches() && !identity.startsWith("guest");
    }

    /**
     * Sends a roomlist with just the newly created room if successfully created, otherwise send an empty list. If we
     * just send the entire roomlist, the client would have no way of differentiating if the room was already there
     * before creating or they have successfully created the room
     * @param jsonMessage input command
     */
    private void handleCreateRoom(JsonObject jsonMessage) {
        String roomToCreate = jsonMessage.get("roomid").getAsString();
        boolean success = server.createRoom(roomToCreate, this);

        Map<String, Object> roomListCommand;
        if (!success && server.getRoomMap().containsKey(roomToCreate)) {
            roomListCommand = server.roomListCreate(roomToCreate);
        }else {
            roomListCommand = server.roomList();
        }
        server.reply(roomListCommand, this);
    }

    private void handleJoin(JsonObject jsonMessage) {
        String newRoomId = jsonMessage.get("roomid").getAsString();
        joinRoom(newRoomId);
    }

    public void joinRoom(String roomId){
        try {
            server.joinRoom(roomId, this);
        }
        catch (KeyNotFoundException e) {
            server.reply(roomChange(currentRoom.getRoomId(), currentRoom.getRoomId()), this);
        }
    }

    private void handleDelete(JsonObject jsonMessage) throws KeyNotFoundException {
        String deletedRoom = jsonMessage.get("roomid").getAsString();
        boolean success = server.deleteRoom(deletedRoom, this);
//        server.reply(server.roomList(), this);
        Map<String, Object> roomListCommand = new HashMap<>();
        if (!success && server.getRoomMap().containsKey(deletedRoom)) {
            roomListCommand = server.roomListDelete(deletedRoom);
        }else {
            roomListCommand = server.roomList();
        }
        server.reply(roomListCommand, this);
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room currentRoom) {
        this.currentRoom = currentRoom;
    }
}
