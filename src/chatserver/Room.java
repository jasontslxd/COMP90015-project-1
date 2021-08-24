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

    public void quit(ClientThread client){
        clients.remove(client);
        String quitMsg = "";
    }

    public String roomContents(ClientThread client){
        String roomContents;
        String type = "type:roomcontents";
        String roomid = "roomid:"+this.roomId;
        String identitiesList ="";
        for (Identity i:identities){
            if (!i.equals(client)){
                identitiesList += i.getIdentity() +",";
            }
        }
        String identities = "identities:"+identitiesList;
        String owner = "owner:"+this.owner;
        roomContents = type + "," + roomid + "," + identities + "," + owner;
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
