package gr.uoa.di;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Random;

public class Crypto {

    public static KeyPair generateRSA(String descrText) throws NoSuchAlgorithmException {
        KeyPair keypair;
        KeyPairGenerator kpg;
        SecureRandom secRandom;

        kpg = KeyPairGenerator.getInstance("RSA");
        secRandom = SecureRandom.getInstance("NativePRNGNonBlocking");
        kpg.initialize(2048, secRandom);
        keypair = kpg.generateKeyPair();

        return keypair;
    }

    static Integer generateCryptoRandomInteger() throws NoSuchAlgorithmException {
        Random random;

        random = new Random(1337);
//        random = SecureRandom.getInstance("SHA1PRNG");
        return random.nextInt(Integer.MAX_VALUE - 1);
    }

    static String generateCryptoRandomSHA256() throws NoSuchAlgorithmException {
        SecureRandom secureRandomGenerator = SecureRandom.getInstance("SHA1PRNG");
        byte[] buffer = new byte[64];
        secureRandomGenerator.nextBytes(buffer);

        return Crypto.generateSHA256hash(new String(buffer));
    }

    static String generateSHA256hash(String plaintext) throws NoSuchAlgorithmException {
        String  hash;
        MessageDigest digest;

        digest = MessageDigest.getInstance("SHA-256");
        hash = DatatypeConverter.printHexBinary(digest.digest(plaintext.getBytes(StandardCharsets.UTF_8)));

        return hash;
    }

    static CipherOutputStream initEncryption(String hashDescrText, OutputStream outputStream)
                                            throws NoSuchPaddingException, NoSuchAlgorithmException,
                                            InvalidKeyException, IOException {
        Key secretKey;
        Cipher cipher;
        byte[] aes_key;

        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        /* AES key length is 128 bits/16 bytes */
        aes_key = Arrays.copyOfRange(DatatypeConverter.parseHexBinary(hashDescrText), 0, 16);
        secretKey = new SecretKeySpec(aes_key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        return new CipherOutputStream(outputStream, cipher);
    }

    static void sendEncrypted(String hashDescrText, InputStream inputStream, OutputStream outputStream)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        int count;
        Key secretKey;
        Cipher cipher;
        byte[] aes_key;
        byte[] buffer;
        CipherOutputStream cipherOutputStream;

        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        /* AES key length is 128 bits/16 bytes */
        aes_key = Arrays.copyOfRange(DatatypeConverter.parseHexBinary(hashDescrText), 0, 16);
        secretKey = new SecretKeySpec(aes_key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        buffer = new byte[8192];
        cipherOutputStream = new CipherOutputStream(outputStream, cipher);
        while ((count = inputStream.read(buffer)) > 0) {
            cipherOutputStream.write(buffer, 0, count);
        }
        cipherOutputStream.close();
    }

    static CipherInputStream initDecryption(String hashDescrText, InputStream inputStream)
                                            throws NoSuchPaddingException, NoSuchAlgorithmException,
                                            InvalidKeyException, IOException {
        Key secretKey;
        Cipher cipher;
        byte[] aes_key;

        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        /* AES key length is 128 bits/16 bytes */
        aes_key = Arrays.copyOfRange(DatatypeConverter.parseHexBinary(hashDescrText), 0, 16);
        secretKey = new SecretKeySpec(aes_key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        return new CipherInputStream(inputStream, cipher);
    }

    static AbstractMap.SimpleEntry<String, String> signFile(PrivateKey privateKey, PublicKey publicKey, String pathToFile)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException {
        int len;
        File file;
        Signature signature;
        byte[] digitalSignature, buffer;
        FileInputStream fileInputStream;
        StringBuilder stringBuilderPK, stringBuilderDS;

        stringBuilderPK = new StringBuilder();
        stringBuilderDS = new StringBuilder();
        signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privateKey);

        buffer = new byte[8192];
        file = new File(pathToFile);
        fileInputStream = new FileInputStream(file);
        while ((len = fileInputStream.read(buffer)) >= 0){
            signature.update(buffer, 0, len);
        }
        digitalSignature = signature.sign();

        /* convert encoded public key and digital signature to hex */
        for(byte b : publicKey.getEncoded()){
            stringBuilderPK.append(String.format("%02x", b));
        }
        for(byte b : digitalSignature){
            stringBuilderDS.append(String.format("%02x", b));
        }
        return new AbstractMap.SimpleEntry <>(stringBuilderPK.toString(), stringBuilderDS.toString());
    }

    public static boolean verifySignature(String encodedPK, String digitalSignature, String subDirName, String fileName)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, SignatureException, IOException {
        int len;
        File file;
        String filePath;
        Signature signature;
        PublicKey publicKey;
        FileInputStream fileInputStream;
        byte[] buffer, byteArrayDS, byteArrayPK;

        filePath = System.getProperty("user.dir") + "/Downloads/" + subDirName + "/" + fileName;
        byteArrayPK = DatatypeConverter.parseHexBinary(encodedPK);
        byteArrayDS = DatatypeConverter.parseHexBinary(digitalSignature);
        signature = Signature.getInstance("SHA1withRSA");
        publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(byteArrayPK));
        signature.initVerify(publicKey);
        buffer = new byte[8192];
        file = new File(filePath);
        fileInputStream = new FileInputStream(file);
        while ((len = fileInputStream.read(buffer)) >= 0){
            signature.update(buffer, 0, len);
        }
        return signature.verify(byteArrayDS);
    }

    static String getXOR(String s1, String s2) {
        int i;
        byte[] byteArray1, byteArray2;

        i = 0;
        byteArray1 = DatatypeConverter.parseHexBinary(s1);
        byteArray2 = DatatypeConverter.parseHexBinary(s2);
        for (byte b: byteArray1){
            byteArray1[i] = (byte) (b ^ byteArray2[i++]);
        }

        return new String(byteArray1);
    }

    static AbstractMap.SimpleEntry<KeyPair, String> createSSK(String descrText) throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidKeyException, SignatureException {
        KeyPair namespaceKP;
        String hashPK, hashDescrText, hashFK; // PK: Public Key, FK: File Key

        namespaceKP = generateRSA(descrText);
        hashDescrText = generateSHA256hash(descrText);
        hashPK = generateSHA256hash(new String(namespaceKP.getPublic().getEncoded()));
        hashFK = generateSHA256hash(getXOR(hashPK, hashDescrText));

        /* encryption of file using the SHA256 of the descriptive text string */
        return new AbstractMap.SimpleEntry<>(namespaceKP, hashFK);
    }

}
