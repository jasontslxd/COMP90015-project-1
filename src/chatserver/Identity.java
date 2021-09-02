package chatserver;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Identity {

    private String identity;
    private int idNum;

    public Identity(int idNum){
        this.idNum = idNum;
        this.identity = "guest"+ idNum;
    }

    public Identity(){
        this.identity = "";
    }

    public boolean identityChange(Server server, ClientThread client, String newIdentity){

        // Check if identity meets requirement
        Pattern p = Pattern.compile("\\w{3,16}");
        Matcher m = p.matcher(newIdentity);
        boolean validName = m.matches();

        if (validName) {
            this.identity = newIdentity;
            return true;
        } else {
            return false;
        }
    }

    public Map<String, Object> newIdentity(String former, String identity){
        HashMap<String, Object> newIdentity = new HashMap<>();
        newIdentity.put("type","newidentity");
        newIdentity.put("former",former);
        newIdentity.put("identity",identity);
        return newIdentity;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public int getIdNum() {
        return idNum;
    }

}
