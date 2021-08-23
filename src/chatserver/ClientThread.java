package chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientThread extends Thread{
    private Socket socket;
    private Server server; // Is this necessary?
    private Identity identity; // not sure if needed
    private String currentRoom = "MainHall";
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connectionAlive = false;

    public ClientThread(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.identity = new Identity(server.getSmallestInt());
        // Replace below with JSON replacements
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream());
    }

    @Override
    public void run() {
        connectionAlive = true;
        // Initial NewIdentity message
        server.reply(identity.newIdentity(), this);
        while (connectionAlive) {
            try {
                // Alter according to how JSON objects are read in
                String in  = reader.readLine();
                switch(in){
                    case "identitychange":
                        identity.identityChange(server, this,""); // Placeholder for new name
                        server.getInUse().remove(identity.getIdNum()); // See if this works despite name not being in inUse array
                        break;
                    case "join":
                        String formerRoom = currentRoom;
                        currentRoom = ""; // Placeholder for room name to join into
                        getRoom(server.getRooms(), "");// Placeholder for room name to join into
                        server.broadcastRoom(roomChange(formerRoom,currentRoom),getRoom(server.getRooms(),formerRoom), this);
                        server.broadcastRoom(roomChange(formerRoom,currentRoom),getRoom(server.getRooms(),currentRoom), this);
                        break;
                    case "who":
                        break;
                    case "list":
                        break;
                    case "createroom":
                        break;
                    case "delete":
                        break;
                    case "message":
                        server.broadcastRoom("", getRoom(server.getRooms(),currentRoom), this);
                        break;
                    case "quit":
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
            server.leave(this); // May not be necessary
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

    public String roomChange(String formerRoom, String currentRoom){
        String roomChange;
        roomChange = "";
        String type = "type:roomchange";
        String identity = "identity:"+this.getIdentity().getIdentity(); // Consider renaming
        String former = "former:"+formerRoom;
        String roomid = "roomid:"+currentRoom;
        roomChange = type + "," + identity + "," + former + "," + roomid;
        return roomChange;
    }

    private Room getRoom(ArrayList<Room> rooms, String roomName){
        int len=rooms.size();
        for(int i=0; i<len; i++) {
            if (rooms.get(i).getRoomId().equals(roomName)) {
                return rooms.get(i);
            }
        }
        return null;
    }

    public Socket getSocket() {
        return socket;
    }

    public Identity getIdentity() {
        return identity;
    }
}
