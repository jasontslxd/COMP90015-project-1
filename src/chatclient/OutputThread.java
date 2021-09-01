package chatclient;

import util.JsonHandler;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OutputThread extends Thread {
    private Socket socket;
    private Client client;
    private PrintWriter writer;
    private BufferedReader reader;
    private boolean alive;

    public OutputThread(Socket socket, Client client) {
        try {
            this.socket = socket;
            this.client = client;
            writer = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(System.in));
            alive = false;
        }
        catch (IOException e) {
            System.out.println("Cannot get output stream from socket: ".concat(e.getMessage()));
        }
    }

    /**
     * Reads messages from client and sends to server
     * If the server sends a message and interrupts an unfinished input in console, the input will
     * be wiped and start again from the new prompt
     */
    @Override
    public void run() {
        alive = true;
        while (alive) {
            try {
                String inputLine = reader.readLine();
                if (inputLine == null) {
                    System.out.println("Received null input from client, quitting");
                    alive = false;
                }
                else if (inputLine.equals("") && client.isTimeToPrompt()) {
                    // Dont send anything to the server, prompt again
                    System.out.printf("[%s] %s> ", client.getRoomid(), client.getUsername());
                }
                else {
                    client.setSentMessage(true);
                    String message = convertToProtocol(inputLine);
                    writer.print(message);
                    writer.flush();
                }
            } catch (IOException e) {
                System.out.println("Error reading input, quitting: ".concat(e.getMessage()));
                alive = false;
            }
            catch (InvalidCommandException e) {
                System.out.println("Invalid command: ".concat(e.getMessage()));
            }
        }
        close();
    }

    private String convertToProtocol(String input) throws InvalidCommandException {
        String rawOutput;
        if (input.charAt(0) == '#') {
            rawOutput = buildCommandJsonString(input);
        }
        else {
            rawOutput = buildMessageJsonString(input);
        }
        byte[] bytes = rawOutput.getBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String buildMessageJsonString(String input) {
        Map<String, Object> messageProtocol = new HashMap<>();
        messageProtocol.put("type", "message");
        messageProtocol.put("content", input);
        return JsonHandler.constructJsonMessage(messageProtocol);
    }

    private String buildCommandJsonString(String input) throws InvalidCommandException {
        Map<String, Object> commandProtocol = new HashMap<>();
        String[] contents = input.split("\\s+");
        if (contents.length > 2) {
            throw new InvalidCommandException("Too many arguments to command");
        }
        else {
            String type = contents[0].substring(1);
            try {
                switch (type) {
                    case ("identitychange"):
                        commandProtocol.put("type", type);
                        commandProtocol.put("identity", contents[1]);
                        break;
                    case ("join"):
                    case ("who"):
                    case ("createroom"):
                        client.setSentCreateRoom(true);
                    case ("delete"):
                        commandProtocol.put("type", type);
                        commandProtocol.put("roomid", contents[1]);
                        break;
                    case ("list"):
                    case ("quit"):
                        commandProtocol.put("type", type);
                        break;
                    default:
                        throw new InvalidCommandException("Unknown command: ".concat(type));
                }
            }
            catch (ArrayIndexOutOfBoundsException e) {
                throw new InvalidCommandException("Missing argument in command: ".concat(type));
            }
        }
        return JsonHandler.constructJsonMessage(commandProtocol);
    }

    public void close(){
        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Error closing connection: ".concat(e.getMessage()));
        }
    }
}
