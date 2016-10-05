/**************************************

Peer Process Class
Handles Main Logical Course of Program
Inputs: 4-digit PeerId

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

	public static void main (String[] args) {

		readPeerId(args);
	}

	public static void readPeerId(String[] args) {
		try {
			peerId = Integer.parseInt(args[0]);

		} catch (Exception e) {
			System.out.println("Error: Please input Peer Id");
			System.exit(0);
		}
	} 
}