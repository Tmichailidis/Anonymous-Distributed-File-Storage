package gr.uoa.di;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.UUID;

import static gr.uoa.di.Util.*;

public class NodeHandler {

    private static Node node;

    private static void joinSystem(String IP, int portNum)
            throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, SignatureException {
        Socket socket;
        String command;
        int hopsToLive;
        InputStream socketInputStream;
        OutputStream socketOutputStream;
        String response, uniqueID, pseudoKey, seed;

        socket = new Socket(node.getIP(), node.getPort());
        socketInputStream = socket.getInputStream();
        socketOutputStream = socket.getOutputStream();

        /* add new neighbor by using a pseudokey */
        seed = Crypto.generateCryptoRandomSHA256();
        uniqueID = UUID.randomUUID().toString();
        pseudoKey = Crypto.generateCryptoRandomSHA256();
        command = "addNeighbor " + pseudoKey + " " + uniqueID + " " + IP + " " + portNum;
        sendCommandBlocking(command, socketInputStream, socketOutputStream, 1);

        socket = new Socket(node.getIP(), node.getPort());
        socketInputStream = socket.getInputStream();
        socketOutputStream = socket.getOutputStream();

        /* initiate system join procedure */
        hopsToLive = 5;
        command = "join " + Crypto.generateSHA256hash(seed) + " " + uniqueID + " " + hopsToLive;
        response = sendCommandBlocking(command, socketInputStream, socketOutputStream,65);
        System.out.println(response);

        socket = new Socket(node.getIP(), node.getPort());
        socketInputStream = socket.getInputStream();
        socketOutputStream = socket.getOutputStream();

        /* now we have to add this node to the other nodes' routing tables */
        command = "addNeighbor " + response.substring(1) + " "
                + uniqueID + " " + node.getIP() + " " + node.getPort();
        sendCommandBlocking(command, socketInputStream, socketOutputStream, 1);
        System.out.println("Closing socket " + socket.getLocalAddress() + " " + socket.getLocalPort());
        socket.close();
    }
    
    public static void main(String[] args) throws IOException, InterruptedException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        node = new Node(50, 0);
        String friendIP = getInternalIP(); //or externalIP
        int friendPortNum = 33201;
        System.out.println("Starting node in IP = " + node.getIP() + " and port = " + node.getPort());
        joinSystem(friendIP, friendPortNum);
        System.out.println("Main: Join Done");
    }
}
