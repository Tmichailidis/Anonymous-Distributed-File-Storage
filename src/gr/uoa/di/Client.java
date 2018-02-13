package gr.uoa.di;

import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

import static gr.uoa.di.Util.*;

public class Client {

    private static int nodePortNum;
    private static ClientFileManager fileManager;
    private static String nodeIP, subDirName;

    static void addNeighbor(String friendIP, int friendPortNum) throws NoSuchAlgorithmException, IOException {
        Socket socket;
        InputStream socketInputStream;
        OutputStream socketOutputStream;
        String pseudoKey, command, uniqueID;

        socket = null;
        try {
            socket = new Socket(nodeIP, nodePortNum);
        } catch (ConnectException e){
            System.out.println("Unreachable node. Exiting.");
            System.exit(1);
        }
        socketInputStream = socket.getInputStream();
        socketOutputStream = socket.getOutputStream();

        uniqueID = UUID.randomUUID().toString();
        pseudoKey = Crypto.generateCryptoRandomSHA256();
        command = "addNeighbor " + pseudoKey + " " + uniqueID + " " + friendIP + " " + friendPortNum;
        sendCommandBlocking(command, socketInputStream, socketOutputStream, 1);
        System.out.println("Closing socket " + socket.getLocalAddress() + " " + socket.getLocalPort());
        socket.close();
    }

    static String getNodeIP(){
        return nodeIP;
    }

    static int getNodePortNum(){
        return nodePortNum;
    }

    static String getNodeSubDirName(){
        return subDirName;
    }

    static void downloadFile(String fileKey, String fileName, String descriptiveString,
                             String encodedPK, String digitalSignature) throws IOException, InvalidKeySpecException,
            NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchPaddingException {
        Socket socket;
        long fileSize;
        String command;
        int hopsToLive;
        String response, uniqueID;
        InputStream socketInputStream;
        OutputStream socketOutputStream;

        socket = null;
        try {
            socket = new Socket(nodeIP, nodePortNum);
        } catch (ConnectException e){
            System.out.println("Unreachable node. Exiting.");
            System.exit(1);
        }
        socketInputStream = socket.getInputStream();
        socketOutputStream = socket.getOutputStream();

        hopsToLive = 5;
        uniqueID = UUID.randomUUID().toString();
        command = "download " + nodeIP + " " + nodePortNum + " "
                + uniqueID + " " + fileKey + " " + hopsToLive;
        response = sendCommandBlocking(command, socketInputStream, socketOutputStream, 1);
        System.out.println("Response " + response);
        if (!response.equalsIgnoreCase("s")){
            System.out.println("Could not download file " + fileName);
            return;
        }
        fileSize = bytesToLong(readByteArrayFromInputStream(socketInputStream, 8));
        System.out.println("Filesize " + fileSize);

        CipherInputStream cipherInputStream =
                Crypto.initDecryption(Crypto.generateSHA256hash(descriptiveString), socketInputStream);
        fileManager.addToDownload(fileName, fileSize, cipherInputStream);
        System.out.println("downloaded successfully");
        if (!Crypto.verifySignature(encodedPK, digitalSignature, subDirName, fileName)) {
            System.out.println("file " + fileName + " not verified! Initializing self-destruction");
            fileManager.removeFromDownloads(fileName);
            return;
        }
        System.out.println(fileName + " verified successfully");
        System.out.println("Closing socket " + socket.getLocalAddress() + " " + socket.getLocalPort());
        socket.close();
    }

    static String uploadFile(String fullFilePath, String fileName, String descriptiveString) throws IOException,
            InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, SignatureException {
        Socket socket;
        long fileSize;
        int hopsToLive;
        File uploadedFile;
        String command, fileKey;
        String response, uniqueID;
        InputStream socketInputStream;
        OutputStream socketOutputStream;
        FileInputStream fileInputStream;
        SimpleEntry <KeyPair, String> infoSSK;
        SimpleEntry <String, String> infoDigitalSignature;

        socket = null;
        try {
            System.out.println("Trying to connect on upload");
            socket = new Socket(nodeIP, nodePortNum);
        } catch (ConnectException e){
            System.out.println("Unreachable node. Exiting.");
            System.exit(1);
        }
        socketInputStream = socket.getInputStream();
        socketOutputStream = socket.getOutputStream();

        hopsToLive = 5;
        uniqueID = UUID.randomUUID().toString();
        System.out.println("UploadFile start");
        do {
            Socket upSocket = new Socket(nodeIP, nodePortNum);
            InputStream upSocketInputStream = upSocket.getInputStream();
            OutputStream upSocketOutputStream = upSocket.getOutputStream();

            infoSSK = Crypto.createSSK(descriptiveString);
            fileKey = infoSSK.getValue();
            command = "upload " + nodeIP + " " + nodePortNum + " "
                    + uniqueID + " " + fileKey + " " + hopsToLive;
            response = sendCommandBlocking(command, upSocketInputStream, upSocketOutputStream, 1);
        }while(response.equals("e"));

        infoDigitalSignature = Crypto.signFile(infoSSK.getKey().getPrivate(), infoSSK.getKey().getPublic(), fullFilePath);

        command = "addFileKey " + fileKey + " " + uniqueID;
        sendCommandBlocking(command, socketInputStream, socketOutputStream, 1);

        uploadedFile = new File(fullFilePath);
        fileInputStream = new FileInputStream(uploadedFile);

        /*file size must be divisible by 16, due to the AES key */
        fileSize = (uploadedFile.length() % 16 == 0) ? uploadedFile.length() : (uploadedFile.length() / 16 + 1) * 16;
        System.out.println("FileSize from client interface = " + fileSize);
        socketOutputStream.write(longToBytes(fileSize), 0, 8);
        Crypto.sendEncrypted(Crypto.generateSHA256hash(descriptiveString), fileInputStream, socketOutputStream);
        System.out.println("UploadFile end");

        fileManager.addFileInfo(fileKey, fileName, descriptiveString,
                infoDigitalSignature.getKey(), infoDigitalSignature.getValue());
        System.out.println("Signing complete");
        socket.close();
        return fileKey;
    }

    static String[] getFileInfo(String fileKey) throws IOException {
        return fileManager.retrieveFileInfo(fileKey);
    }

    static void nodeShutdown() throws IOException {
        Socket socket;
        String command;
        InputStream socketInputStream;
        OutputStream socketOutputStream;

        socket = null;
        try {
            socket = new Socket(nodeIP, nodePortNum);
        } catch (ConnectException e){
            System.out.println("Unreachable node. Exiting.");
            System.exit(1);
        }
        socketInputStream = socket.getInputStream();
        socketOutputStream = socket.getOutputStream();

        command = "shutdown";
        sendCommandBlocking(command, socketInputStream, socketOutputStream, 0);
        System.out.println("Shutdown done successfully");
        socket.close();
    }

    static HashMap <String, String> getAllFileInfo() throws IOException {
        return fileManager.getFileInfoKeysNames();
    }

    static void connectToNode(String IP, int portNum) throws IOException, NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        nodeIP = IP;
        nodePortNum = portNum;
        subDirName = nodeIP.replace(".", "") + nodePortNum;
        fileManager = new ClientFileManager(subDirName);
    }

    public static void main(String[] args) throws Exception {
        nodeIP = "192.168.1.108";
        nodePortNum = 56631;
        subDirName = nodeIP.replace(".", "") + nodePortNum;
        fileManager = new ClientFileManager(subDirName);
        System.out.println(fileManager.getFileInfoKeysNames());
        Scanner input = new Scanner(System.in);
    }

}
