package chatserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Identity {

    private String identity;
    private String former;
    private int idNum;

    public Identity(int idNum){
        this.idNum = idNum;
        this.identity = "guest"+String.valueOf(idNum);
        this.former = "";
    }

    public Identity(){
        this.identity = "";
        this.former = "";
    }

    public void identityChange(Server server, ClientThread client, String newIdentity){

        // Check if identity meets requirement
        Pattern p = Pattern.compile("\\w{3,16}");
        Matcher m = p.matcher(newIdentity);
        boolean validName = m.matches();

        if (validName) {
            this.former = identity;
            this.identity = newIdentity;
            server.broadcastAll(newIdentity(), client);
        } else {
            server.reply(newIdentity(),client);
        }
    }

    public Map<String, Object> newIdentity(){
        HashMap<String, Object> newIdentity = new HashMap<>();
        newIdentity.put("type","newidentity");
        newIdentity.put("former",getFormer());
        newIdentity.put("identity",getIdentity());
        return newIdentity;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getFormer() {
        return former;
    }

    public void setFormer(String former) {
        this.former = former;
    }

    public int getIdNum() {
        return idNum;
    }

}
