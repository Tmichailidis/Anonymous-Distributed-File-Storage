package gr.uoa.di;

import java.io.*;
import java.util.ArrayList;

import static gr.uoa.di.Util.*;

class DataStore {

    private String basePath;
    private StorageLRU storageLRU;

    DataStore(long storageCapacity, String subDirName)
    {
        basePath = System.getProperty("user.dir") + "/Storage/" + subDirName + "/";
        createDir("Storage", subDirName);
        storageLRU = new StorageLRU(storageCapacity);
    }

    FileInputStream getFileInputStream(String fileKey) throws IOException {
        storageLRU.update(fileKey);
        storageLRU.print();
        return new FileInputStream(new File(basePath + fileKey));
    }

    long getFileSize(String fileKey){
        return (new File(basePath + fileKey)).length();
    }

    boolean isAvailable(String fileKey) {
        return !storageLRU.contains(fileKey);
    }
    boolean diskAvailability(long fileSize) { return !storageLRU.exceedsStorageCapacity(fileSize); }

    void saveFile(InputStream inputStream, ArrayList<OutputStream> nextNodesStreams,
                         String fileName) throws IOException {
        int len;
        long size;
        byte[] buf;
        File newFile;
        FileOutputStream output;

        newFile = new File(basePath + fileName);
        newFile.getParentFile().mkdirs();
        if (!newFile.createNewFile()){
            System.out.println("Cannot create new file");
            return;
        }
        output = new FileOutputStream(newFile, false);
        final long start = System.nanoTime();
        buf = readByteArrayFromInputStream(inputStream, 8);
        size = bytesToLong(buf);
        System.out.println("Size in datastore = " + size);
        for (OutputStream dstOutputStream: nextNodesStreams){
            dstOutputStream.write(buf, 0, 8);
        }
        buf = new byte[8192];
        while ((len = inputStream.read(buf)) != -1) {
            output.write(buf, 0, len);
            for (OutputStream dstOutputStream: nextNodesStreams){
                dstOutputStream.write(buf, 0, len);
            }
            size -= len;
            if (size == 0){
                break;
            }
        }
        final long end = System.nanoTime();
        System.out.println("File saved in storage in : " + ((end - start) / 1000000) + "ms");

        /* save the new filename in the data structure */
        storageLRU.add(fileName, newFile.length());
        output.close();
    }


}
