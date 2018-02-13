package gr.uoa.di;

import java.io.File;
import java.util.LinkedHashSet;

public class StorageLRU {

    private long capacity, size;
    private LinkedHashSet <String> fileKeys;

    StorageLRU(long capacity) {
        long minCapacity, maxCapacity;

        minCapacity = 10L * 1024L * 1024L * 1024L;
        maxCapacity = new File("/").getFreeSpace();
        size = 0;
        if (capacity < minCapacity) {
            this.capacity = minCapacity;
        }
        else if (capacity > maxCapacity){
            this.capacity = maxCapacity;
        }
        else{
            this.capacity = capacity;
        }
        fileKeys = new LinkedHashSet<>();
    }

    synchronized void update(String fileKey){
        fileKeys.remove(fileKey);
        fileKeys.add(fileKey);
    }

    private void evict(){
        assert fileKeys.size() > 0 : "Trying to remove a fileKey from empty Storage LRU";

        String oldestFK;
        File removedFile;

        oldestFK = fileKeys.iterator().next();
        fileKeys.remove(oldestFK);
        /* delete file from the storage */
        removedFile = new File(oldestFK);
        assert removedFile.delete() : "Error on removing file " + oldestFK;
        size -= removedFile.length();
    }

    synchronized boolean exceedsStorageCapacity(long fileSize) {
        return fileSize > capacity;
    }

    synchronized boolean contains(String fileKey) {
        return fileKeys.contains(fileKey);
    }

    synchronized void add(String fileKey, long fileSize) {
        while (fileSize + size > capacity){
            evict();
        }
        size += fileSize;
        fileKeys.add(fileKey);
    }

    synchronized void print() {
        System.out.println("\nPrinting Storage LRU\n");
        fileKeys.forEach(System.out::println);
        System.out.println();
    }

}
