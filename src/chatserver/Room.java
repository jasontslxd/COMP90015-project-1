package chatserver;

import java.util.HashSet;

public class Room {
    private HashSet<ClientThread> clients = new HashSet<>(); // Either lists of clients or identities idk
    private HashSet<Identity> identities = new HashSet<Identity>();
    private String roomId;
    private String owner;

    public Room(String roomid, String owner ){
        this.roomId = roomid;
        this.owner = owner;
    }

    /**
     * Default constructor for room
     * Note: Make it equal to MainHall?
     */
    public Room(){
        this.roomId = "";
        this.owner = "";
    }

    public void join(ClientThread client){
        clients.add(client);
        String joinMsg = "";

    }

    public String quit(ClientThread client){
        clients.remove(client);
        String quitMsg = "";
        return quitMsg;
    }

    public void roomContents(ClientThread client){

    }

    public HashSet<ClientThread> getClients() {
        return clients;
    }

    public String getRoomId() {
        return roomId;
    }
}
