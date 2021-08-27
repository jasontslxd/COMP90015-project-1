package chatserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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

    public void quit(ClientThread client){
        clients.remove(client);
        String quitMsg = "";
    }

    public Map<String, Object> roomContents(ClientThread client){
        HashMap<String, Object> roomContents = new HashMap<>();
        roomContents.put("type","roomcontents");
        roomContents.put("roomid",this.roomId);
        ArrayList<String> identitiesList = null;
        for (Identity i:identities){
            if (!i.equals(client)){
                identitiesList.add(i.getIdentity());
            }
        }
        roomContents.put("identities",identitiesList);
        return roomContents;
    }

    public HashSet<ClientThread> getClients() {
        return clients;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
