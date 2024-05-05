package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReceiveMessageFromServer implements Runnable {
    
    
    Client parent;
    BufferedReader br;

    public ReceiveMessageFromServer(Client parent) {
        
        this.parent = parent;
        this.br = parent.getBr();
    }
    
    @Override
    public void run() {
        while(true) {
            try {
                String line;
                line = this.br.readLine();
                
                if (line.startsWith("Aktivan set je ")) { 
                    parent.setHelpsEnabled();
                }
                if (line.equals("Pogresno ime ili sifra! ")) {
                    parent.setLogInWindowVisible();
                }
                
                if(line.startsWith("Učesnici-")) {
                    parent.getCbUsers().removeAllItems();
                    if(line.split("- ").length >= 2) {
                        String[] imena = line.split("- ")[1].split(" ");
                        for (String ime : imena) {
                            if (!ime.equals("") && !ime.equals(parent.getClientUsername())) {
                                parent.getCbUsers().addItem(ime.trim());
                            }
                        }
                    }
                }
                else if(!line.startsWith("Dobrodosli ") || !parent.getClientRole().equals("admin")) {
                        parent.setTaQuestions(line);
                }
                
                    
            } catch (IOException ex) {
                Logger.getLogger(ReceiveMessageFromServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
}
