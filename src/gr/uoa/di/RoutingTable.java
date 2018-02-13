package gr.uoa.di;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;

class RoutingTable {

    private final int capacity;
    private LinkedHashMap <String, SimpleEntry <String, Integer>> routingTable;

    RoutingTable(int capacity){
        System.out.println("Created RT");
        this.capacity = capacity;
        routingTable = new LinkedHashMap <String, SimpleEntry <String, Integer>>
                (capacity + 1, 1f, false) {
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > capacity;
            }
        };
    }

    synchronized boolean isAvailable(String FK){
        return !routingTable.containsKey(FK);
    }

    synchronized void addKey(String fileKey, String IP, Integer port){
        if (routingTable.size() == capacity) {
            //get first key in the routing table (least recently used) and delete it from the Set.
            routingTable.keySet().iterator().next();
        }
        routingTable.put(fileKey, new SimpleEntry<>(IP, port));
    }

    synchronized ArrayList<Map.Entry<String, SimpleEntry<String, Integer>>> getRoutingTable()
    {
        ArrayList <Map.Entry<String, SimpleEntry<String, Integer>>> addresses = new ArrayList<>(routingTable.size());
        addresses.addAll(routingTable.entrySet());
        return addresses;
    }

    synchronized void removeKey(String fileKey) {
        routingTable.remove(fileKey);
    }

    synchronized void update(String fileKey){
        SimpleEntry <String, Integer> updatedElement;

        updatedElement = routingTable.remove(fileKey);
        if (updatedElement == null){
            return;
        }
        routingTable.put(fileKey, updatedElement);
    }

    /* higher value means higher similarity */
    private int getSimilarity(String key1, String key2)
    {
        /* we know that keys will have the same length */
        int length, similarity, charKey1, charKey2;

        length = key1.length();
        similarity = 0;
        for (int i = 0; i < length; i++){
            charKey1 = key1.charAt(i);
            charKey2 = key2.charAt(i);
            similarity += 42 - Math.abs(charKey1 - charKey2);   //42 is the biggest distance between two ASCII codes in hex
            if (charKey1 != charKey2){
                break;
            }
        }
        return similarity;
    }

    synchronized LinkedHashMap <String, SimpleEntry <String, Integer>> getKeysBySimilarity(String requestedKey)
    {
        TreeMap <Integer, ArrayList <SimpleEntry <String, SimpleEntry <String, Integer>>>>
                simKeys = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<String, SimpleEntry<String, Integer>> s : routingTable.entrySet()){
            int similarity;
            ArrayList <SimpleEntry <String, SimpleEntry <String, Integer>>> keys;

            similarity = getSimilarity(s.getKey(), requestedKey);
            keys = simKeys.computeIfAbsent(similarity, k -> new ArrayList<>());
            keys.add(new SimpleEntry<>(s.getKey(), s.getValue()));
        }
        LinkedHashMap <String, SimpleEntry <String, Integer>> sortedRoutingTable = new LinkedHashMap<>();
        for (ArrayList <SimpleEntry <String, SimpleEntry <String, Integer>>> l: simKeys.values()){
            for (SimpleEntry<String, SimpleEntry<String, Integer>> s: l) {
                sortedRoutingTable.put(s.getKey(), s.getValue());
            }
        }

        return sortedRoutingTable;
    }

    void print()
    {
        System.out.println("Printing Routing Table (size = " + routingTable.size() + ")");
        for (Object o : routingTable.entrySet()) {
            Map.Entry me = (Map.Entry) o;
            SimpleEntry se = (SimpleEntry) me.getValue();
            System.out.println("Key: " + me.getKey() + " points to Node " + se.getKey() + ":" + se.getValue());
        }
    }
}
