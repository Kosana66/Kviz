package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import symmetric.enc.dec.SymmetricEncryption;


public class Server {

    private ServerSocket socket;
    private int port;
    private ArrayList<ConnectedClient> allClients;
    private SecretKey symmetricKey;
    private byte[] initializationVector;
    
    public Server(int port){
        try {
            this.port = port;
            try {
                this.socket = new ServerSocket(port);
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            this.allClients = new ArrayList<>();
            // Pozivamo metode iz klase SymmetricEncryption
            this.symmetricKey = SymmetricEncryption.createAESKey();
            this.initializationVector = SymmetricEncryption.createInitializationVector();
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void acceptClients() {
        Socket clientSocket = null;
        //Thread thr;
        while(true) {
            System.out.println("Cekam novog klijenta...");
            try {
                clientSocket = this.socket.accept();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex); 
            }
            if(clientSocket != null) {
                ConnectedClient clnt = new ConnectedClient(clientSocket, allClients, this.symmetricKey, this.initializationVector);
                allClients.add(clnt);
                Thread thr = new Thread(clnt);
                thr.start();
            }
            else {
                break;
            }
        }
    }
    
    public static void main(String[] args) {
        Server server = new Server(6001);
        System.out.println("Server pokrenut i slusa na portu 6001");
        server.acceptClients();
    }
    
}
