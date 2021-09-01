package chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.google.gson.JsonObject;
import util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClientThread extends Thread{
    private Socket socket;
    private Server server; // Is this necessary?
    private Identity identity; // not sure if needed
    private Room currentRoom;
    String fmrRoomName;
    String currentRoomName;
    private Room fmrRoom;
    //private String currentRoomName = "MainHall";
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connectionAlive = false;

    public ClientThread(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.identity = new Identity(server.getSmallestInt());
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream());
        this.currentRoom = null;
    }

    @Override
    public void run() {
        connectionAlive = true;
        initialSetup();
        while (connectionAlive) {
            try {
                // Alter according to how JSON objects are read in
                String in  = reader.readLine();
                System.out.println(in);
                JsonObject command = JsonHandler.stringToJson(in);
                String cmdType = command.get("type").getAsString();
                switch(cmdType){
                    case "identitychange":
                        handleIdentityChange(command);
                        break;
                    case "join":
                        // Potentially have to pass in rooms rather than create object
                        String newRoom = command.get("roomid").getAsString();
                        fmrRoom = server.getRoom(currentRoom.getRoomId());
                        fmrRoomName = fmrRoom.getRoomId();
                        currentRoom = server.getRoom(newRoom);
                        currentRoomName = currentRoom.getRoomId();
                        try{
                            fmrRoom.quit(this);
                            server.broadcastRoom(roomChange(fmrRoomName,currentRoomName),fmrRoom);
                            currentRoom.join(this);
                            server.reply(currentRoom.roomContents(),this);
                            server.broadcastRoom(roomChange(fmrRoomName,currentRoomName),currentRoom);
                            server.autoDeleteEmpty();
                        } catch (Exception e) { // Create new exception class?
                            server.reply(roomChange(fmrRoomName,currentRoomName), this);
                        }
                        break;
                    case "who":
                        server.reply(currentRoom.roomContents(), this);
                        break;
                    case "list":
                        server.reply(server.roomList(), this);
                        break;
                    case "createroom":
                        String createdRoom = command.get("roomid").getAsString();
                        server.createRoom(createdRoom, this.getIdentity().getIdentity());
                        server.reply(server.roomList(), this);
                        break;
                    case "delete":
                        String deletedRoom = command.get("roomid").getAsString();
                        // Add messages for those kicked out of deleted room?
                        server.deleteRoom(deletedRoom);
                        break;
                    case "message":
                        handleMessage(command);
                        break;
                    case "quit":
                        close();
                        break;
                }
            } catch (IOException e) {
                connectionAlive = false;
            }
        }
        close();
    }

    public void close(){
        try {
            server.quit(this); // May not be necessary
            server.broadcastRoom(roomChange(fmrRoomName,""),currentRoom);
            server.updateOwner(this.identity, "");
            socket.close();
            reader.close();
            writer.close();
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

    public void joinRoom(String roomId) throws KeyNotFoundException {
        this.server.joinRoom(this, roomId);
    }

    private void initialSetup() {
        server.reply(identity.newIdentity(), this);
        server.reply(server.roomList(), this);
        try {
            joinRoom("MainHall");
        } catch (KeyNotFoundException e) {
            System.out.println("Server error, MainHall not found");
        }
    }

    public Map<String, Object> relayedMessage(JsonObject command){
        HashMap<String, Object> relayMessage = new HashMap<>();
        relayMessage.put("type",command.get("type").getAsString());
        relayMessage.put("identity",this.getIdentity().getIdentity());
        relayMessage.put("content",command.get("content").getAsString());
        return relayMessage;
    }

    // Move to server?
    public Map<String, Object> roomChange(String formerRoom, String currentRoom){
        HashMap<String, Object> roomChange = new HashMap<>();
        roomChange.put("type","roomchange");
        roomChange.put("identity",this.getIdentity().getIdentity());
        roomChange.put("former",formerRoom);
        roomChange.put("roomid",currentRoom);
        return roomChange;
    }

    public Socket getSocket() {
        return socket;
    }

    public Identity getIdentity() {
        return identity;
    }

    private void handleMessage(JsonObject jsonMessage) {
        server.broadcastRoom(relayedMessage(jsonMessage), currentRoom);
    }

    private void handleIdentityChange(JsonObject jsonMessage) {
        String newName = jsonMessage.get("identity").getAsString();
        identity.identityChange(server, this,newName);
        server.getInUse().remove((Integer) identity.getIdNum()); // See if this works despite name not being in inUse array
        System.out.println(server.getInUse());
        server.updateOwner(this.identity, newName);
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room currentRoom) {
        this.currentRoom = currentRoom;
    }
}
