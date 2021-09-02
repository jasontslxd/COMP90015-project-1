package chatclient;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Client {
    public static final int DEFAULT_PORT = 4444;

    @Option(name="-p", usage="Specify port number")
    private int port = DEFAULT_PORT;

    private final String hostname;
    private Socket socket;
    private InputThread inputThread;
    private OutputThread outputThread;
    private String username;
    private String roomid;
    private boolean sentMessage = false;
    private boolean timeToPrompt = false;
    private String createRoomName;
    private String deleteRoomName;
    private boolean alive = false;

    public Client(String hostname) {
        this.hostname = hostname;
    }

    public static void main(String[] args){
        if (args.length == 0) {
            System.out.println("No hostname received");
            System.exit(-1);
        }
        Client client = new Client(args[0]);
        CmdLineParser parser = new CmdLineParser(client);
        try {
            parser.parseArgument(Arrays.copyOfRange(args, 1, args.length));
        } catch (CmdLineException e) {
            System.out.println("Command line input is not well formed: ".concat(e.getMessage()));
            System.exit(-1);
        }
        client.handle();
    }

    public void handle() {
        try {
            socket = new Socket(hostname, port);
            inputThread = new InputThread(socket, this);
            outputThread = new OutputThread(socket, this);
            inputThread.start();
            outputThread.start();
            alive = true;
        } catch (UnknownHostException e) {
            System.out.println("Server not found: ".concat(e.getMessage()));
        } catch (IOException e) {
            System.out.println("IO error: ".concat(e.getMessage()));
        }
    }

    public void promptInput() {
        if (isTimeToPrompt()) {
            System.out.printf("[%s] %s> ", getRoomid(), getUsername());
        }
    }

    public void close() {
        alive = false;
        try {
            socket.close();
            outputThread.close();
            inputThread.close();
            System.out.printf("Disconnected from %s\n", hostname);
            System.exit(0);
        }
        catch (IOException e){
            System.out.println("Error closing connection: ".concat(e.getMessage()));
        }
    }

    public String getHostname(){
        return hostname;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setRoomid(String roomid) {
        this.roomid = roomid;
    }

    public String getRoomid() {
        return roomid;
    }

    public boolean hasSentMessage() {
        return sentMessage;
    }

    public void setSentMessage(boolean sentMessage) {
        this.sentMessage = sentMessage;
    }

    public boolean isTimeToPrompt() {
        return timeToPrompt;
    }

    public void setTimeToPrompt(boolean timeToPrompt) {
        this.timeToPrompt = timeToPrompt;
    }

    public String getCreateRoomName() {
        return createRoomName;
    }

    public void setCreateRoomName(String createRoomName) {
        this.createRoomName = createRoomName;
    }

    public String getDeleteRoomName() {
        return deleteRoomName;
    }

    public void setDeleteRoomName(String deleteRoomName) {
        this.deleteRoomName = deleteRoomName;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
