package gr.uoa.di;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

import static gr.uoa.di.Util.*;

public class ServerThread extends Thread {
    private Socket socket;

    ServerThread(Socket clientSocket) {
        this.socket = clientSocket;
    }

    public void run() {
        String line;
        String []cmd;
        InputStream inpSocket;
        OutputStream outSocket;
        int cmdSize;
        try {
            inpSocket = socket.getInputStream();
            outSocket = socket.getOutputStream();
            /* command size will be always < 255, thus we read only one byte*/
            cmdSize = inpSocket.read();
            line = readStringFromInputStream(inpSocket, cmdSize);
            System.out.println("ServerThread command: " + line);
            cmd = line.split(" ");
            if (cmd[0].equalsIgnoreCase("quit")){
                System.out.println("Normal Quitting");
                socket.close();
            } else if (cmd[0].equalsIgnoreCase("upload")){
                assert cmd.length != 6 : "Error while checking consensus for file key";

                String inserterIP, UID, fileKey;
                int inserterPortNum, hopsToLive;

                inserterIP = cmd[1];
                inserterPortNum = Integer.parseInt(cmd[2]);
                UID = cmd[3];
                fileKey = cmd[4];
                hopsToLive = Integer.parseInt(cmd[5]);
                Node.checkFKConsensus(inserterIP, inserterPortNum, UID, fileKey, hopsToLive, outSocket);
                socket.close();
            } else if (cmd[0].equalsIgnoreCase("addNeighbor")){
                assert cmd.length != 5 : "Error while entering a neighbor";

                int portNum;
                String key, IP, UID;
                System.out.println("Addneighbor in serverthread command: " + line);
                key = cmd[1];
                UID = cmd[2];
                IP  = cmd[3];
                portNum = Integer.parseInt(cmd[4]);
                Node.addNeighbor(key, UID, IP, portNum);
                outSocket.write(0);       //ACK
                socket.close();
            } else if (cmd[0].equalsIgnoreCase("addFileKey")) {
                assert cmd.length != 3 : "Error while entering a file key";

                String fileKey, UID;

                fileKey = cmd[1];
                UID = cmd[2];
                outSocket.write(0);       //ACK
                Node.addFileKey(fileKey, UID, inpSocket);
                socket.close();
            } else if (cmd[0].equalsIgnoreCase("download")){
                assert cmd.length != 6 : "Error while trying to download a file";

                int requesterPortNum, hopsToLive;
                String requesterIP, UID, fileKey;

                requesterIP = cmd[1];
                requesterPortNum = Integer.parseInt(cmd[2]);
                UID = cmd[3];
                fileKey = cmd[4];
                hopsToLive = Integer.parseInt(cmd[5]);
                Node.downloadFile(requesterIP, requesterPortNum, UID, fileKey, hopsToLive, outSocket);
                socket.close();
            } else if (cmd[0].equalsIgnoreCase("join")){
                assert cmd.length != 4 : "Error while executing a node join in the system";

                int hopsToLive;
                String UID, hashSeed;

                hashSeed = cmd[1];
                UID = cmd[2];
                hopsToLive = Integer.parseInt(cmd[3]);
                Node.joinSystem(hashSeed, UID, hopsToLive, outSocket);
                socket.close();
            } else if (cmd[0].equalsIgnoreCase("shutdown")){
                Node.shutdown();
            } else{
                System.out.println("Serverthread: unknown command");
            }
        } catch (IOException | NoSuchAlgorithmException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Serverthread exit");
    }
}