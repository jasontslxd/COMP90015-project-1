package chatclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class InputThread extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private boolean alive;

    public InputThread(Socket socket){
        try {
            this.socket = socket;
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            alive = false;
        }
        catch (IOException e) {
            System.out.println("Cannot get input stream from socket: ".concat(e.getMessage()));
        }
    }

    /**
     * Receives information from server and displays it to the client console
     */
    @Override
    public void run() {
        alive = true;
        while (alive) {
            try {
                String inputLine = reader.readLine();
                if (inputLine == null) {
                    System.out.println("Received null input from server, quitting");
                    alive = false;
                }
                else {
                    System.out.println(inputLine);
                }
            } catch (IOException e) {
                System.out.println("Error reading input: ".concat(e.getMessage()));
                alive = false;
            }
        }
        close();
    }

    public void close() {
        try {
            reader.close();
            socket.close();
        }
        catch (IOException e){
            System.out.println("Error closing connection: ".concat(e.getMessage()));
        }
    }
}
