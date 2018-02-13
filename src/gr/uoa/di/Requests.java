package gr.uoa.di;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Requests {

    private HashMap <String, ArrayList<SimpleEntry <String, Integer>>> requestsMap;

    public Requests(){
        requestsMap = new HashMap<>();
    }

    /* address of (1) next node for joinRequests or (2) prev node for fileRequests */
    public synchronized void addUid(String uid, SimpleEntry <String, Integer> address) {
        requestsMap.get(uid).add(address);
    }

    public synchronized void initUid(String uid) {
        requestsMap.put(uid, new ArrayList<>());
    }

    public synchronized void removeUid(String uid) {
        requestsMap.remove(uid);
    }

    public synchronized boolean existsUid(String uid) {
        return requestsMap.containsKey(uid);
    }

    public synchronized ArrayList <SimpleEntry <String, Integer>> getValues(String uid) { return requestsMap.get(uid); }

    public synchronized void print(){
        System.out.print("Printing requestmap: ");
        for (Object o : requestsMap.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            System.out.println(pair.getKey() + " = " + pair.getValue());
        }
    }
}
