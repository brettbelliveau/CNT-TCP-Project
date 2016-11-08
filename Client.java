import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;


public class Client {
    static Socket requestSocket[];           //socket connect to the server
    static ObjectOutputStream out[];         //stream write to the socket
    static ObjectInputStream in[];          //stream read from the socket
    static String message;                //message send to the server
    static String MESSAGE;                //capitalized message read from the server

    static int peerId;
    static int ownIndex;
    static boolean[] madeConnection;
    static int[] peerIds;
    static String[] hostNames;
    static int[] portNumbers;
    static boolean[] hasFile;
    static boolean[] hasHandshakeReceived;
    static boolean[] hasBitfieldReceived;
    static boolean[] hasHandshakeSent;
    static boolean[] hasBitfieldSent;
    static Logger logger;
    static FileHandler fileHandler;

    static int[] clientIDToPeerID; // Holds the conversion for converting a client ID to peer ids

    static ServerListener serverListener;

    public Client(int peerId, int[] peerIds, String[] hostNames, int[] portNumbers, boolean[] hasFile) {
        this.peerId = peerId;
        this.peerIds = peerIds;
        this.hostNames = hostNames;
        this.portNumbers = portNumbers;
        this.hasFile = hasFile; 

        madeConnection = new boolean[peerIds.length];

        hasHandshakeReceived = new boolean[peerIds.length];
        hasBitfieldReceived = new boolean[peerIds.length];
        hasHandshakeSent = new boolean[peerIds.length];
        hasBitfieldSent = new boolean[peerIds.length];

        //init to -1 so we know which ones arent being used
        clientIDToPeerID = new int[peerIds.length];
        for (int i = 0; i < peerIds.length; i++) {
            clientIDToPeerID[i] = -1;
        }


        requestSocket = new Socket[peerIds.length];
        out = new ObjectOutputStream[peerIds.length];
        //in = new ObjectInputStream[peerIds.length];

        ownIndex = Arrays.binarySearch(peerIds, peerId);
        madeConnection[ownIndex] = true;

        //Set up for local logging (this process)
        prepareLogging();

        run();
    }

