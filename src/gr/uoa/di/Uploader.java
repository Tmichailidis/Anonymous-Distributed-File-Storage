package gr.uoa.di;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Random;
import java.util.UUID;

import static gr.uoa.di.Client.uploadFile;
import static gr.uoa.di.Util.*;

public class Uploader {

    private static int nodePortNum;
    private static ClientFileManager fileManager;
    private static String nodeIP;


    public static void main(String[] args) throws Exception {
        nodeIP = getInternalIP();
        System.out.println("Internal ip " + nodeIP);
        Random rand = new Random();
        nodePortNum = Integer.parseInt(args[0]);
        String subDirName = nodeIP.replace(".", "") + nodePortNum;
        fileManager = new ClientFileManager(subDirName);

        int filesno = 10;
        System.out.println("Going to upload sum stuff");
        for (int i = 0; i < filesno; i++) {
            uploadFile("../../../../stress.py", "stress.py", "testDescriptiveString");
        }
    }

}
