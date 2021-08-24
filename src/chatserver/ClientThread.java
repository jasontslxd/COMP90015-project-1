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
    private Room currentRoom;
    private Room fmrRoom;
    //private String currentRoomName = "MainHall";
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
        server.reply(server.roomList(), this);
        while (connectionAlive) {
            try {
                // Alter according to how JSON objects are read in
                String in  = reader.readLine();
                switch(in){
                    case "identitychange":
                        identity.identityChange(server, this,""); // Placeholder for new name
                        server.getInUse().remove(identity.getIdNum()); // See if this works despite name not being in inUse array
                        server.updateOwner(this.identity);
                        break;
                    case "join":
                        // Potentially have to pass in rooms rather than create object
                        fmrRoom = server.getRoom(currentRoom.getRoomId());
                        currentRoom = server.getRoom( "");// Placeholder for room name to join into
                        try{
                            fmrRoom.quit(this);
                            server.broadcastRoom(roomChange(fmrRoom.getRoomId(),currentRoom.getRoomId()),fmrRoom, this);
                            currentRoom.join(this);
                            server.reply(currentRoom.roomContents(this),this);
                            server.broadcastRoom(roomChange(fmrRoom.getRoomId(),currentRoom.getRoomId()),currentRoom, this);
                        } catch (Exception e) { // Create new exception class?
                            server.reply("The requested room is invalid or non existent.", this);
                        }
                        break;
                    case "who":
                        server.reply(currentRoom.roomContents(this), this);
                        break;
                    case "list":
                        server.reply(server.roomList(), this);
                        break;
                    case "createroom":
                        String roomName = "";
                        server.createRoom(roomName, this.getIdentity().getIdentity());
                        break;
                    case "delete":
                        break;
                    case "message":
                        server.broadcastRoom("", currentRoom, this);
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

    // Move to server?
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

    private Room getRoom(ArrayList<Room> rooms, String roomName){ // Duplicated in server class, remove one
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
