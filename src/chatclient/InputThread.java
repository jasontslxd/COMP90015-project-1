package chatclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import util.JsonHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class InputThread extends Thread {
    private Client client;
    private Socket socket;
    private BufferedReader reader;
    private boolean alive = false;

    public InputThread(Socket socket, Client client){
        try {
            this.socket = socket;
            this.client = client;
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (IOException e) {
            System.out.println("Cannot get input stream from socket: ".concat(e.getMessage()));
        }
    }

    /**
     * Receives information from server and displays it to the client console
     */
    @Override
    public void run() {
        alive = true;
        while (alive) {
            try {
                String inputLine = reader.readLine();
                if (inputLine == null) {
                    System.out.println("Received null input from server, quitting");
                    alive = false;
                }
                else {
                    handleServerReply(inputLine);
                }
                if (client.isTimeToPrompt()) {
                    System.out.printf("[%s] %s> ", client.getRoomid(), client.getUsername());
                }
            } catch (IOException e) {
                System.out.println("Error reading input: ".concat(e.getMessage()));
                alive = false;
            }
        }
        close();
    }

    public void close() {
        try {
            reader.close();
            socket.close();
        }
        catch (IOException e){
            System.out.println("Error closing connection: ".concat(e.getMessage()));
        }
    }

    private void handleServerReply(String reply) {
        JsonObject jsonMessage = JsonHandler.stringToJson(reply);
        String type = jsonMessage.get("type").getAsString();
        if (client.isTimeToPrompt() && !client.hasSentMessage()) {
            System.out.println();
        }
        switch (type) {
            case "newidentity":
                handleNewIdentity(jsonMessage);
                break;
            case "roomchange":
                handleRoomChange(jsonMessage);
                break;
            case "roomcontents":
                handleRoomContents(jsonMessage);
                break;
            case "roomlist":
                handleRoomList(jsonMessage);
                break;
            case "message":
                handleMessage(jsonMessage);
                break;
        }
        client.setSentMessage(false);
    }

    private void handleNewIdentity(JsonObject jsonMessage) {
        if (jsonMessage.get("former").getAsString().equals("")) {
            // Connecting for the first time
            String username = jsonMessage.get("identity").getAsString();
            System.out.printf("Connected to %s as %s\n", client.getHostname(), username);
            client.setUsername(username);
        }
        else {
            String former = jsonMessage.get("former").getAsString();
            String identity = jsonMessage.get("identity").getAsString();
            if (former.equals(identity)){
                System.out.println("Requested identity invalid or in use");
            }
            else {
                System.out.printf("%s is now %s\n", former, identity);
                if (client.getUsername().equals(former)){
                    client.setUsername(identity);
                }
            }
        }
    }

    private void handleMessage(JsonObject jsonMessage) {
        String identity = jsonMessage.get("identity").getAsString();
        String content = jsonMessage.get("content").getAsString();
        System.out.printf("%s: %s\n", identity, content);
    }

    private void handleRoomList(JsonObject jsonMessage) {
        JsonArray roomData = jsonMessage.get("rooms").getAsJsonArray();
        if (client.getSentCreateRoom()) {
            for (JsonElement roomInstance : roomData) {
                String roomid = roomInstance.getAsJsonObject().get("roomid").getAsString();
            }
        }
        else {
            for (JsonElement roomInstance : roomData) {
                JsonObject room = roomInstance.getAsJsonObject();
                String roomid = room.get("roomid").getAsString();
                int count = room.get("count").getAsInt();
                String plural = count == 1 ? "guest" : "guests";
                System.out.printf("%s: %d %s \n", roomid, count, plural);
            }
        }

    }

    private void handleRoomContents(JsonObject jsonMessage) {
        String roomid = jsonMessage.get("roomid").getAsString();
        JsonArray identities = jsonMessage.get("identities").getAsJsonArray();
        String owner = jsonMessage.get("owner").getAsString();
        if (identities.isEmpty()) {
            System.out.printf("%s contains no users\n", roomid);
        }
        else {
            StringBuilder sb = new StringBuilder();
            for (JsonElement identity : identities) {
                String username = identity.getAsString();
                if (username.equals(owner)) {
                    sb.append(username).append("* ");
                }
                else {
                    sb.append(username).append(" ");
                }
            }
            String users = sb.toString().trim();
            System.out.printf("%s contains %s\n", roomid, users);
        }
    }

    private void handleRoomChange(JsonObject jsonMessage) {
        String identity = jsonMessage.get("identity").getAsString();
        String former = jsonMessage.get("former").getAsString();
        String roomid = jsonMessage.get("roomid").getAsString();
        if (former.equals("") && roomid.equals("MainHall")) {
            // New user connecting to main hall
            System.out.printf("%s has joined %s\n", identity, roomid);
        }
        else {
            System.out.printf("%s moved from %s to %s\n", identity, former, roomid);
        }
        client.setTimeToPrompt(true);
    }
}
