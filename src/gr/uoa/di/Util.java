package gr.uoa.di;


import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Util {

    public static String getInternalIP() throws IOException
    {
        String ip;
        Process pip;
        BufferedReader bufferReader;
        if (System.getProperty("os.name").contains("Mac"))
        {
            pip = Runtime.getRuntime().exec("ipconfig getifaddr en0");
            bufferReader = new BufferedReader(new InputStreamReader(pip.getInputStream()));
            if ((ip = bufferReader.readLine()) == null)
            {
                pip = Runtime.getRuntime().exec("ipconfig getifaddr en1");
                bufferReader = new BufferedReader(new InputStreamReader(pip.getInputStream()));
                if ((ip = bufferReader.readLine()) == null)
                {
                    pip = Runtime.getRuntime().exec("ipconfig getifaddr en2");
                    bufferReader = new BufferedReader(new InputStreamReader(pip.getInputStream()));
                    if (bufferReader.readLine() == null)
                    {
                        System.out.println("Cannot find ip address");
                        System.exit(-1);
                    }
                }
            }
        }
        else
        {
            pip = Runtime.getRuntime().exec("hostname -I");
            bufferReader = new BufferedReader(new InputStreamReader(pip.getInputStream()));
            if ((ip = bufferReader.readLine()) == null)
            {
                System.out.println("Cannot find ip address");
                System.exit(-1);
            }
        }

        assert ip != null;
        return ip.trim();
    }

    static String getExternalIP() throws IOException {
        String[] ipSites = {"http://checkip.amazonaws.com/", "http://icanhazip.com/", "http://www.trackip.net/ip",
                "http://myexternalip.com/raw", "http://ipecho.net/plain"};
        for (String site: ipSites) {
            String ip;
            URL whatismyip = new URL(site);
            BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            ip = in.readLine();
            in.close();
            if (ip != null) {
                return ip;
            }
        }
        return null;
    }

    static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    static long bytesToLong(byte[] b) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    static String readStringFromInputStream(InputStream inputStream, int bytesToRead) throws IOException {
        byte[] buf;
        StringBuilder input = new StringBuilder();
        int bytes_read, totalBytesRead;

        totalBytesRead = 0;
        buf = new byte[8192];
        while ((bytes_read = inputStream.read(buf, 0, bytesToRead - totalBytesRead)) != -1){
            input.append(new String(buf, 0, bytes_read));
            totalBytesRead += bytes_read;
            if (totalBytesRead == bytesToRead){
                break;
            }
        }
        return input.toString();
    }

    static byte[] readByteArrayFromInputStream(InputStream inputStream, int bytesToRead) throws IOException {
        byte[] buf, finalBuf;
        int bytes_read, totalBytesRead;

        totalBytesRead = 0;
        buf = new byte[8192];
        finalBuf = new byte[bytesToRead];
        while ((bytes_read = inputStream.read(buf, 0, bytesToRead - totalBytesRead)) != -1){
            System.arraycopy(buf, 0, finalBuf, totalBytesRead, bytes_read);
            totalBytesRead += bytes_read;
            if (totalBytesRead == bytesToRead){
                break;
            }
        }
        return finalBuf;
    }

    static String sendCommandBlocking(String command, InputStream inputStream,
                                      OutputStream outputStream, int responseSize) throws IOException {
        byte[] buf;
        /* sending the upload command to the node */
        buf = command.getBytes(StandardCharsets.UTF_8);
        outputStream.write(command.length());
        outputStream.write(buf, 0, command.length());
        /* waiting for response */
        return readStringFromInputStream(inputStream, responseSize);
    }

    static void createDir(String dirName, String subDirName) {
        File newDir;
        String fullPath;

        fullPath = System.getProperty("user.dir") + "/"  + dirName + "/" + subDirName + "/";
        newDir = new File(fullPath);

        if (!newDir.exists()) {
            if (!newDir.mkdirs()){
                System.out.println("Could not create directory " + dirName);
            }
        }
    }

}
