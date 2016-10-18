/**************************************

Peer Process Class
Handles Main Logical Course of Program
Arguments: 4-digit PeerId

**************************************/

import java.lang.*;
import java.util.*;
import java.io.*;

public class PeerProcess {

	static int peerId;

	//Common configuration fields
	static int numberOfPreferredNeighbors; 
	static int unchokingInterval; 
	static int optimisticUnchokingInterval; 
	static String fileName; 
	static int fileSize; 
	static int pieceSize;

	//Peer configuration fields
	static int[] peerIds;
	static String[] hostNames;
	static int[] portNumbers;
	static boolean[] hasFile;

	public static void main(String[] args) {

		//Check for peerId input
		readPeerId(args);
		
		//Check for/consome common config file
		readCommonCfg();
		
		//Check for/consome peer config file
		readPeerCfg();
		
		//Create directories for peer processes
		createPeerDirs();
		
		//Set up for local logging (this process)
		//prepareLogging();
		
		//Print input fields (for debugging)
		//checkFields();
		
		//Initialize server
		Server server = new Server(peerId, peerIds, hostNames, portNumbers, hasFile);
		
		//Initialize client
		Client client = new Client(peerId, peerIds, hostNames, portNumbers, hasFile);
	}

	public static void readPeerId(String[] args) {
		try {
			peerId = Integer.parseInt(args[0]);

		} catch (Exception e) {
			System.out.println("Error: Please input Peer Id");
			System.exit(0);
		}
	} 

	public static void readCommonCfg() {
		BufferedReader reader;
		
		try {
			File file = new File("Common.cfg");
			String line;
			reader = new BufferedReader(new FileReader(file));

			while ((line = reader.readLine()) != null) {
			
				if (line.contains("NumberOfPreferredNeighbors "))
					numberOfPreferredNeighbors = Integer.parseInt((
						line.split("NumberOfPreferredNeighbors ")[1]));

				else if (line.contains("OptimisticUnchokingInterval "))
					optimisticUnchokingInterval = Integer.parseInt((
						line.split("OptimisticUnchokingInterval ")[1]));

				else if (line.contains("UnchokingInterval "))
					unchokingInterval = Integer.parseInt((
						line.split("UnchokingInterval ")[1]));

				else if (line.contains("FileName "))
					fileName = line.split("FileName ")[1];

				else if (line.contains("FileSize "))
					fileSize = Integer.parseInt((
						line.split("FileSize ")[1]));

				else if (line.contains("PieceSize "))
					pieceSize = Integer.parseInt((
						line.split("PieceSize ")[1]));
				else
					throw new Exception();
			}

		} catch (Exception e) {
			System.out.println("Error reading common configuration file");
			System.exit(0);
		}

	}
	
	public static void readPeerCfg() {
		BufferedReader reader;
		Scanner sc;
		
		try {
			File file = new File("PeerInfo.cfg");
			String line;
			reader = new BufferedReader(new FileReader(file));
			peerIds = new int[6];
			hostNames = new String[6];
			portNumbers = new int[6];
			hasFile = new boolean[6];
			
			for (int i = 0; i < 6; i++) {
				
				if ((line = reader.readLine()) == null)
					throw new Exception();

				sc = new Scanner(line);
				peerIds[i] = sc.nextInt();
				hostNames[i] = sc.next();
				portNumbers[i] = sc.nextInt();
				hasFile[i] = (sc.nextInt() == 1);

			}

		} catch (Exception e) {
			System.out.println("Error reading peer configuration file");
			System.exit(0);
		}
	}

	public static void checkFields() {
		System.out.print("Peer Id:");
		System.out.println(peerId);
		System.out.println("Common Fields:");
		//Common fields
		System.out.println(numberOfPreferredNeighbors); 
		System.out.println(unchokingInterval);
		System.out.println(optimisticUnchokingInterval);
		System.out.println(fileName);
		System.out.println(fileSize);
		System.out.println(pieceSize);

		System.out.println("Peer Fields:");
		//Peer fields 
		for (int i = 0; i < 6; i++) {
			System.out.println(peerIds[i] + " " + hostNames[i] + 
				" " + portNumbers[i] + " " + (hasFile[i] ? 1 : 0));
		}
	}

	public static void createPeerDirs() {
		String workingDir = System.getProperty("user.dir");
		File file;
		for (int peerId : peerIds) {
			file = new File (workingDir + "\\" + "peer_" + peerId);
			if (!file.isDirectory()) {
				file.mkdir();
			}
		}
	}
}