    private static void run() {
        try {
            //Starts our server listener which will create multiple sockets based on how many clients connect
            serverListener = new ServerListener(peerIds[ownIndex], hostNames[ownIndex], portNumbers[ownIndex]);

            
            //This checks for connections to the other clients
            initConnections();

            logger.info("All connections have been established.");

            //initialize outputStreams
            for (int i = 0; i < peerIds.length; i++) {
                if (i != ownIndex && madeConnection[i]) {       
                    out[i] = new ObjectOutputStream(requestSocket[i].getOutputStream());
                    out[i].flush();
                    System.out.println(i);
                }
            }
            //Main loop that checks if we need to send or receive messages
            while (true) {

            //This is where we check to see if we need to send any messages:
            for (int i = 0; i < peerIds.length; i++) {
                if (i != ownIndex) {
                    //if we havent sent the bitfield and we are connected:
                    if (!hasHandshakeSent[i] && madeConnection[i]) {
                        //Send the bitfield
                        sendHandshake(i);
                        hasHandshakeSent[i] = true;
                        logger.info("Sent handshake to host " + hostNames[i] + " on port " + portNumbers[i]);
                    }
                    //if we havent sent the bitfield and we are connected:
                    if (!hasBitfieldSent[i] && madeConnection[i]) {
                        //Send the bitfield
                        sendBitfield(i);
                        hasBitfieldSent[i] = true;
                        logger.info("Sent bitfield to host " + hostNames[i] + " on port " + portNumbers[i]);
                    }
                }
            }

            //Now check if we have received messages from any clients (Synchronized, thread safe):
            synchronized (serverListener.receivedMessages) {
                //loop thru all of the received messages
                for (int j = 0; j < serverListener.receivedMessages.size(); j++) {
                   Message incomingMessage = (Message)serverListener.receivedMessages.get(j);
                   //Using a switch statement base on which type this message is:
                   switch (incomingMessage.type) {
                    case (byte)Message.handshake:
                    {
                            //it is a handshake:
                        //Length holds the peer ID in a handshake message:
                        clientIDToPeerID[incomingMessage.clientID] = incomingMessage.length;
                        int localIndex =  ownIndex;
                        //We have received the handshake lets set it and print it out
                        hasHandshakeReceived[localIndex] = true;
                        logger.info("Received handshake from host:" + hostNames[localIndex] + " on port:" + portNumbers[localIndex]);
                        break;
                    }
                    /*
                     * Here is the case in which we receive a bitfield:
                     */
                    case (byte)Message.bitfield:
                    {
                        //it is a bitfield:
                        //Get the local index
                        int localIndex = ownIndex;

                        if (localIndex == -1) {
                            //We haven't received the handshake yet so skip to next iteration:
                            continue;
                        }
                        hasBitfieldReceived[localIndex] = true;
                        logger.info("Received bitfield from host:" + hostNames[localIndex] + " on port:" + portNumbers[localIndex]);
                        break;
                    }

                    //Default statement to catch errors:
                    default:
                    System.out.println("Error unknown type.");
                    break;
                }
                //After we have parsed this message we are done with it, remove it:
                serverListener.receivedMessages.remove(j);
            }
        }

    }
}
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        //catch ( ClassNotFoundException e ) {
            //System.err.println("Class not found");
        //}
        catch(UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException) {
            ioException.printStackTrace();
        } 
        //catch (InterruptedException e) {
            //System.err.println("Interrupted thread execution.");
        //}
        finally {
            //Close connections
            try {
                for (int i = 0; i < peerIds.length; i++) {
                    if (i != ownIndex) {
                        out[i].close();
                        requestSocket[i].close();
                    }
                }
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }

    private static void sendHandshake(int index) {
        //create handshake message and send it to peers
            byte[] handshakeHeader = new byte[18];
            try {
            handshakeHeader = "P2PFILESHARINGPROJ".getBytes("UTF-8");
            } catch (Exception e) {

            }
            
            byte[] zeroBits = new byte[10];
            byte[] peeridArray = ByteBuffer.allocate(4).putInt(peerId).array();

            ByteBuffer handshakeBuffer = ByteBuffer.allocate(32);
            
            handshakeBuffer.put(handshakeHeader);
            handshakeBuffer.put(zeroBits);
            handshakeBuffer.put(peeridArray);
            
            byte[] handshake = handshakeBuffer.array();


            sendMessage(handshake, index);
            handshakeBuffer.clear();
    }

    private static void sendBitfield(int index) {
        //create bitfield message and send it to peers
        byte[] bitfieldPayload = {(byte)1,(byte)6};
        Message bitfield = new Message(2,(byte)5, bitfieldPayload);
        //System.out.println("sending bitfield: " + bitfield.toString());

        sendMessage(bitfield.getMessageBytes(), index);
    }

    private static void initConnections() {
        int connectionsLeft = peerIds.length-1;
            while (connectionsLeft > 0) {
                System.out.println();
                boolean connectionRefused[] = new boolean[peerIds.length];

                for (int i = 0; i < peerIds.length; i++) {
                    if (i != ownIndex && !madeConnection[i]) {
                        logger.info("Attempting to connect to " + hostNames[i] + 
                            " on port " + portNumbers[i]);
                        try {
                            //create a socket to connect to the server
                            requestSocket[i] = new Socket(hostNames[i], portNumbers[i]);

                            if (requestSocket[i].isConnected()) {
                            logger.info("Connected to " + hostNames[i] + 
                                " in port " + portNumbers[i]); 
                            connectionsLeft--;
                            madeConnection[i] = true;
                            }
                        } catch (ConnectException e) {
                            connectionRefused[i] = true;
                        } catch (IOException e) {
                            connectionRefused[i] = true;
                        }
                    }
                }
                System.out.println();
                for (int i = 0; i < peerIds.length; i++) {
                    if (connectionRefused[i]) {
                        logger.info("Connection refused for " + hostNames[i] + 
                            " on port " + portNumbers[i]);
                    }
                }
                if (connectionsLeft > 0) {
                //Wait four seconds before attempting to reconnect
                    try {
                    System.out.println();
                    System.out.print("Waiting to reconnect");
                    System.out.print(".");
                    Thread.sleep(1000);
                    System.out.print(".");
                    Thread.sleep(1000);
                    System.out.print(".");
                    Thread.sleep(1000);
                    System.out.println();
                    } catch (Exception e) {

                    }
                }
            }
    }

    //send a message to the output stream
    private static void sendMessage(byte[] msg, int socketIndex)
    {
        try {
            //stream write the message
            out[socketIndex].writeObject(msg);
            out[socketIndex].flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    public static void prepareLogging() {
        logger = Logger.getLogger(PeerProcess.class.getName());
        
        try {
            String workingDir = System.getProperty("user.dir");
            
            fileHandler = new FileHandler(workingDir + "\\" + "log_peer_" + peerId + ".log");
            logger.addHandler(fileHandler);
            
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            
        } catch (Exception e) {
            e.printStackTrace();   
        }
    }
}
