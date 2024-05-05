package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import symmetric.enc.dec.SymmetricEncryption;

public class ConnectedClient implements Runnable {
    
    private Socket socket;
    private PrintWriter pw;
    private BufferedReader br;
    private String username = "";
    private String password = "";
    private String role = "";
    
    private SecretKey symmetricKey;
    private byte[] initializationVector;
    private ArrayList<ConnectedClient> allClients;
    String sP = System.getProperty("file.separator");
    
    private Map<Integer, String> questions = new HashMap<>();
    private Map<Integer, List<String>> answers = new HashMap<>();
    private Map<Integer, String> correctAnswers = new HashMap<>();
    
    private Map<Integer, Integer> results = new HashMap<>();
    
    private Map<String, Integer> stateAnsweredQues = new HashMap<>();
    private Map<String, Integer> stateCorrectAns = new HashMap<>();
    
    private int activeSet = 1;
    private int numOfQuestion = 0;
    private int numOfCorrectAnswers = 0;
    private int numOfAnsweredQuest = 0;
    
    
    public ConnectedClient(Socket clientSocket, ArrayList<ConnectedClient> allClients, SecretKey symmetricKey, byte[] initializationVector) {
        this.socket = clientSocket;
        this.allClients = allClients;
        this.symmetricKey = symmetricKey;
        this.initializationVector = initializationVector;
        // inicijalno nijedan set nije predjen
        for (int i = 1; i <= 4; i++) {
            results.put(i, -5);
        }
        try {  
            this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream(),"UTF-8"));
            this.pw = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()), true);
        } catch (IOException ex) {
            Logger.getLogger(ConnectedClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String getUsername() {
        return this.username;
    }
    
    public String getRole() {
        return this.role;
    }
    
    public int getActiveSet() {
        return this.activeSet;
    }
    
    public void setActiveSet(int tmpInt) {
       this.activeSet = tmpInt;
    }
    
    public void resetSet() {
        numOfQuestion = 0;
        numOfCorrectAnswers = 0;
        numOfAnsweredQuest = 0;
    }
    
    public void setStateAnsweredQues(String tmpUsername, int tmpNum) {
        stateAnsweredQues.put(tmpUsername, tmpNum);
    }
    
    public void setStateCorrectAns(String tmpUsername, int tmpNum) {
        stateCorrectAns.put(tmpUsername, tmpNum);
    }
    
    private boolean userExistsForWrite(String username, String password, File file) {
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] tokens = line.split(":");
                if(tokens.length != 3){
                    System.out.println("Greska pri ocitavanju username:password:role iz fajla users.txt");
                    System.exit(0);
                }
                if (username.equals(tokens[0]) || password.equals(tokens[1]) ) {
                    scanner.close();
                    return true;
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Datoteka users.txt nije pronađena");
            Logger.getLogger(ConnectedClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false; 
    }
    
    private boolean userExistsForRemoveAndLogIn(String username, String password, String role, File file) {
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] tokens = line.split(":");
                if(tokens.length != 3){
                    System.out.println("Greska pri ocitavanju username:password:role iz fajla users.txt");
                    System.exit(0);
                }
                if( username.equals(tokens[0]) && password.equals(tokens[1]) && role.equals(tokens[2]) ) {
                    scanner.close();
                    return true; 
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Datoteka users.txt nije pronađena");
            Logger.getLogger(ConnectedClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false; 
    }
    
    private void writeFileUsers(String usernameTmp, String passwordTmp, String roleTmp) {
        File file = new File("." + sP + "src" + sP + "server" + sP + "users.txt");

        // Provjera da li korisnik već postoji
        if (userExistsForWrite(usernameTmp, passwordTmp, file)) {
            System.out.println("Korisnik sa istim podacima već postoji u datoteci users.txt");
            return;
        }

        try {
            try (BufferedWriter out = new BufferedWriter(new FileWriter(file, true))) {
                out.write(usernameTmp + ":" + passwordTmp + ":" + roleTmp);
                out.newLine();
                System.out.println("Podaci su uspešno upisani u datoteku users.txt");
            }
        } catch (IOException ex) {
            System.out.println("Greška pri upisu u datoteku users.txt");
        }
    }
    
    private void removeFileUsers(String usernameTmp, String passwordTmp, String roleTmp) {
        
        File inputFile = new File("." + sP + "src" + sP + "server" + sP + "users.txt");

        if (!userExistsForRemoveAndLogIn(usernameTmp, passwordTmp, roleTmp, inputFile)) {
            System.out.println("Korisnik " + usernameTmp + " ne postoji u datoteci users.txt");
            return;
        }

        try {
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
                File tempFile = new File("." + sP + "src" + sP + "server" + sP + "temp_users.txt");

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                    String currentLine;

                    while ((currentLine = reader.readLine()) != null) {
                        String[] tokens = currentLine.split(":");
                        if (tokens.length == 3 &&
                                usernameTmp.equals(tokens[0]) &&
                                passwordTmp.equals(tokens[1]) &&
                                roleTmp.equals(tokens[2])) {
                            continue; 
                        }
                        writer.write(currentLine + System.getProperty("line.separator"));
                    }
                }

                if (!inputFile.delete()) {
                    System.out.println("Nije moguće obrisati originalnu datoteku users.txt");
                    return;
                }

                if (!tempFile.renameTo(inputFile)) {
                    System.out.println("Nije moguće preimenovati privremenu datoteku u users.txt");
                }

                System.out.println("Linija sa korisnikom " + usernameTmp + " je uspešno obrisana iz datoteke users.txt");
                this.pw.println("Korisnik je uklonjen! ");
            }
        } catch (IOException ex) {
            System.out.println("Greška pri brisanju linije iz datoteke users.txt");
        }
    }
   
    
    private void ReadSetFile() {
        try {
            try (BufferedReader reader = new BufferedReader(new FileReader(new File("." + sP + "src" + sP + "server" + sP + "set" + activeSet + ".txt")))) {
                String line;
                int questionCount = 0;
                // Čitanje linija iz datoteke
                while ((line = reader.readLine()) != null) {
                    // Ako je prazna linija, preskoči je
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Dodavanje pitanja
                    questions.put(++questionCount, line);
                    
                    // Lista za čuvanje odgovora
                    List<String> answerList = new ArrayList<>();
                    
                    // Čitanje odgovora
                    for (char answerChar = 'a'; answerChar <= 'd'; answerChar++) {
                        if ((line = reader.readLine()) != null) {
                            // Izdvoji tekst odgovora nakon taba, bez oznake a), b), c), d)
                            String answer = line.substring(line.indexOf('\t') + 4 );
                            answerList.add(answer);
                            // Zapamti tacan odgovor
                            if(answerChar == 'd')
                                correctAnswers.put(questionCount, answer);
                        } else {
                            // Ako nema više linija, prekidamo čitanje
                            break;
                        }
                    }
                    
                    // Mešanje odgovora
                    Collections.shuffle(answerList);
                    
                    // Vracanje oznaka a), b), c), d) nakon mesanja
                    for (int i = 0; i < answerList.size(); i++) {
                        answerList.set( i, (char)('a' + i) + ") " + answerList.get(i));
                    }
                    
                    // Cuvanje odgovora
                    answers.put(questionCount, answerList);
                }
                // Zatvaranje reader-a
                
                // Cuvanje oznake tacnog odgovora nakon mesanja 
                for (int i = 1; i <= correctAnswers.size(); i++) {
                    for (int j = 0; j < answers.get(i).size(); j++) {
                        if(correctAnswers.get(i).equals(answers.get(i).get(j).substring(3))) {
                            correctAnswers.put(i, (char)('a' + j) + ")");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Greška prilikom čitanja datoteke: " + e.getMessage());
        }
    }
    
    private void updateStatus() {
        String connectedUsers = "Učesnici-";
        for (ConnectedClient c : this.allClients) {
            if(!c.getRole().equals("admin"))
                connectedUsers += " " + c.getUsername();
        }

        for (ConnectedClient allUpdateCB : this.allClients) {
            allUpdateCB.pw.println(connectedUsers);
        }
    }
    
    public void updateTableState() {
        int tmpAnsw = 0;
        int tmpCorrect = 0;
        
        for (int i = 1; i <= 4; i++) {
            if(results.get(i) != -5) {
                tmpAnsw += 10;
                tmpCorrect += results.get(i);
            }
        }
        tmpAnsw += numOfAnsweredQuest;
        tmpCorrect += numOfCorrectAnswers;
        System.out.println(this.username + ":" + tmpCorrect + "-" + tmpAnsw);

        for (ConnectedClient allUpdateTable : this.allClients) {
            allUpdateTable.setStateAnsweredQues(this.username, tmpAnsw);
            allUpdateTable.setStateCorrectAns(this.username, tmpCorrect);
        }
        
    }

    
    private void FiftyFiftyHelp() {
        String str = "***********50/50**************\n";
        List<String> ansList = answers.get(numOfQuestion - 1);
        String correctAnswer = correctAnswers.get(numOfQuestion - 1); 
        
        Random rand = new Random();
        int randomNum;
        
        do {
            randomNum = rand.nextInt(4);
        } while (correctAnswer.charAt(0) == randomNum + 'a');
        
        char randomPrefix = (char) ('a' + randomNum);
        
        for(String ans : ansList) {
            if(ans.startsWith(String.valueOf(correctAnswer.charAt(0))) || ans.startsWith(String.valueOf(randomPrefix))) {
                str +=  ans + '\n';
            }
        }
        
        str += "*******************************\n";
        this.pw.println(str);
    }
    
    private void ReplaceQuestion() {
        String str = "************BONUS PITANJE*************\n";
        str += "Pitanje " + (numOfQuestion - 1) + "." + questions.get(11).substring(3)+ '\n';
        List<String> ansList = answers.get(11);

        // Zamena pitanja i odgovora
        answers.put(numOfQuestion - 1, ansList);
        correctAnswers.put(numOfQuestion - 1, correctAnswers.get(11));

        // Dodavanje odgovora u string
        for (String ans : ansList) {
            str += ans + '\n';
        }

        str += "*****************************************\n";
        this.pw.println(str);
    }
    
    @Override
    public void run() {
        while (true) {
            if (username.equals("") || password.equals("") || role.equals("")) {
                try {
                    String [] tokeni = this.br.readLine().split(":");
                    if (tokeni.length == 3) {
                        this.username = tokeni[0];
                        this.password = tokeni[1];
                        this.role = tokeni[2];
                        if (username != null || password != null || role != null) {
                            File inputFile = new File("." + sP + "src" + sP + "server" + sP + "users.txt");
                            if(userExistsForRemoveAndLogIn(username, password, role, inputFile )) {
                                System.out.println("Konektovao se klijent " + username);
                                updateStatus();
                                this.pw.println("Dobrodosli u kviz!     Zatrazite nov set pitanja za pocetak! :) ");
                            }
                            else {
                                this.pw.println("Pogresno ime ili sifra! ");
                                for (ConnectedClient cl : this.allClients) {
                                    if (cl.getUsername().equals(this.username)) {
                                        this.allClients.remove(cl);
                                        break;
                                    }
                                }
                            }
                        } else {
                            System.out.println("Diskonektovao se klijent " + username);
                            for (ConnectedClient cl : this.allClients) {
                                if (cl.getUsername().equals(this.username)) {
                                    this.allClients.remove(cl);
                                    break;
                                }
                            }
                            updateStatus();
                            break;
                        }
                    }
                    else {
                        System.out.println("Greska pri ocitavanju username:password:role");
                        System.exit(0);
                    }
                } catch (Exception ex) {
                    System.out.println("Diskonektovao se klijent ");
                    updateStatus();
                    return;
                }
            }
            else {
                try {
                    String line = this.br.readLine();
                    if (line != null) {
                        String[] informacija = line.split("-");
                        String primacKorisnik = informacija[0];
                        String poruka = informacija[1];
                        String [] tokeni = poruka.split(":");
                        if (tokeni.length == 4) {
                            if(tokeni[0].equals("upis")) {
                                boolean isValidUsername = tokeni[1].matches("^[a-zA-Z][a-zA-Z0-9]*$");
                                if (isValidUsername) {
                                    boolean isValidPassword = tokeni[2].matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{6,}$");
                                    if (isValidPassword) {
                                        this.pw.println("Korisnik je dodat! ");
                                        // Enkripcija poruke
                                        try {
                                            byte[] cipherText = SymmetricEncryption.do_AESEncryption(poruka, symmetricKey, initializationVector);
                                            System.out.println("Sifrat, odnosno kriptovana poruka je: " + SymmetricEncryption.bytesToHex(cipherText));
                                            String decryptedText = SymmetricEncryption.do_AESDecryption(cipherText, symmetricKey, initializationVector);
                                            System.out.println("Desifrovana poruka je: " + decryptedText);
                                            writeFileUsers(tokeni[1], tokeni[2], tokeni[3]);
                                        } catch (Exception ex) {
                                            Logger.getLogger(ConnectedClient.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                    else {
                                        this.pw.println("Password je nevalidan! ");
                                    }
                                } 
                                else {
                                    this.pw.println("Korisnicko ime je nevalidno! ");
                                }
                            }
                            else if(tokeni[0].equals("brisanje")) {
                                removeFileUsers(tokeni[1], tokeni[2], tokeni[3]);
                            }
                        }
                        else if(poruka.equals("nov set")) {
                            if(results.get(activeSet) == -5) {
                                if(numOfQuestion == 0) {
                                    numOfQuestion = 1;
                                    ReadSetFile();
                                    // kreiranje stringa za slanje klijentu
                                    String question = "Pitanje " + questions.get(numOfQuestion) + "\n";
                                    List<String> ansList = answers.get(numOfQuestion);
                                    for (int j = 0; j < ansList.size(); j++) {
                                        question += ansList.get(j) +  '\n';
                                    } 
                                    numOfQuestion++; 
                                    this.pw.println(question);
                                }
                                else {
                                    this.pw.println("Trenutno odgovarate na ovaj set pitanja\n");
                                }
                            }
                            else {
                                this.pw.println("Ovaj set pitanja je vec predjen\n");
                            }
                        }
                        else if ((poruka.equals("a)") || poruka.equals("b)") || poruka.equals("c)") || poruka.equals("d)")) && numOfQuestion > 0) {                       
                            if(numOfQuestion <= 10)  {
                                if(poruka.equals(correctAnswers.get(numOfQuestion-1))) {
                                    numOfCorrectAnswers++;
                                    stateCorrectAns.put(username, numOfCorrectAnswers);
                                    this.pw.println("\nTacan odgovor!\n");
                                }
                                else {
                                    this.pw.println("\nPogresan odgovor!\n");
                                }
                                numOfAnsweredQuest++;
                                stateAnsweredQues.put(username, numOfAnsweredQuest);
                                updateTableState();
                                String question = "Pitanje " + questions.get(numOfQuestion) + "\n";
                                List<String> ansList = answers.get(numOfQuestion);
                                for (int j = 0; j < ansList.size(); j++) {
                                    question += ansList.get(j) +  '\n';
                                }
                                numOfQuestion++;  
                                this.pw.println(question);
                            } else if(numOfQuestion == 11)  {
                                if(poruka.equals(correctAnswers.get(numOfQuestion-1))) {
                                    numOfCorrectAnswers++;
                                    stateCorrectAns.put(username, numOfCorrectAnswers);
                                    this.pw.println("\nTacan odgovor!\n");
                                }
                                else {
                                    this.pw.println("\nPogresan odgovor!\n");
                                }
                                numOfAnsweredQuest++;
                                stateAnsweredQues.put(username, numOfAnsweredQuest);
                                results.put(activeSet, numOfCorrectAnswers);
                                this.pw.println("Zavrsili ste set " + activeSet + "\nBroj tacnih odgovora je : " + numOfCorrectAnswers + " od ukupno " + numOfAnsweredQuest + " pitanja\n");
                                numOfQuestion = 0;
                                numOfCorrectAnswers = 0;
                                numOfAnsweredQuest = 0;
                                updateTableState();
                            }
                        }
                        else if (poruka.equals("1") || poruka.equals("2") || poruka.equals("3") || poruka.equals("4")) {   
                            for (ConnectedClient allUpdateAS : this.allClients) {
                                allUpdateAS.setActiveSet(Integer.parseInt(poruka));
                                allUpdateAS.pw.println("Aktivan set je " + allUpdateAS.getActiveSet());
                                allUpdateAS.resetSet();
                            }
                        }
                        else if (poruka.equals("50/50") && results.get(activeSet) == -5) {
                            FiftyFiftyHelp();
                        }
                        else if (poruka.equals("zamena") && results.get(activeSet) == -5) {
                            ReplaceQuestion();
                        }
                        else if (poruka.equals("stanje")) {
                            updateTableState();
                            List<String> usernames = new ArrayList<>(stateCorrectAns.keySet());
                            Collections.sort(usernames, (u1, u2) -> -Integer.compare(stateCorrectAns.get(u1), stateCorrectAns.get(u2)));

                            this.pw.println(" ------------STANJE NA TABELI----------------");
                            for (int i = 0; i < usernames.size(); i++) {
                                String tmpUser = usernames.get(i);
                                int tmpCorrectAnswers = stateCorrectAns.get(tmpUser);
                                int tmpTotalQuestions = stateAnsweredQues.get(tmpUser);
                                this.pw.println(String.format("%-2d. %-20s %-2d/%d", (i + 1), tmpUser, tmpCorrectAnswers, tmpTotalQuestions));

                            }
                            this.pw.println(" --------------------------------------------");
                        }
                        else if (tokeni.length == 2 && tokeni[0].equals("upomoć") && results.get(activeSet) == -5) {
                            for (ConnectedClient clnt : this.allClients) {
                               if (clnt.getUsername().equals(primacKorisnik)) {
                                   clnt.pw.println(this.username + ": " + tokeni[1]);
                               } else {
                                   if (primacKorisnik.equals("")) {
                                       this.pw.println("Korisnik " + primacKorisnik + " je odsutan!");
                                   }
                               }
                            }
                        }
                    }
                    else {
                        System.out.println("Diskonektovao se klijent " + username);
                        for (ConnectedClient cl : this.allClients) {
                            if (cl.getUsername().equals(this.username)) {
                                this.allClients.remove(cl);
                                updateStatus();
                                return;
                            }
                        }
                    }
                } catch (IOException ex) {
                    System.out.println("Diskonektovao se klijent " + username);
                    for (ConnectedClient cl : this.allClients) {
                        if (cl.getUsername().equals(this.username)) {
                            this.allClients.remove(cl);
                            updateStatus();
                            return;
                        }
                    }
                }
            }
        }
    }
    
}
