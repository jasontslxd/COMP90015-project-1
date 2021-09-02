package chatserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Room {
    private ArrayList<ClientThread> clients = new ArrayList<>(); // Either lists of clients or identities idk
    private String roomId;
    private ClientThread owner;

    public Room(String roomid, ClientThread owner){
        this.roomId = roomid;
        this.owner = owner;
    }

    /**
     * Default constructor for room
     * Note: Make it equal to MainHall?
     */
    public Room(){
        this.roomId = "";
        this.owner = null;
    }

    public void join(ClientThread client){
        clients.add(client);
    }

    public void quit(ClientThread client){
        clients.remove(client);
    }

    public Map<String, Object> roomContents(){
        HashMap<String, Object> roomContents = new HashMap<>();
        roomContents.put("type","roomcontents");
        roomContents.put("roomid",roomId);
        roomContents.put("owner", owner == null ? "" : owner.getIdentity());
        ArrayList<String> identitiesList = new ArrayList<>();
        for (ClientThread client : clients) {
            identitiesList.add(client.getIdentity());
        }
        roomContents.put("identities",identitiesList);
        return roomContents;
    }

    public ArrayList<ClientThread> getClients() {
        return clients;
    }

    public String getRoomId() {
        return roomId;
    }

    public ClientThread getOwner() {
        return owner;
    }

    public void setOwner(ClientThread owner) {
        this.owner = owner;
    }
}
