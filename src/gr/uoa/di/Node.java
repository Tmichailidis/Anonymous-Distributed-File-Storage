package gr.uoa.di;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static gr.uoa.di.Util.*;

class Node {

    private static Thread server;
    private static String IP;
    private static int portNum;
    private static DataStore datastore;
    private static RoutingTable routingTable;
    private static ServerSocket serverSocket;
    private static TaskMaintenance taskMaintenance;
    private static Requests joinRequestsSet, fileRequestsSet, fileKeyAvailabilitySet;

    private static final long StorageCapacity = 16L * 1024L * 1024L * 1024L;

    Node(int routingTableSize, int portNumber) throws IOException, InterruptedException {
        String dirName;

        joinRequestsSet = new Requests();
        fileRequestsSet = new Requests();
        fileKeyAvailabilitySet = new Requests();
        serverSocket = new ServerSocket(portNumber);
        taskMaintenance = new TaskMaintenance();
        IP = getInternalIP();
//        IP = InetAddress.getLocalHost().getHostAddress();
        portNum = serverSocket.getLocalPort();
        routingTable = new RoutingTable(routingTableSize);
        dirName = IP.replace(".", "") + portNum;
        datastore = new DataStore(StorageCapacity, dirName);
        server = new Thread(() -> {
            try {
                Socket socket;
                serverSocket.setReuseAddress(true);
                System.out.println("Server up in port " + serverSocket.getLocalPort() + " " + serverSocket.getInetAddress());
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("Waiting for a new connection");
                    socket = serverSocket.accept();
                    System.out.println("A new connection has arrived");
                    Thread t = new ServerThread(socket);
                    taskMaintenance.addTaskThread(t);
                    t.start();
                }
            } catch (SocketException e){
                System.out.println("Shutting down server thread of node " + getIP() + ":" + getPort());
            } catch (IOException e){
                e.printStackTrace();
            }

        });
        server.start();
    }

    static void shutdown() throws IOException, InterruptedException {
        /* stop accepting new connections */
        server.interrupt();
        /* waiting for all tasks to complete */
        taskMaintenance.shutDown(Integer.toString(portNum));
        /* closing socket */
        serverSocket.close();
        /* fail safe: server thread should have ended after interrupt */
        server.join();
    }

    private static void sendCommand(String command, OutputStream outputStream) throws IOException {
        byte[] buf;

        buf = command.getBytes(StandardCharsets.UTF_8);
        outputStream.write(command.length());
        outputStream.write(buf, 0, command.length());
    }

    int getPort()
    {
        return portNum;
    }
    String getIP()
    {
        return IP;
    }

    private static boolean isAvailable(String FK){
        return datastore.isAvailable(FK) && routingTable.isAvailable(FK);
    }

    static void addNeighbor(String key, String UID, String neighborIP, Integer neighborPortNum) throws IOException {
        ArrayList <SimpleEntry <String, Integer>> nextNeighbors;

        if (!IP.equals(neighborIP) || portNum != neighborPortNum){
            routingTable.addKey(key, neighborIP, neighborPortNum);
        }

        nextNeighbors = joinRequestsSet.getValues(UID);
        if (nextNeighbors == null){
            return;
        }
        System.out.println(nextNeighbors + " ");
        for (SimpleEntry <String, Integer> neighbor: nextNeighbors){
            Socket socket;
            OutputStream outputStream;
            String command, nextIP;
            int timeout, nextPortNum;

            timeout = (int) TimeUnit.SECONDS.toMillis(10);
            socket = new Socket();
            nextIP = neighbor.getKey();
            nextPortNum = neighbor.getValue();
            try {
                System.out.println("Socket connect in add neighbor " + nextIP + " " + nextPortNum);
                socket.connect(new InetSocketAddress(nextIP, nextPortNum), timeout);
            } catch (SocketTimeoutException | ConnectException e) {
                System.out.println("AddNeighbor: " + nextIP + " " + nextPortNum + " " + e.toString());
                continue;
            }
            outputStream = socket.getOutputStream();
            command = "addNeighbor " + key + " " + UID + " " + neighborIP + " " + neighborPortNum;
            sendCommand(command, outputStream);
            joinRequestsSet.removeUid(UID);
        }
        routingTable.print();
    }

    static void addFileKey(String fileKey, String UID, InputStream inputStream) throws IOException {
        SimpleEntry <String, Integer> inserter;
        ArrayList <SimpleEntry <String, Integer>> nextNeighbors;

        nextNeighbors = fileKeyAvailabilitySet.getValues(UID);


        assert nextNeighbors.size() > 0;
        inserter = nextNeighbors.get(0);
        ArrayList<OutputStream> nextNeighborsOutputStreams = new ArrayList<>();

        System.out.println(nextNeighbors + " ");
        for (SimpleEntry <String, Integer> neighbor: nextNeighbors.subList(1, nextNeighbors.size())){
            Socket socket;
            OutputStream outputStream;
            InputStream dstInputStream;
            String command, neighborIP;
            int timeout, neighborPortNum;

            timeout = (int) TimeUnit.SECONDS.toMillis(10);
            socket = new Socket();
            neighborIP = neighbor.getKey();
            neighborPortNum = neighbor.getValue();
            try {
                System.out.println("Socket connect in addFileKey " + neighborIP + " " + neighborPortNum);
                socket.connect(new InetSocketAddress(neighborIP, neighborPortNum), timeout);
            } catch (SocketTimeoutException | ConnectException e) {
                System.out.println("AddFileKey: " + neighborIP + " " + neighborPortNum + " " + e.toString());
                continue;
            }
            outputStream = socket.getOutputStream();
            dstInputStream = socket.getInputStream();
            nextNeighborsOutputStreams.add(outputStream);
            command = "addFileKey " + fileKey + " " + UID;
            sendCommand(command, outputStream);
            dstInputStream.read();
        }
        fileKeyAvailabilitySet.removeUid(UID);
        datastore.saveFile(inputStream, nextNeighborsOutputStreams, fileKey);
        if (!inserter.equals(new SimpleEntry <>(IP, portNum))){
            routingTable.addKey(fileKey, inserter.getKey(), inserter.getValue());
        }
        routingTable.print();
    }

    static void downloadFile(String requesterIP, int requesterPortNum, String UID, String FK,
                                 int hopsToLive, OutputStream srcNodeOutputStream) throws NoSuchAlgorithmException {
        try{
            System.out.println("Start of downloadFile: " + hopsToLive);
            if (fileRequestsSet.existsUid(UID)){
                /* cycle; nothing changes */
                srcNodeOutputStream.write('c');
                System.out.println("CYCLE! Backtrack");
                return;
            }
            if (!isAvailable(FK)){
                int count;
                byte[] buffer;
                long fileSize;
                FileInputStream fileInputStream;
                /* Found the file: send 's' (success) */
                srcNodeOutputStream.write('s');
                /* send the file */
                fileSize = datastore.getFileSize(FK);
                fileInputStream = datastore.getFileInputStream(FK);
                srcNodeOutputStream.write(longToBytes(fileSize), 0, 8);
                buffer = new byte[8192];
                while ((count = fileInputStream.read(buffer)) > 0) {
                    srcNodeOutputStream.write(buffer, 0, count);
                }
                srcNodeOutputStream.close();
                System.out.println("The first node sent successfully the file");
                return;
            }
            if (hopsToLive == 0){
                srcNodeOutputStream.write('e');
                return;
            }

            fileRequestsSet.initUid(UID);
            fileRequestsSet.addUid(UID, new SimpleEntry<>(requesterIP, requesterPortNum));

            LinkedHashMap <String, SimpleEntry <String, Integer>> similarFK = routingTable.getKeysBySimilarity(FK);
            for (Map.Entry<String, SimpleEntry <String, Integer>> rtEntry : similarFK.entrySet()) {
                Socket dstNodeSocket;
                InputStream dstNodeInputStream;
                OutputStream dstNodeOutputStream;
                String dstIP, pseudoRequesterIP, command, fileKey;
                int dstPortNum, timeout, response, pseudoRequesterPortNum;

                fileKey = rtEntry.getKey();
                dstIP = rtEntry.getValue().getKey();
                dstPortNum = rtEntry.getValue().getValue();

                dstNodeSocket = new Socket();
                timeout = (int) TimeUnit.SECONDS.toMillis(5);

                try {
                    System.out.println("Socket connect in downloadFile " + dstIP + " " + dstPortNum);
                    dstNodeSocket.connect(new InetSocketAddress(dstIP, dstPortNum), timeout);
                }catch (SocketTimeoutException | ConnectException e){
                    System.out.println("DownloadFile: "
                            + dstIP + " " + dstPortNum + " " + e.toString());
                    routingTable.removeKey(fileKey);
                    continue;
                }
                dstNodeInputStream = dstNodeSocket.getInputStream();
                dstNodeOutputStream = dstNodeSocket.getOutputStream();

                if (Crypto.generateCryptoRandomInteger() % 2 == 0) {
                    pseudoRequesterIP = requesterIP;
                    pseudoRequesterPortNum = requesterPortNum;
                }
                else {
                    pseudoRequesterIP = IP;
                    pseudoRequesterPortNum = portNum;
                }

                /* send random source node */
                command = "download " + pseudoRequesterIP + " " +  pseudoRequesterPortNum + " "
                        + UID + " " + FK + " " + (hopsToLive - 1);
                System.out.println(command);
                sendCommand(command, dstNodeOutputStream);
                response = dstNodeInputStream.read();

                /* update routing table to maintain its LRU property */
                routingTable.update(fileKey);
                /* "response" is either 's' => in case of 's'uccessfully finding the file
                *                       'e' => hops to live 'e'xhausted
                *                       'c' => 'c'ycle
                *                       'u' => 'u'nsuccessful search (hops to live > 0)
                */
                if (response == 's'){
                    ArrayList<OutputStream> nextNeighborOutputStream = new ArrayList<>();

                    srcNodeOutputStream.write(response);
                    /* this arrayList contains the next neighbor to get the file
                    (the node that passed the request to the current node) */
                    nextNeighborOutputStream.add(srcNodeOutputStream);
                    datastore.saveFile(dstNodeInputStream, nextNeighborOutputStream, FK);
                    /* get the file and, concurrently, send it to the previous node*/
                    if (!(requesterIP.equalsIgnoreCase(IP) && requesterPortNum == portNum)) {
                        routingTable.addKey(FK, requesterIP, requesterPortNum);
                    }
                    System.out.println("Successful return from node " + IP + ":" + portNum);
                    fileRequestsSet.removeUid(UID);
                    routingTable.print();
                    return;
                }
                else if (response == 'e'){
                    System.out.println("File not found!");
                    srcNodeOutputStream.write(response);
                    return;
                }
                else if (response == 'c'){
                    System.out.println("DownloadFile: cycle - go to next neighbor");
                }
                else{
                    System.out.println("DownloadFile: unsuccessful search - go to next neighbor");
                }
                dstNodeSocket.close();
            }
            srcNodeOutputStream.write('u');
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    static void checkFKConsensus(String insIP, int insPortNum, String UID, String FK,
                                 int hopsToLive, OutputStream srcNodeOutputStream) throws NoSuchAlgorithmException {
        try{
            if (fileKeyAvailabilitySet.existsUid(UID)){
                /* cycle; nothing changes */
                srcNodeOutputStream.write('c');
                System.out.println("Cycle! go back");
                return;
            }
            if (!isAvailable(FK)){
                /* e: fk exists in this node */
                srcNodeOutputStream.write('e');
                return;
            }
            fileKeyAvailabilitySet.initUid(UID);
            fileKeyAvailabilitySet.addUid(UID, new SimpleEntry<>(insIP, insPortNum));

            if (hopsToLive == 0){
                srcNodeOutputStream.write(0);
                return;
            }

            LinkedHashMap <String, SimpleEntry <String, Integer>> similarFK = routingTable.getKeysBySimilarity(FK);
            for (Map.Entry<String, SimpleEntry <String, Integer>> rtEntry : similarFK.entrySet()) {
                Socket dstNodeSocket;
                InputStream dstNodeInputStream;
                OutputStream dstNodeOutputStream;
                String dstIP, command, pseudoInserterIP, fileKey;
                int dstPortNum, timeout,response, pseudoInserterPortNum;

                fileKey = rtEntry.getKey();
                dstIP = rtEntry.getValue().getKey();
                dstPortNum = rtEntry.getValue().getValue();

                dstNodeSocket = new Socket();
                timeout = (int) TimeUnit.SECONDS.toMillis(5);

                try {
                    System.out.println("Socket connect in checkFKconsensus " + dstIP + " " + dstPortNum);
                    dstNodeSocket.connect(new InetSocketAddress(dstIP, dstPortNum), timeout);
                }catch (SocketTimeoutException | ConnectException e){
                    System.out.println("CheckFKconsensus: "
                            + dstIP + " " + dstPortNum + " " + e.toString());
                    routingTable.removeKey(fileKey);
                    continue;
                }
                dstNodeInputStream = dstNodeSocket.getInputStream();
                dstNodeOutputStream = dstNodeSocket.getOutputStream();

                if (Crypto.generateCryptoRandomInteger() % 2 == 0) {
                    pseudoInserterIP = insIP;
                    pseudoInserterPortNum = insPortNum;
                }
                else {
                    pseudoInserterIP = IP;
                    pseudoInserterPortNum = portNum;
                }

                /* send random source node */
                command = "upload " + pseudoInserterIP + " " +  pseudoInserterPortNum + " "
                        + UID + " " + FK + " " + (hopsToLive - 1);
                System.out.println(command);
                sendCommand(command, dstNodeOutputStream);
                response = dstNodeInputStream.read();

                /* update routing table to maintain its LRU property */
                routingTable.update(fileKey);
                /* "response" is either 'e' => in case of hopsToLive became zero
                *                       'c' => if a cycle was detected
                *                       non-negative value => remaining hops to live
                */
                if (response == 0){
                    System.out.println("Successful return from node " + IP + ":" + portNum);
                    fileKeyAvailabilitySet.addUid(UID, new SimpleEntry <>(dstIP, dstPortNum));
                    fileKeyAvailabilitySet.print();
                    srcNodeOutputStream.write(response);
                    return;
                }
                else if (response == 'e'){
                    System.out.println("Collision  " + IP + ":" + portNum);
                    srcNodeOutputStream.write(response);
                }
                else if (response == 'c'){
                    System.out.println("Check FK consensus: cycle - go to next neighbor");
                }
                else{
                    hopsToLive = response;
                    fileKeyAvailabilitySet.addUid(UID, new SimpleEntry <>(dstIP, dstPortNum));
                }
                dstNodeSocket.close();
            }
            // all neighbors were checked, without fully satisfy hopsToLive
            srcNodeOutputStream.write(hopsToLive);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    static void joinSystem(String prevHashSeed, String UID, int hopsToLive,
                           OutputStream srcNodeOutputStream) throws NoSuchAlgorithmException {
        String hashSeed;
        byte[] answer, seedToReturn;
        ArrayList<Map.Entry<String, SimpleEntry<String, Integer>>> neighbors;
        System.out.println("Join");
        try {
            answer = new byte[65];
            if (joinRequestsSet.existsUid(UID)){
                answer[0] = 'c';
                srcNodeOutputStream.write(answer, 0, 65);
                return;
            }
            joinRequestsSet.initUid(UID);
            neighbors = routingTable.getRoutingTable();
            hashSeed = Crypto.generateSHA256hash(Crypto.getXOR(Crypto.generateCryptoRandomSHA256(), prevHashSeed));
            System.out.println("HopsToLive " + hopsToLive);
            if (hopsToLive == 0){
                answer = (0 + hashSeed).getBytes();
                srcNodeOutputStream.write(answer, 0, 65);
                return;
            }
            seedToReturn = (hopsToLive + hashSeed).getBytes();
            while(neighbors.size() > 0){
                byte[] buf;
                String neighborIP, command;
                Socket outNodeSocket;
                InputStream outNodeiStream;
                OutputStream outNodeoStream;
                SimpleEntry <String, Integer> neighborAddress;
                int randomNeighborIndex, timeout, neighborPortNum, totalBytes, bytes_read;

                randomNeighborIndex = (new Random()).nextInt(neighbors.size());
                neighborAddress = neighbors.get(randomNeighborIndex).getValue();
                neighborIP = neighborAddress.getKey();
                neighborPortNum = neighborAddress.getValue();

                outNodeSocket = new Socket();
                timeout = (int) TimeUnit.SECONDS.toMillis(5);

                try {
                    System.out.println("Socket connect in join " + neighborIP + " " + neighborPortNum);
                    outNodeSocket.connect(new InetSocketAddress(neighborIP, neighborPortNum), timeout);
                }catch (SocketTimeoutException | ConnectException e){
                    System.out.println("Join System: "
                            + neighborIP + " " + neighborPortNum + " " + e.toString());
                    /* remove neighbor from routing table */
                    routingTable.removeKey(neighbors.get(randomNeighborIndex).getKey());
                    neighbors.remove(randomNeighborIndex);
                    continue;
                }
                outNodeiStream = outNodeSocket.getInputStream();
                outNodeoStream = outNodeSocket.getOutputStream();

                command = "join " + hashSeed + " " +  UID + " " + (hopsToLive - 1);
                sendCommand(command, outNodeoStream);

                totalBytes = 0;
                buf = new byte[65];
                while ((bytes_read = outNodeiStream.read(buf)) != -1){
                    System.arraycopy(buf, 0, answer, totalBytes, bytes_read);
                    totalBytes += bytes_read;
                    if (totalBytes == 65){
                        break;
                    }
                }
                /* The first byte of "answer" is either (a) 'c' => if a cycle was detected
                *                                       (b) remaining hops to live
                *
                */
                if (answer[0] == '0'){
                    System.out.println("Node: " + IP + ": seed: " + hashSeed + " answer: " + new String(answer));
                    srcNodeOutputStream.write(answer, 0, 65);
                    joinRequestsSet.addUid(UID, neighborAddress);
                    return;
                }
                else if (answer[0] == 'c'){
                    /* in case of a cycle, keep the last hashSeed */
                    System.arraycopy(seedToReturn, 0, answer, 0, 65);
                }
                else{
                    System.out.println("Node: " + IP + ": seed: " + hashSeed + " answer: " + new String(answer));
                    hopsToLive = Character.getNumericValue(answer[0]);
                    joinRequestsSet.addUid(UID, neighborAddress);
                    seedToReturn = Arrays.copyOf(answer, 65);
                }
                neighbors.remove(randomNeighborIndex);
                outNodeSocket.close();
            }
            // all neighbors were checked, without success
            srcNodeOutputStream.write(seedToReturn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}