package gr.uoa.di;

import javax.crypto.CipherInputStream;
import java.io.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import static gr.uoa.di.Util.*;

class ClientFileManager {

    private String fileInfoPath, downloadsPath;

    ClientFileManager(String subDirName) {
        fileInfoPath = System.getProperty("user.dir") + "/FileInfo/" + subDirName + "/";
        downloadsPath = System.getProperty("user.dir") + "/Downloads/" + subDirName + "/";
        createDir("FileInfo", subDirName);
        createDir("Downloads", subDirName);
    }

    synchronized void addFileInfo(String fileKey, String fileName,
                                  String descrString, String encodedPK, String digitalSignature)
            throws IOException, NoSuchAlgorithmException {
        File newFile;
        String fileInfo;


        newFile = new File(fileInfoPath + fileKey);
        if (!newFile.createNewFile()) {
            System.out.println("Creation of " + fileName + " info failed!");
            System.exit(1);
        }
        fileInfo = fileName + ", " + descrString + ", " + encodedPK + ", " + digitalSignature;
        Files.write(newFile.toPath(), fileInfo.getBytes());
    }

    synchronized String[] retrieveFileInfo(String fileKey) throws IOException {
        File file;
        String fileInfo;

        file = new File(fileInfoPath + fileKey);
        if (!file.exists()) {
            return null;
        }
        fileInfo = new String (Files.readAllBytes(file.toPath()));

        return fileInfo.split(", ");
    }

    synchronized HashMap<String, String> getFileInfoKeysNames() throws IOException {
        File folder = new File(fileInfoPath);
        File[] listOfFiles = folder.listFiles();
        HashMap <String, String> fileKeysToFileNames = new HashMap<>();
        if (listOfFiles == null) {
            return fileKeysToFileNames;
        }
        for (File file: listOfFiles) {
            String fileName;
            String[] fileInfo;

            fileName = file.getName();
            fileInfo = retrieveFileInfo(fileName);
            assert fileInfo != null: "trying to get file info of a non existing file";
            fileKeysToFileNames.put(fileName, fileInfo[0]);
        }

        return fileKeysToFileNames;
    }

    synchronized void removeFileInfo(String fileKey) {
        File file = new File(fileInfoPath + fileKey);

        if (!file.delete()) {
            System.out.println("Could not delete fileInfo of fileKey " + fileKey);
        }
    }

    synchronized void addToDownload(String fileName, long fileSize, CipherInputStream cipherInputStream) throws IOException {
        int len;
        File file;
        byte[] buffer;
        OutputStream outputStream;

        buffer = new byte[8192];
        file = new File(downloadsPath + fileName);
        outputStream = new FileOutputStream(file, false);
        while ((len = cipherInputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, len);
            fileSize -= len;
            if (fileSize == 0){
                break;
            }
        }
        outputStream.close();
        cipherInputStream.close();
    }

    synchronized void removeFromDownloads(String fileName) {
        File file = new File(downloadsPath + fileName);

        if(!file.delete()){
            System.out.println("Deleting " + fileName + " failed!");
        }
    }

}