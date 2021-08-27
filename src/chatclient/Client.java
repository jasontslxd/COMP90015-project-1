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
            e.printStackTrace();
        }
        client.handle();
    }

    public void handle() {
        try {
            socket = new Socket(hostname, port);
            inputThread = new InputThread(socket);
            outputThread = new OutputThread(socket);
            inputThread.start();
            outputThread.start();
            System.out.printf("Connected to %s at port %d\n", hostname, port);
        } catch (UnknownHostException e) {
            System.out.println("Server not found: ".concat(e.getMessage()));
        } catch (IOException e) {
            System.out.println("IO error: ".concat(e.getMessage()));
        }
    }
}
