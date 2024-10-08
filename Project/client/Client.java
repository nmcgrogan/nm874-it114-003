package Project.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.logging.Logger;

import Project.common.Constants;
import Project.common.Payload;
import Project.common.PayloadType;
import Project.common.RoomResultPayload;

public enum Client {
    INSTANCE;

    Socket server = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    final String ipAddressPattern = "/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})";
    final String localhostPattern = "/connect\\s+(localhost:\\d{3,5})";
    boolean isRunning = false;
    private Thread inputThread;
    private Thread fromServerThread;
    // private String clientName = "";
    private ClientPlayer myPlayer = new ClientPlayer();
    private long myClientId = Constants.DEFAULT_CLIENT_ID;
    private static Logger logger = Logger.getLogger(Client.class.getName());

    private Hashtable<Long, ClientPlayer> userList = new Hashtable<Long, ClientPlayer>();
    //Grid clientGrid = new Grid();

    private static IClientEvents events;
    /*nm874
     * 12/8/23
     */
    private void sendWhisperPayload(String text) throws IOException {
        // Assume the format of the whisper command is "/whisper username message"
        String[] parts = text.split(" ", 3);
        if (parts.length >= 3) {
            String username = parts[1];
            String message = parts[2];
            Payload p = new Payload();
            p.setPayloadType(PayloadType.WHISPER); // You need to define this type in PayloadType enum
            p.setClientName(username); // Set the recipient's username
            p.setMessage(message); // Set the actual message
            out.writeObject(p); // Send the payload
        } else {
            // Handle error: command format is incorrect
        }
    }
    
    private void sendMutePayload(String text) throws IOException {
        // Assume the format of the mute command is "/mute username"
        String[] parts = text.split(" ", 2);
        if (parts.length == 2) {
            String username = parts[1];
            Payload p = new Payload();
            p.setPayloadType(PayloadType.MUTE); // You need to define this type in PayloadType enum
            p.setMessage(username); // Set the username to be muted
            out.writeObject(p); // Send the payload
        } else {
            // Handle error: command format is incorrect
        }
    }
    
    private void sendUnmutePayload(String text) throws IOException {
        // Assume the format of the unmute command is "/unmute username"
        String[] parts = text.split(" ", 2);
        if (parts.length == 2) {
            String username = parts[1];
            Payload p = new Payload();
            p.setPayloadType(PayloadType.UNMUTE); // You need to define this type in PayloadType enum
            p.setMessage(username); // Set the username to be unmuted
            out.writeObject(p); // Send the payload
        } else {
            // Handle error: command format is incorrect
        }
    }
    private void sendRollPayload(String text) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROLL);
        
        // Extracting dice count and sides from the command
        if (text.matches("/roll \\d+d\\d+")) {
            String[] parts = text.substring(6).split("d");
            int diceCount = Integer.parseInt(parts[0]);
            int diceSides = Integer.parseInt(parts[1]);
            p.setDiceCount(diceCount);
            p.setDiceSides(diceSides);
        }
        
        out.writeObject(p);
    }

    // Method to send a flip command payload
    private void sendFlipPayload() throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.FLIP);
        out.writeObject(p);
    }
