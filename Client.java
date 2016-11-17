import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;
import java.math.BigInteger;


public class Client {
    static Socket requestSocket[];           //socket connect to the server
    static ObjectOutputStream out[];         //stream write to the socket
    static ObjectInputStream in[];          //stream read from the socket
    static String message;                //message send to the server
    static String MESSAGE;                //capitalized message read from the server

    static int peerId;
    static int ownIndex;
    static int fileSize;
    static int pieceSize;
    //static boolean[] madeConnection;
    static int[] peerIds;
    static Logger logger;
    static FileHandler fileHandler;

    static int[] clientIDToPeerID; // Holds the conversion for converting a client ID to peer ids

    public static Neighbor[] neighbors;
    public static byte[] bitfield;
    public int numberOfBits;
    public int numberOfBytes;

    static ServerListener serverListener;

    public Client(int peerId, int[] peerIds, String[] hostNames, int[] portNumbers, boolean[] hasFile, int fileSize, int pieceSize) {
        this.peerId = peerId;
        this.peerIds = peerIds;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;

        ownIndex = Arrays.binarySearch(peerIds, peerId);
        //Initialize bitmap to the file size divided by the piece size and then add one because we cast it to an int
        //Which floors the value
        numberOfBits = (int)(fileSize/pieceSize + 1);
        numberOfBytes = (int)(numberOfBits/8 + 1);
        bitfield = new byte[numberOfBytes];

        //Set all of the bits to true if we have the file:
        if (hasFile[ownIndex]) {
            //To set all the bits we make an integer that has all the bits set, then turn it into a byte[]
            BigInteger bigInt = new BigInteger("0");
            for (int i = 0; i < numberOfBits; i++) {
                bigInt = bigInt.setBit(i);
                System.out.println(bigInt.toString());
            }
            byte[] array = bigInt.toByteArray();
            //Here we handle the case where the sign carries over since BigInt is signed:
            if (array[0] == 0) {
                array = Arrays.copyOfRange(array, 1, array.length);
            }
            bitfield = array;
        }

        //Initialize our neighbor data:
        neighbors = new Neighbor[peerIds.length];
        clientIDToPeerID = new int[peerIds.length];

        for (int i = 0; i < peerIds.length; i++) {
            //init to -1 so we know which ones arent being used
            clientIDToPeerID[i] = -1;
            //Init neighbors
            neighbors[i].peerId = peerIds[i];
            neighbors[i].hostName = hostNames[i];
            neighbors[i].portNumber = portNumbers[i];
            neighbors[i].hasFile = hasFile[i];
        }


        requestSocket = new Socket[peerIds.length];
        out = new ObjectOutputStream[peerIds.length];
        //in = new ObjectInputStream[peerIds.length];

        
        //madeConnection[ownIndex] = true;

        //Set up for local logging (this process)
        prepareLogging();

        run();
    }

    private static void run() {
        try {
            //Starts our server listener which will create multiple sockets based on how many clients connect
            serverListener = new ServerListener(peerIds[ownIndex], neighbors[ownIndex].hostName, neighbors[ownIndex].portNumber);

            
            //This checks for connections to the other clients
            initConnections();

            logger.info("All connections have been established.");

            //initialize outputStreams
            for (int i = 0; i < peerIds.length; i++) {
                if (i != ownIndex && neighbors[i].madeConnection) {       
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
                    if (!neighbors[i].hasHandshakeSent && neighbors[i].madeConnection) {
                        //Send the bitfield
                        sendHandshake(i);
                        neighbors[i].hasHandshakeSent = true;
                        logger.info("Sent handshake to host " + neighbors[i].hostName + " on port " + neighbors[i].portNumber);
                    }
                    //if we havent sent the bitfield and we are connected and we have received the handshake:
                    if (!neighbors[i].hasBitfieldSent && neighbors[i].madeConnection && neighbors[i].hasHandshakeReceived) {
                        //Send the bitfield
                        sendBitfield(i);
                        neighbors[i].hasBitfieldSent = true;
                        logger.info("Sent bitfield to host " + neighbors[i].hostName + " on port " + neighbors[i].portNumber);
                    }
                }
            }

            //Now check if we have received messages from any clients (Synchronized, thread safe):
            synchronized (serverListener.receivedMessages) {
                //loop thru all of the received messages
                for (int j = 0; j < serverListener.receivedMessages.size(); j++) {
                   Message incomingMessage = (Message)serverListener.receivedMessages.get(j);
                    int localIndex = clientIDToPeerID[incomingMessage.clientID];


                    if (localIndex == -1 && !(incomingMessage.type == (byte)Message.handshake)) {
                        //We haven't received the handshake yet so skip to next iteration:
                        continue;
                    }

                   //Using a switch statement base on which type this message is:
                   switch (incomingMessage.type) {
                    case (byte)Message.handshake:
                    {
                        //it is a handshake:
                        //Length holds the peer ID in a handshake message:
                        clientIDToPeerID[incomingMessage.clientID] = incomingMessage.length;
                        //We have received the handshake lets set it and print it out
                        neighbors[localIndex].hasHandshakeReceived = true;
                        logger.info("Received handshake from host:" + neighbors[localIndex].hostName + " on port:" + neighbors[localIndex].portNumber);
                        break;
                    }
                    /*
                     * Here is the case in which we receive a bitfield:
                     */
                    case (byte)Message.bitfield:
                    {
                        //it is a bitfield:
                        neighbors[localIndex].bitmap = incomingMessage.payload;
                        neighbors[localIndex].hasBitfieldReceived = true;

                        //Check to see if we need to send interested or uninterested to the sender
                        //As per project requirement
                        if (neighbors[localIndex].hasFile) {}

                        logger.info("Received bitfield from host:" + neighbors[localIndex].hostName + " on port:" + neighbors[localIndex].portNumber);
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
        Message bitfieldMessage = new Message(2,(byte)5, bitfield);
        //System.out.println("sending bitfield: " + bitfield.toString());

        sendMessage(bitfieldMessage.getMessageBytes(), index);
    }

    private static void initConnections() {
        int connectionsLeft = peerIds.length-1;
            while (connectionsLeft > 0) {
                System.out.println();

                for (int i = 0; i < peerIds.length; i++) {
                    if (i != ownIndex && !neighbors[i].madeConnection) {
                        logger.info("Attempting to connect to " + neighbors[i].hostName + 
                            " on port " + neighbors[i].portNumber);
                        try {
                            //create a socket to connect to the server
                            requestSocket[i] = new Socket(neighbors[i].hostName, neighbors[i].portNumber);

                            if (requestSocket[i].isConnected()) {
                            logger.info("Connected to " + neighbors[i].hostName + 
                                " in port " + neighbors[i].portNumber); 
                            connectionsLeft--;
                            neighbors[i].madeConnection = true;
                            //madeConnection[i] = true;
                            }
                        } catch (ConnectException e) {
                            neighbors[i].connectionRefused = true;
                        } catch (IOException e) {
                            neighbors[i].connectionRefused = true;
                        }
                    }
                }
                System.out.println();
                for (int i = 0; i < peerIds.length; i++) {
                    if (neighbors[i].connectionRefused) {
                        logger.info("Connection refused for " + neighbors[i].hostName + 
                            " on port " + neighbors[i].portNumber);
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


