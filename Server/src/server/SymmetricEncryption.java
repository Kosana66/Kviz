package symmetric.enc.dec;
  
import java.security.SecureRandom;
import java.util.Scanner;
  
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
  
// Klasa koja implementira simetricnu enkripciju
public class SymmetricEncryption {
  
    private static final String AES = "AES";
  
    // Block cipher(CBC mode) - tzv Blok sifra kod koje se originalna poruka sifruje po grupama (blokovima)
    private static final String AES_CIPHER_ALGORITHM = "AES/CBC/PKCS5PADDING";
  
    private static Scanner message;
  
    // Funkcija koja kreira skriveni kljuc
    public static SecretKey createAESKey() throws Exception {
        SecureRandom securerandom = new SecureRandom();
        String seed = "RSZEOS2024";
        securerandom.setSeed(seed.getBytes());
        
        //prilikom pravljenja kljuca navodi se koji se algoritam koristi
        KeyGenerator keygenerator = KeyGenerator.getInstance(AES);
  
        //duzina kljuca se navodi prilikom pozivanja init funkcije
        //ovde koristimo duzinu 128 bita (za 256 bita je potrebno instalirati 
        //dodatne pakete)
        keygenerator.init(128, securerandom);
        
        SecretKey key = keygenerator.generateKey();
  
        return key;
    }
  
    // Funkcija koja kreira inicijalizacioni vektor - on je potreban kako bi se obezbedilo
    // tzv inicijalno stanje. Kod sifrovanja u blokovima, isti blokovi otvorenog teksta se
    //sifruju u iste blokove kriptovanog teksta, sto se moze spreciti inicijalizacionim vektorom
    // Inicijalizacioni vektor se ne smatra tajnom u kriptografskom sistemu, tj. moze se nekriptovan slati 
    public static byte[] createInitializationVector() {
        /*byte[] initializationVector = new byte[16];
        for (int i = 0; i < initializationVector.length; i++) {
            initializationVector[i] = (byte) (i + 1);
        }
        return initializationVector;*/
        return "0123456789ABCDEF".getBytes();
    }
  
    //Funkcija koja prima otvoreni tekst, kljuc i inicijalizacioni vektor i 
    //generise sifrat (cipher text)
    public static byte[] do_AESEncryption(String plainText, SecretKey secretKey, byte[] initializationVector) throws Exception{
        //klasa Cipher se koristi za enkripciju/dekripciju, prilikom kreiranja navodi se koji algoritam se koristi
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
        
        //IvParameterSpec se kreira koristeci inicijalizacioni vektor a potreban je za inicijalizaciju cipher objekta
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);
  
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
  
        //metoda doFinal nakon sto se inicijalizuje metodom init, vrsi enkripciju otvorenog teksta
        return cipher.doFinal(plainText.getBytes());
    }

    //Funkcija koja prima sifrat (kriptovan tekst), kljuc i inicijalizacioni vektor i vraca dekriptovani tekst
    //generise sifrat (cipher text)
    public static String do_AESDecryption(byte[] cipherText, SecretKey secretKey, byte[] initializationVector) throws Exception{
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
  
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);
  
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
  
        //ista metoda doFinal se koriti i za dekripciju
        byte[] result = cipher.doFinal(cipherText);
  
        return new String(result);
    }
  
    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
    /*
    // Main metoda 
    public static void main(String args[]) throws Exception{
        //generisi kljuc za enkripciju/dekripciju
        SecretKey symmetricKey = createAESKey();
  
        //prikazi kljuc (prethodno ga konvertuj koriscenjem bytesToHex metode kako bi se mogao prikazati u formi stringa
        System.out.println("Kljuc koji se koristi za simetricnu enkripciju :" + bytesToHex(symmetricKey.getEncoded()));
  
        //Napravi inicijalizacioni vektor
        byte[] initializationVector = createInitializationVector();
  
        //Otvoreni tekst, poruka koja se kriptuje
        //Kod simetricne enkripcije/dekripcije ne postoji ogranicenje na velicinu poruke koja se kriptuje/dekriptuje
        //String plainText = "Ovo je poruka koju je potrebno kriptovati.";
        String plainText = "admin:admin:admin";
        System.out.println("Originalna poruka je: " + plainText);
  
        // Enkripcija poruke
        byte[] cipherText = do_AESEncryption(plainText, symmetricKey, initializationVector);
  
        System.out.println("Sifrat, odnosno kriptovana poruka je: " + bytesToHex(cipherText));
  
        // Dekriptovanje poruke
        String decryptedText = do_AESDecryption(cipherText, symmetricKey, initializationVector);
  
        System.out.println("Desifrovana poruka je: " + decryptedText);
    }*/
        
        
        
  /*
        // Dekriptovanje poruke
        String decryptedText = SymmetricEncryption.do_AESDecryption(cipherText, symmetricKey, initializationVector);
  
        System.out.println("Desifrovana poruka je: " + decryptedText);
    */
}