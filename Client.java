/* To compile, run the following command:
 * javac Client.java Message.java Neighbor.java ServerListener.java PeerProcess.java
 */

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
    static boolean[] hasFile;
    //static boolean[] madeConnection;
    static int[] peerIds;
    static Logger logger;
    static FileHandler fileHandler;
    static byte[][] filePieces;
    static String fileName;
	static int requestedPieceNumber = 5; //TODO: initialize this in the sendRequest()

    static int[] clientIDToPeerID; // Holds the conversion for converting a client ID to peer ids

    public static Neighbor[] neighbors;
    public static byte[] bitfield;
    public int numberOfBits;
    public int numberOfBytes;
    
    static ServerListener serverListener;
    static int totalPieces = 0;

    public Client(int peerId, int[] peerIds, String[] hostNames, int[] portNumbers, boolean[] hasFile, int fileSize, int pieceSize, String fileName) {
        this.peerId = peerId;
        this.peerIds = peerIds;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        this.fileName = fileName;
        this.hasFile = hasFile;

        ownIndex = Arrays.binarySearch(peerIds, peerId);
        //Initialize bitmap to the file size divided by the piece size and then add one because we cast it to an int
        //Which floors the value
        numberOfBits = (int)(fileSize/pieceSize + 1);
        numberOfBytes = (int)(numberOfBits/8 + 1);
        bitfield = new byte[numberOfBytes];

        //Set up for local logging (this process)
        prepareLogging();

        //Set all of the bits to true if we have the file:
        if (hasFile[ownIndex]) {
            //To set all the bits we make an integer that has all the bits set, then turn it into a byte[]
            BigInteger bigInt = new BigInteger("0");
            for (int i = 0; i < numberOfBits; i++) {
                bigInt = bigInt.setBit(i);
            }
            byte[] array = bigInt.toByteArray();
            //Here we handle the case where the sign carries over since BigInt is signed:
            if (array[0] == 0) {
                array = Arrays.copyOfRange(array, 1, array.length);
            }
            bitfield = array;

         }

        initializeFilePieces(hasFile[ownIndex]);

        //Initialize our neighbor data:
        neighbors = new Neighbor[peerIds.length];
        clientIDToPeerID = new int[peerIds.length];

        for (int i = 0; i < peerIds.length; i++) {
            //init to -1 so we know which ones arent being used
            clientIDToPeerID[i] = -1;
            //Init neighbors
            neighbors[i] = new Neighbor();
            neighbors[i].peerId = peerIds[i];
            neighbors[i].hostName = hostNames[i];
            neighbors[i].portNumber = portNumbers[i];
            neighbors[i].hasFile = hasFile[i];
        }


        requestSocket = new Socket[peerIds.length];
        out = new ObjectOutputStream[peerIds.length];
        //in = new ObjectInputStream[peerIds.length];

        
        //madeConnection[ownIndex] = true;

        run();
    }

    private static void run() {
        try {
            //Starts our server listener which will create multiple sockets based on how many clients connect
            serverListener = new ServerListener(peerIds[ownIndex], neighbors[ownIndex].hostName, neighbors[ownIndex].portNumber);

            
            //This checks for connections to the other clients
            initConnections();

            logger.info("All connections have been established.");
            logger.info("" + '\n');
            //initialize outputStreams
            for (int i = 0; i < peerIds.length; i++) {
                if (i != ownIndex && neighbors[i].madeConnection) {       
                    out[i] = new ObjectOutputStream(requestSocket[i].getOutputStream());
                    out[i].flush();
                }
            }
            int iteration = 0;
            //Main loop that checks if we need to send or receive messages
            while (true) {

            	//Send Message Test
	           	if (iteration++ == 1) {
            		try{ Thread.sleep(1000); } catch(Exception e){}
            		if (hasFile[ownIndex]) {
            			System.out.println("Own index:" + ownIndex);
            			sendFilePiece(((ownIndex+1)%2), 5);
        			}
            	}


            //This is where we check to see if we need to send any messages:
            for (int i = 0; i < peerIds.length; i++) {
                if (i != ownIndex) {
                    //if we havent sent the bitfield and we are connected:
                    if (!neighbors[i].hasHandshakeSent && neighbors[i].madeConnection) {
                        //Send the bitfield
                        sendHandshake(i);
                        neighbors[i].hasHandshakeSent = true;
                        logger.info("Sent handshake to host " + neighbors[i].hostName + " on port " + neighbors[i].portNumber + '\n');
                    }
                    //if we havent sent the bitfield and we are connected and we have received the handshake:
                    if (!neighbors[i].hasBitfieldSent && neighbors[i].madeConnection && neighbors[i].hasHandshakeReceived) {
                        //Send the bitfield
                        sendBitfield(i);
                        neighbors[i].hasBitfieldSent = true;
                        logger.info("Sent bitfield to host " + neighbors[i].hostName + " on port " + neighbors[i].portNumber + '\n');
                    }
                }
            }

            //Now check if we have received messages from any clients (Synchronized, thread safe):
            synchronized (serverListener.receivedMessages) {
                //loop thru all of the received messages
				//logger.info("Size: " + serverListener.receivedMessages.size());
	            
                for (int j = 0; j < serverListener.receivedMessages.size(); j++) {
                    Message incomingMessage = (Message)serverListener.receivedMessages.get(j);
                    //This gets the peerID of the incoming message, we have to do it like this because
                   //this server has to know which client the message is coming from, and it's inside the message.
                    int messageIndex = clientIDToPeerID[incomingMessage.clientID];

                    if (messageIndex == -1 && !((int)incomingMessage.type == Message.handshake)) {
                        //We haven't received the handshake yet so skip to next iteration:
                        continue;
                    }

                    if ((int)incomingMessage.type != Message.handshake) {
	                    for (int i = 0; i < peerIds.length; i++) {
	                       		if (peerIds[i] == messageIndex) {
	                       			messageIndex = i;
	                       			break;
	               			}
	                   	}
                    }
                   //Using a switch statement base on which type this message is:
                   	switch ((int)incomingMessage.type) {
                    	case Message.handshake:
	                    {
	                        //it is a handshake:
	                        //Length holds the peer ID in a handshake message:
	                        clientIDToPeerID[incomingMessage.clientID] = incomingMessage.length;
	                       	
							for (int i = 0; i < peerIds.length; i++) {
		                       		if (peerIds[i] == incomingMessage.length) {
		                       			messageIndex = i;
		                       			break;
		                       		}
		                   	}
	                       	//We have received the handshake lets set it and print it out
	                        neighbors[messageIndex].hasHandshakeReceived = true;
	                        logger.info("Received handshake from host:" + neighbors[messageIndex].hostName + " on port:" + neighbors[messageIndex].portNumber + '\n');
	                        break;
	                    }
	                    /*
	                     * Here is the case in which we receive a bitfield:
	                     */
	                    case Message.bitfield:
	                    {
	                        //it is a bitfield:
	                        neighbors[messageIndex].bitmap = incomingMessage.payload;
	                        neighbors[messageIndex].hasBitfieldReceived = true;

	                        logger.info("Received bitfield from host:" + neighbors[messageIndex].hostName + " on port:" + neighbors[messageIndex].portNumber + '\n');
	                        //Check to see if we need to send interested or uninterested to the sender
	                        //As per project requirement
	                        if (checkIfNeedPieces(neighbors[messageIndex])) {
	                            //This person has some pieces that we are interested in
	                            //Send that we are interested
	                            sendInterested(messageIndex);
	                        } else {
	                            //Send that we are not interested currently:
	                            sendNotInterested(messageIndex);
	                        }

	                        
	                        break;
	                    }
	                    //Message type is interested
	                    case Message.interested:
	                    {
	                        neighbors[messageIndex].interested = true;
	                        logger.info("Peer " + peerIds[ownIndex] + " received the 'interested' message from " + neighbors[messageIndex].peerId + '\n');
	                        break;
	                    }


	                    //Message type is not interested
	                    case Message.not_interested:
	                    {
	                        neighbors[messageIndex].interested = false;
	                        logger.info("Peer " + peerIds[ownIndex] + " received the 'not interested' message from " + neighbors[messageIndex].peerId + '\n');
	                        break;
	                    }

	                    case Message.have:
	                    {
	                    	ByteBuffer buffer = ByteBuffer.wrap(incomingMessage.payload);

	                    	logger.info("Peer " + peerIds[ownIndex] + " received the 'have' message from " + neighbors[messageIndex].peerId + 
	                    		" for the piece " + buffer.getInt() + "." +'\n');
	                        break;
	                    }

	                    //Message type is file piece
	                    case Message.piece:
	                    {
	                    	filePieces[requestedPieceNumber] = incomingMessage.payload;

	                    	BigInteger tempField = new BigInteger(bitfield);
	                    	tempField.setBit(requestedPieceNumber); //update with most recently requested number
	                    	bitfield = tempField.toByteArray();
	                    	
	                    	logger.info("Peer " + peerIds[ownIndex] + " has downloaded the piece " + requestedPieceNumber 
	                    		+ " from " + neighbors[messageIndex].peerId + ". Now the number of pieces it has is " + (++totalPieces) + "." + '\n');
	                    	
	                    	for (int i = 0; i < peerIds.length; i++) {
	                    		if (i == ownIndex)
	                    			continue;
	                			sendHave(i, requestedPieceNumber);
	                    	}
	                    	
							//TODO: Request logic
							//sendRequest();
	                    	break;
	                    }

	                    //Default statement to catch errors:
	                    default:
	                    System.out.println("Error unknown type: " + (int)incomingMessage.type);
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
            System.err.print("IOException:");	
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
        Message bitfieldMessage = new Message(bitfield.length, (byte)Message.bitfield, bitfield);
        //System.out.println("sending bitfield: " + bitfield.toString());

        sendMessage(bitfieldMessage.getMessageBytes(), index);
    }

    private static void sendInterested(int index) {
        //send an interested message to a given index
        Message message = new Message(0,(byte)Message.interested, null);
        //System.out.println("sending bitfield: " + bitfield.toString());
        sendMessage(message.getMessageBytes(), index);
    }

    private static void sendNotInterested(int index) {
        //send a not interested message to a given index
        Message message = new Message(0,(byte)Message.not_interested, null);
        //System.out.println("sending bitfield: " + bitfield.toString());
        sendMessage(message.getMessageBytes(), index);
    }

    private static void sendHave(int index, int pieceNumber) {
    	//send a have message to a given inde
    	byte[] pieceIndex = ByteBuffer.allocate(4).putInt(pieceNumber).array();
    	Message message = new Message(4,(byte)Message.have,pieceIndex);

		logger.info("Sending have " + pieceNumber + " to server " + index + '\n');

    	sendMessage(message.getMessageBytes(), index);
    }

   	private static void sendFilePiece(int index, int pieceNumber) {
		//Send file piece to a given server    		
   		Message message = new Message(pieceSize, (byte)Message.piece, filePieces[pieceNumber]);

   		sendMessage(message.getMessageBytes(), index); 
    }

    private static boolean checkIfNeedPieces(Neighbor neighbor) {
        BigInteger incomingBitfieldInt = new BigInteger(neighbor.bitmap);
        BigInteger selfBitfieldInt = new BigInteger(bitfield);

        //Check the bits of the bitfield to see if the incoming bitfield has any bits that we don't
        //Example:
        //00000010 (own bitfield)
        //00001111 (incoming bitfield)
        //AND =
        //00000010
        //NOT = 
        //11111101
        //00001111 (Now we And it with the incoming bitfield again)
        //AND =
        //00001101 We should be left with the bits that we dont have
        //If it is greater than 0 then we need pieces from the sender:
        if (incomingBitfieldInt.and(selfBitfieldInt.and(incomingBitfieldInt).not()).doubleValue() > 0) {
            return true;
        }
        return false;
    }

    private static void initConnections() {
        int connectionsLeft = peerIds.length-1;
            while (connectionsLeft > 0) {
                System.out.println();

                for (int i = 0; i < peerIds.length; i++) {
                    if (i != ownIndex && !neighbors[i].madeConnection) {
                        logger.info("Attempting to connect to " + neighbors[i].hostName + 
                            " on port " + neighbors[i].portNumber + '\n');

                        try {
                            //create a socket to connect to the server
                            requestSocket[i] = new Socket(neighbors[i].hostName, neighbors[i].portNumber);

                            if (requestSocket[i].isConnected()) {
                            logger.info("Connected to " + neighbors[i].hostName + 
                                " in port " + neighbors[i].portNumber + '\n');

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
                            " on port " + neighbors[i].portNumber + '\n');

                    }
                }
                if (connectionsLeft > 0) {
                //Wait four seconds before attempting to reconnect
                    try {
                    logger.info("Waiting to reconnect.." + '\n');
                    Thread.sleep(3000);
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
        	System.err.println("Error sending message.");
            ioException.printStackTrace();
        }
    }

    private static void initializeFilePieces(boolean hasFile) {
    	filePieces = new byte[(fileSize/pieceSize)+1][pieceSize];
    	
    	if (!hasFile)
    		return;

    	try {
    	 	File file = new File(fileName);
    	 	FileInputStream inputStream = new FileInputStream(file);

            int currentPieceSize, currentPieceIndex;
            currentPieceIndex = currentPieceSize = 0;

            while (currentPieceIndex < filePieces.length) {
            	currentPieceSize = inputStream.read(filePieces[currentPieceIndex++]);
            }
        } catch (Exception e) {
        	logger.info("Error generating file pieces:");
        }

        //for (int i = 0; i < filePieces.length; i++) {
        //	System.out.println(i);
        //	logger.info("Bytes:" + Arrays.toString(filePieces[i]));
        //}
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