/*nm874
     * 12/8/23
     */
    // Method to process client commands
    public void processClientCommand(String text) throws IOException {
        if (text.startsWith("/roll")) {
            sendRollPayload(text);
        } else if (text.equals("/flip")) {
            sendFlipPayload();
        } else if (text.startsWith("/whisper")) {
            sendWhisperPayload(text);
        } else if (text.startsWith("/mute")) {
            sendMutePayload(text);
        } else if (text.startsWith("/unmute")) {
            sendUnmutePayload(text);
        }
        }
        public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine
        // if the server had a problem
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();

    }

    /**
     * Takes an ip address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @param username
     * @param callback (for triggering UI events)
     * @return true if connection was successful
     */
    public boolean connect(String address, int port, String username, IClientEvents callback) {
        // TODO validate
        // this.clientName = username;
        myPlayer.setClientName(username);
        Client.events = callback;
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            logger.info("Client connected");
            listenForServerPayload();
            sendConnect();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    /*  Send methods
    protected void sendMove(int x, int y) throws IOException {
        PositionPayload pp = new PositionPayload();
        pp.setCoord(x, y);
        out.writeObject(pp);
    }

    protected void sendLoadCharacter(String characterCode) throws IOException {
        CharacterPayload cp = new CharacterPayload();
        Character c = new Character();
        c.setCode(characterCode);
        cp.setCharacter(c);
        out.writeObject(cp);
    }

    protected void sendCreateCharacter(CharacterType characterType) throws IOException {
        CharacterPayload cp = new CharacterPayload();
        cp.setCharacterType(characterType);
        out.writeObject(cp);
    }
*/
    protected void sendReadyStatus() throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.READY);
        out.writeObject(p);
    }

    public void sendListRooms(String query) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.GET_ROOMS);
        p.setMessage(query);
        out.writeObject(p);
    }

    public void sendJoinRoom(String roomName) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.JOIN_ROOM);
        p.setMessage(roomName);
        out.writeObject(p);
    }

    public void sendCreateRoom(String roomName) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.CREATE_ROOM);
        p.setMessage(roomName);
        out.writeObject(p);
    }

    protected void sendDisconnect() throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.DISCONNECT);
        out.writeObject(p);
    }

    protected void sendConnect() throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.CONNECT);
        p.setClientName(myPlayer.getClientName()); // Set the client's name
        out.writeObject(p);
    }

    public void sendMessage(String message) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
    
        p.setMessage(message);
        p.setClientName(myPlayer.getClientName());
        out.writeObject(p);
    }

    // end send methods

    private void listenForServerPayload() {
        isRunning = true;
        fromServerThread = new Thread() {
            @Override
            public void run() {
                try {
                    Payload fromServer;

                    // while we're connected, listen for objects from server
                    while (isRunning && !server.isClosed() && !server.isInputShutdown()
                            && (fromServer = (Payload) in.readObject()) != null) {

                        logger.info("Debug Info: " + fromServer);
                        processPayload(fromServer);

                    }
                    logger.info("listenForServerPayload() loop exited");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    logger.info("Stopped listening to server input");
                    close();
                }
            }
        };
        fromServerThread.start();// start the thread
    }

    protected String getClientNameById(long id) {
        if (userList.containsKey(id)) {
            return userList.get(id).getClientName();
        }
        if (id == Constants.DEFAULT_CLIENT_ID) {
            return "[Server]";
        }
        return "unkown user";
    }
    /**
     * Processes incoming payloads from ServerThread
     * 
     * @param p
     */
    private void processPayload(Payload p) {
        switch (p.getPayloadType()) {
            case CONNECT:
                if (!userList.containsKey(p.getClientId())) {
                    ClientPlayer cp = new ClientPlayer();
                    cp.setClientName(p.getClientName());
                    cp.setClientId(p.getClientId());
                    userList.put(p.getClientId(), cp);
                }
                System.out.println(String.format("*%s %s*",
                        p.getClientName(),
                        p.getMessage()));
                events.onClientConnect(p.getClientId(), p.getClientName(), p.getMessage());
                break;
            case DISCONNECT:
                if (userList.containsKey(p.getClientId())) {
                    userList.remove(p.getClientId());
                }
                if (p.getClientId() == myClientId) {
                    myClientId = Constants.DEFAULT_CLIENT_ID;
                }
                System.out.println(String.format("*%s %s*",
                        p.getClientName(),
                        p.getMessage()));
                events.onClientDisconnect(p.getClientId(), p.getClientName(), p.getMessage());
                break;
            case SYNC_CLIENT:
                if (!userList.containsKey(p.getClientId())) {
                    ClientPlayer cp = new ClientPlayer();
                    cp.setClientName(p.getClientName());
                    cp.setClientId(p.getClientId());
                    userList.put(p.getClientId(), cp);
                }
                events.onSyncClient(p.getClientId(), p.getClientName());
                break;
            case MESSAGE:
                System.out.println(String.format("%s: %s",
                        getClientNameById(p.getClientId()),
                        p.getMessage()));
                events.onMessageReceive(p.getClientId(), p.getMessage());
                break;
            case CLIENT_ID:
                if (myClientId == Constants.DEFAULT_CLIENT_ID) {
                    myClientId = p.getClientId();
                    myPlayer.setClientId(myClientId);
                    userList.put(myClientId, myPlayer);
                } else {
                    logger.warning("Receiving client id despite already being set");
                }
                events.onReceiveClientId(p.getClientId());
                break;
            case GET_ROOMS:
                RoomResultPayload rp = (RoomResultPayload) p;
                System.out.println("Received Room List:");
                if (rp.getMessage() != null) {
                    System.out.println(rp.getMessage());
                } else {
                    for (int i = 0, l = rp.getRooms().length; i < l; i++) {
                        System.out.println(String.format("%s) %s", (i + 1), rp.getRooms()[i]));
                    }
                }
                events.onReceiveRoomList(rp.getRooms(), rp.getMessage());
                break;
            case RESET_USER_LIST:
                userList.clear();
                events.onResetUserList();
                break;
            case READY:
                System.out.println(String.format("Player %s is ready", getClientNameById(p.getClientId())));
                break;
            case PHASE:
                System.out.println(String.format("The current phase is %s", p.getMessage()));
                break;
            case TURN:
                System.out.println(String.format("Current Player: %s", getClientNameById(p.getClientId())));
                break;
            default:
                logger.warning(String.format("Unhandled Payload type: %s", p.getPayloadType()));
                break;

        }
    }

    private void close() {
        myClientId = Constants.DEFAULT_CLIENT_ID;
        userList.clear();
        try {
            inputThread.interrupt();
        } catch (Exception e) {
            System.out.println("Error interrupting input");
            e.printStackTrace();
        }
        try {
            fromServerThread.interrupt();
        } catch (Exception e) {
            System.out.println("Error interrupting listener");
            e.printStackTrace();
        }
        try {
            System.out.println("Closing output stream");
            out.close();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened so this exception is ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Closing input stream");
            in.close();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened so this exception is ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Closing connection");
            server.close();
            System.out.println("Closed socket");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened so this exception is ok");
        }
    }
}