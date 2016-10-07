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

		readPeerId(args);
		readCommonCfg();
		readPeerCfg();
		//checkFields(); //testing only\\
		//TODO: Build TCP connection
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
		System.out.println(peerId);

		//Common fields
		System.out.println(numberOfPreferredNeighbors); 
		System.out.println(unchokingInterval);
		System.out.println(optimisticUnchokingInterval);
		System.out.println(fileName);
		System.out.println(fileSize);
		System.out.println(pieceSize);

		//Peer fields 
		for (int i = 0; i < 6; i++) {
			System.out.println(peerIds[i] + " " + hostNames[i] + 
				" " + portNumbers[i] + " " + (hasFile[i] ? 1 : 0));
		}
	}
}