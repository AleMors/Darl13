
import java.rmi.server.UnicastRemoteObject;
import java.io.File;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger; 

/* Classe che Implementa il server seggio del sistema di votazioni online
 * @author Roberto Squillace (Matr. 0000239924)
 */
public class ServerSeggio extends UnicastRemoteObject implements InterfacciaSeggio {

    private static String regione; //Regione d'appartenenza del seggio
    private static String IP;     // Ip del seggio
    private static int ID;        // Id del seggio
    private String idElezione = ""; //Identificativo elettorale
    private static InterfacciaInterno serverInterno; // Riferimento allìinterfaccia del server interno
    private static ObjLista[] listeCamera; // Lista delle liste/coalizioni candidate alla camera
    private static ObjLista[] listeSenato; // Lista delle liste/coalizioni candidate al senato
    private static ObjQuesito[] referendum; // Lista dei quesiti referendari
    private static ArrayList<ObjCabina> listaCabineLoggate= new ArrayList<ObjCabina>(); //Lista cabine effettivamente loggate
    private static ArrayList<ObjCabina> listaCabineSeggio= new ArrayList<ObjCabina>(); //Lista cabine assegnate a questo seggio e teoricamente disponibili
    private static LinkedList<ObjElettore> iscrittiAlSeggio; //Lista degli elettori iscritti a questo seggio
    static ArrayList<String> hannoVotato = new ArrayList<String>(); //Lista dei documenti degli elettori iscritti a questo seggio che hanno votato
    private static double votantiSenato; //numero degli elettori iscritti a questo seggio in possesso dei requisiti per votare anche per il Senato
    private static double elettoriVotato;//numero degli elettori iscritti a questo seggio che hanno votato
    private static double elettoriVotatoCamera;//numero degli elettori iscritti a questo seggio che hanno votato per la Camera
    private static double elettoriVotatoSenato;//numero degli elettori iscritti a questo seggio che hanno votato per il Senato
    private static double[] elettoriVotatoReferendum;//numero degli elettori iscritti a questo seggio che hanno votato uno o più referendum
    private static Scanner  sceltaEx = new Scanner(System.in); //Scanner utilizzato per gestire le eccezioni
    

    
    public ServerSeggio() throws RemoteException {
        
    }

       /* Metodo che elimina una cabina dalla lista delle cabine loggate ciclando quest'ultima alla
        * ricerca dell'ID della cabina passato dalla cabina stessa durante l'invocazione di questo metodo
        * @author Lorenzo Guerzoni (Matr. 0000639838)
        */
       @Override
 
       public synchronized void logoutCabina(int ID) {
        for (Iterator<ObjCabina> it = listaCabineLoggate.iterator(); it.hasNext();) {
            ObjCabina temp = it.next();
            if (temp.getID() == ID) {
                it.remove();
                System.out.println("La Cabina " + temp.getID() + " si e' disconnessa");
            }
        }


    }
       
       
       /* Metodo che al termine della votazione di un elettore, la cabina richiama questo metodo per resettare
        * le impostazioni precedentemente settate in base alle scelte dell'elettore che ha appena 
        * terminato la votazione. L'oggetto passato come parametro rappresenta la cabina per il 
        * Server Seggio.
        * @author Lorenzo Guerzoni (Matr. 0000639838)
        */
       @Override
    public boolean lasciaCabina(ObjCabina cabina)   {
                   for (Iterator<ObjCabina> it = listaCabineLoggate.iterator(); it.hasNext();) {
            ObjCabina temp = it.next();
            if (temp.getID() == cabina.getID()) {
                temp.resetVotazioni();
                   temp.setElettore(null);
                   temp.setLibera(true);
                   return true;
            }
        }
           
           return false;
    }

    @Override
    /* Al termine della votazione di un elettore, la cabina richiama questo metodo per effettuare 
     * il salvataggio dei voti espressi dall'elettore. Esegue inoltre il backup delle strutture dati
     * che tengono traccia dei voti effettuati dagli elettori fino a questo momento, e la lista dei loro
     * documenti, nel caso fosse necessario ripristinare il sistema a seguito di un malfunzionamento.
     * @author Roberto Squillace (Matr. 0000239924)
     */
    public boolean salvaVotiElettore(int[] risultati, ObjCabina cabina) {
        
        if (risultati[0] != -1) {synchronized(listeCamera){listeCamera[risultati[0]].incrementaVoti(); elettoriVotatoCamera++;}}
        if (risultati[1] != -1) {synchronized(listeSenato){listeSenato[risultati[1]].incrementaVoti(); elettoriVotatoSenato++;}}
        
        for (int i = 2; i < risultati.length; i++) {
                if (risultati[i] != -1) {
                    int indexRef=i-2;
                        switch (risultati[i]) {
                            case 0:
                               synchronized(referendum){referendum[indexRef].votatoNo();}
                                elettoriVotatoReferendum[indexRef]++;
                                break;
                            case 1:
                                synchronized(referendum){referendum[indexRef].votatoSi();}
                                elettoriVotatoReferendum[indexRef]++;
                                break;
                            case 2:
                                synchronized(referendum){referendum[indexRef].votatoBianca();}
                                
                                elettoriVotatoReferendum[indexRef]++;
                                break;
                            default:
                                break;
                        }
                }
        }
        
            
        if(backupVotazioneElettori(cabina.getElettore().getDocumento())){
        lasciaCabina(cabina);
        return true;
        }
        
        
        return false;
    }
    
    /* Metodo di supporto che individua se una lista forma o meno una coalizione di piu' liste
     * @author Alessandro Morsiani (Matr. 0000639120), Roberto Squillace (Matr. 0000239924)
     */
  public static boolean isCoalizione(String riga) {
    int n = 0;
    String token="+";
    for(int i = 0; i <= riga.length()-token.length(); i++) {
        String str = riga.substring(i, i+token.length());
        if(str.equals(token))
            n++;
    }
    if(n > 0)
        return true;
    
    return false;
 }
  
    /* Metodo che effettua il ripristino (su richiesta) dei dati salvati su file dal metodo
     * backupVotazioneElettori()
     * @author Roberto Squillace (Matr. 0000239924)
     */
    private static boolean ripristinaBackup(){
         try {
               
                
                Scanner leggiBak = new Scanner(new File("VotazioniSeggio.bak"));		
                            int idListeCamera = 0;
                           while(leggiBak.hasNextLine()){
                                    
                                   String riga = leggiBak.nextLine();
                                    
                                    if (riga.equalsIgnoreCase("HANNO VOTATO")) {
                                        
                                        String[] docList = leggiBak.nextLine().split(" ");
                                        for(String doc : docList)
                                            hannoVotato.add(doc);
                                   }
                                    
                                   if (riga.equalsIgnoreCase("CAMERA")) {
                                        while(leggiBak.hasNextLine()){
                                            riga = leggiBak.nextLine();
                                            if(riga.equals(" ")) {break;}
                                            else{
                                                String[] Lista = riga.split(" ");
                                                for(ObjLista camera : listeCamera)
                                                    if(camera.getNome().equals(Lista[0]))
                                                        camera.setVoti(Integer.parseInt(Lista[1]));
                                            }
                                        }
                                    }
                                   
                                   if (riga.equalsIgnoreCase("SENATO")) {
                                        while(leggiBak.hasNextLine()){
                                            riga = leggiBak.nextLine();
                                            if(riga.equals(" ")) {break;}
                                            else{
                                                String[] Lista = riga.split(" ");
                                                for(ObjLista senato : listeSenato)
                                                    if(senato.getNome().equals(Lista[0]))
                                                        senato.setVoti(Integer.parseInt(Lista[1]));
                                            }
                                        }
                                    }
                                   
                                   if (riga.equalsIgnoreCase("REFERENDUM")) {
                                        while(leggiBak.hasNextLine()){
                                            riga = leggiBak.nextLine();
                                            if(riga.equals("")) {break;}
                                            else{
                                                String[] ref = riga.split("#");
                                                for(ObjQuesito quesito : referendum)
                                                    if(quesito.getTitolo().equals(ref[0])){
                                                        String[] voti = ref[1].split(" ");
                                                        quesito.setVotiSi(Integer.parseInt(voti[0]));
                                                        quesito.setVotiNo(Integer.parseInt(voti[1]));
                                                        quesito.setVotiBianca(Integer.parseInt(voti[2]));
                                                    }
                                            }
                                        }
                                    }
                           }
                           System.out.println("Backup Ripristinato con successo!\n VERIFICA:\n\nCAMERA");
                           for(ObjLista lista : listeCamera)
                               System.out.println(lista.getNome()+" "+lista.getVoti());
                           System.out.println("\n\nSENATO");
                           for(ObjLista lista : listeSenato)
                               System.out.println(lista.getNome()+" "+lista.getVoti());
                           System.out.println("\n\nREFERENDUM");
                           for(ObjQuesito lista : referendum)
                               System.out.println(lista.getTitolo()+" si:"+lista.getVotiSi()+" no:"+lista.getVotiNo()+" bianca:"+lista.getVotiBianca());
                           
         } catch (FileNotFoundException ex) {
           System.out.println("Non e' stato possibile leggere il file di backup, controllarne l'esistenza e la correttezza");
        }
    return false;
    }
    /*Metodo di supporto al metodo salvaVotiElettore() che si occupa nello specifico del salvataggio
     *della struttura dati che mantiene in memoria i voti fin'ora espressi dagli elettori iscritti a questo
     * seggio
     * @author Roberto Squillace (Matr. 0000239924)
     */
    private  static boolean backupVotazioneElettori(String documento) {
 
        try {
            
            hannoVotato.add(documento);
            elettoriVotato=hannoVotato.size();
            File bakFile = new File("VotazioniSeggio.bak");
            if(bakFile.exists())
                bakFile.delete();
            PrintStream scriviBak = new PrintStream(bakFile);
            
            scriviBak.println("HANNO VOTATO");
            for (String docElettore : hannoVotato) 
                scriviBak.print(docElettore + " ");
            
            scriviBak.println(" ");
            scriviBak.println(" ");
            
            scriviBak.println("CAMERA");
            for (ObjLista lista : listeCamera) 
                scriviBak.println(lista.getNome() + " " + lista.getVoti());
            
            scriviBak.println(" ");
            
            scriviBak.println("SENATO");
            for (ObjLista lista : listeSenato) 
                scriviBak.println(lista.getNome() + " " + lista.getVoti());
            
            scriviBak.println(" ");
            
            scriviBak.println("REFERENDUM");
            for (ObjQuesito quesito : referendum) 
                scriviBak.println(quesito.getTitolo() + "#" + quesito.getVotiSi() + " " + quesito.getVotiNo() + " " + quesito.getVotiBianca());
            

            scriviBak.close();
            
            try{serverInterno.affluenza(iscrittiAlSeggio.size(), votantiSenato, elettoriVotato, elettoriVotatoCamera, elettoriVotatoSenato, elettoriVotatoReferendum);
            } catch (RemoteException ex) {
              System.out.println("Si e' verificato un errore di comunicazione con il server centrale durante l'invio dei dati sull'affluenza.");
            }
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("Uno o più file di backup risultano non leggibili.");
        }
        return false;

    }
    /* Metodo utilizzato dalle cabine per ricevere la lista delle liste candidate alla camera
     * @author Roberto Squillace (Matr. 0000239924)
     */
    @Override
    public ObjLista[] getListeCamera() throws RemoteException {
        return listeCamera;
    }

    /* Metodo utilizzato dalle cabine per ricevere la lista delle liste candidate al senato
     * @author Roberto Squillace (Matr. 0000239924)
     */
    @Override
    public ObjLista[] getListeSenato() throws RemoteException {
        return listeSenato;
    }

    /* Metodo utilizzato dalle cabine per ricevere la lista dei quesiti referendari
     * @author Roberto Squillace (Matr. 0000239924)
     */
    @Override
    public ObjQuesito[] getReferendum() throws RemoteException {
        return referendum;
    }
    
    /* Metodo di supporto per la ricerca della posizione (indice) dell'oggetto Elettore all'interno
     * della lista degli oggetti Elettore (iscrittiAlSeggio) che rappresentano gli elettori iscritti
     * al seggio.
     * @author Roberto Squillace (Matr. 0000239924)
     */
    private static int cercaElettore(String documento) {

        for (ObjElettore temp : iscrittiAlSeggio) {
            
            if (documento.equals(temp.getDocumento())) 
                return iscrittiAlSeggio.indexOf(temp);
        }
        
        return -1;
    }
 
    /* Metodo che inizializza il seggio leggendo i parametri di configurazione dal file di default
     * o da un file passato come parametro da console. Effettua inoltre il lockup del server interno
     * ed il rebind della propria interfaccia per stabilire le connessioni con il server interno e per
     * mettere a disposizione delle cabine (client) i propri servizi (metodi)
     * @author Roberto Squillace (Matr. 0000239924)
     */
    private void inizializzaSeggio(String confFile) {
        try {
            Scanner datiServer = new Scanner(new File(confFile));
            regione = datiServer.nextLine();
            IP = datiServer.nextLine();
            ID = Integer.parseInt(datiServer.nextLine());
            int portaSeggio = Integer.parseInt(datiServer.nextLine());
             
            
            //Connessione ServerInterno <-> Seggio
            String host = datiServer.nextLine();
            int porta = Integer.parseInt(datiServer.nextLine());
            datiServer.close();
            Registry reg = LocateRegistry.getRegistry(host, porta);
            serverInterno =(InterfacciaInterno) reg.lookup("serverInterno");
            
            //Connessione Seggio <-> Cabina
            ServerSeggio server = new ServerSeggio();
            Registry regClient = LocateRegistry.createRegistry(portaSeggio);
            regClient.rebind("ServerSeggio", server);
            idElezione = serverInterno.Login(regione, IP, ID, idElezione);
             
             
             
        } catch (FileNotFoundException ex) {
            System.out.println("Si e' verificato un errore durante la lettura del file di configurazione di questo seggio. Verificarne la presenza e la correttezza");
        } catch (RemoteException ex) {
             
            String scelta="";
            
            do{
                System.out.println("Si e' verificato un errore di connessione al server centrale, riprovare? [S,N]");
                scelta= sceltaEx.nextLine();
                if(scelta.equalsIgnoreCase("s")){inizializzaSeggio("Seggio.conf");}
            }
            while(!scelta.equalsIgnoreCase("s") && !scelta.equalsIgnoreCase("n"));
        } catch (NotBoundException ex) {
            Logger.getLogger(ServerSeggio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /* Metodo di supporto che permette di tagliare al secondo decimale le percentuali di risultati finali ed
     * affluenza calcolate
     * @author Alessandro Morsiani (Matr. 0000639120), Roberto Squillace (Matr. 0000239924)
     */
    public static double arrotondamento(double x) {
        x = Math.floor(x * 100);
        x = x / 100;
        return x;
    }
          
    /* Metodo che inizializza le liste dei candidati e dei requisiti referendari
     * @author Roberto Squillace (Matr. 0000239924)
     */
    private void inizializzaListe() {
        try {
            if(!regione.equalsIgnoreCase("Friuli") && !regione.equalsIgnoreCase("Trentino")){listeCamera = serverInterno.getListeCamera();}
            else if(regione.equalsIgnoreCase("Friuli")){listeCamera = serverInterno.getListeCameraFriuli();}
            else if(regione.equalsIgnoreCase("Trentino")){listeCamera = serverInterno.getListeCameraTrentino();}
            listeSenato = serverInterno.getListeSenato();
            referendum = serverInterno.getListeReferendum();
            elettoriVotatoReferendum = new double[referendum.length];
            
        } catch (RemoteException ex) {
            System.out.println("Si e' verificato un errore durante la richieste delle liste dei candidati al server centrale.");
        } catch (NullPointerException ex) {System.out.println("Si e' verificato un errore durante la richieste delle liste dei candidati al server centrale.");}

    }
    /* Metodo che verifica le credenziali di una cabina (client) che prova a loggarsi al seggio
     * se la verifica ha successo la cabina viene inserita nella lista delle cabine loggate
     * (listaCabineLoggate)e messa a disposizione degli elettori.
     * @author Lorenzo Guerzoni (Matr. 0000639838)
     */
    @Override
    
    public synchronized boolean loginCabina(String IP, int ID) {
        Iterator<ObjCabina> it = listaCabineSeggio.iterator();
        Iterator<ObjCabina> it2 = listaCabineLoggate.iterator();
        boolean esiste = false;
        for (; it.hasNext();) {
            ObjCabina temp = it.next();
            if (temp.getIP().equals(IP) && temp.getID() == ID) {

                for (; it2.hasNext();) {
                    ObjCabina temp2 = it2.next();
                    if (temp.getID() == temp2.getID()) {
                        esiste = true;
                    }
                }
                if (!esiste) {
                    listaCabineLoggate.add(temp) ;
                        return true;
                    }
                }
            }
            return false;
        }
    /*Metodo che legge dal file di configurazione la lista delle cabine assegnate a questo seggio
     * e che sara' utilizzata per verificare le credenziali delle cabine che intendono loggarsi.
     * @author Lorenzo Guerzoni (Matr. 0000639838)
     */
    private void inizializzaCabine() {
        try {
           
            
                    Scanner datiCabina = new Scanner(new File("cabine.conf"));
            
            while (datiCabina.hasNextLine()) {
                try {
                    String info = datiCabina.nextLine();
                    Scanner leggiInfo = new Scanner(info);
                    String IPCabina = leggiInfo.next();
                    int id = Integer.parseInt(leggiInfo.next());
                    listaCabineSeggio.add(new ObjCabina(IPCabina, id, true));
                } catch (NumberFormatException e) {
                    System.out.println("Si e' verificato un errore durante la lettura dei parametri di configurazione della cabina.");
                }
            }
            datiCabina.close();
        } catch (FileNotFoundException ex) {
          System.out.println("Impossibile trovare il file di configurazione della cabina, si prega di verificarne la presenza e la correttezza.");
        }
    }

    /* Metodo extra (non richiesto dalle specifiche) che effettua il calcolo e la stampa 
     * delle statistiche sull'affluenza calcolate sui dati del singolo seggio .
     * @author Roberto Squillace (Matr. 0000239924)
     */
    public static void stampaAffluenza() {
       
        if (iscrittiAlSeggio.size() > 0) {
          
                double percentualeAffluenza = arrotondamento((elettoriVotato / iscrittiAlSeggio.size()) * 100);
                System.out.println("\nL'affluenza e' del " + percentualeAffluenza + "%");
        
        } else {
            System.out.println("\nL'affluenza e' dello 0%");
        }

        if (elettoriVotato > 0) {
            double percentualeAffluenzaCamera;
         
                percentualeAffluenzaCamera = arrotondamento((elettoriVotatoCamera / elettoriVotato) * 100);

                System.out.println("\nL'affluenza per la camera e' del " + percentualeAffluenzaCamera + "%");

            
        } else {
            System.out.println("\nL'affluenza per la camera e' dello 0%");
        }

        if (elettoriVotato > 0) {
            double percentualeAffluenzaSenato;
             
                percentualeAffluenzaSenato = arrotondamento((elettoriVotatoSenato / elettoriVotato) * 100);
                System.out.println("\nL'affluenza per il senato e' del " + percentualeAffluenzaSenato + "%");
             

        } else {
            System.out.println("\nL'affluenza per il senato e' dello 0%");
        }



        for (int i = 0; i < referendum.length; i++) {
            if (iscrittiAlSeggio.size() > 0) {
                double percentualeAffluenzaReferendum;
                 
                    percentualeAffluenzaReferendum = arrotondamento((elettoriVotatoReferendum[i] / iscrittiAlSeggio.size()) * 100);
                    System.out.println("\nL'affluenza per il referendum \" " + referendum[i].getTitolo() + " \" e' del: " + percentualeAffluenzaReferendum + "%");
               

            } else {
                System.out.println("\nL'affluenza per il referendum \" " + referendum[i].getTitolo() + " \" e' dello 0%");
            }
        }

    }
    
    /* Metodo che legge la lista degli elettori iscritti a questo seggio e ne crea una rappresentazione
     * tramite l'utilizzo della classe ObjElettore. Calcola inoltre il numero degli elettori che possiedono
     * i requisiti per votare per il senato.
     * @author Roberto Squillace (Matr. 0000239924)
     */
    private void inizializzaElettori() {
        try {

            Scanner leggiElettori = new Scanner(new File("Elettori.conf"));
            iscrittiAlSeggio = new LinkedList<ObjElettore>();
            while (leggiElettori.hasNextLine()) {
                Scanner leggiRiga = new Scanner(leggiElettori.nextLine());
                String nome = leggiRiga.next();
                String cognome = leggiRiga.next();
                int eta = leggiRiga.nextInt();
                String documento = leggiRiga.next();
                if (eta >= 25) {
                    votantiSenato++;
                }
                iscrittiAlSeggio.add(new ObjElettore(nome, cognome, eta, documento));
                
                

            }
             
            leggiElettori.close();
             
            
        } catch (FileNotFoundException ex) {
            System.out.println("Non e' stato possibile leggere il file contenente gli iscritti a questo seggio, si prega di verificarne la presenza e la correttezza.");
        }
    }

    /* Metodo che mostra a video la lista e le generalità di tutti gli iscritti a questo seggio 
     * @author Roberto Squillace (Matr. 0000239924)
     */
    private static void stampaElettoriIscritti() {
        System.out.println("######## ISCRITTI A QUESTO SEGGIO ########");
        for (ObjElettore elettore : iscrittiAlSeggio) {
            System.out.println(elettore.getNome() + " " + elettore.getCognome() + " " + elettore.getEta()+" "+elettore.getDocumento());
        }
        System.out.println("##########################################");
    }
    /* Metodo che verifica se l'elettore presentatosi per votare, non abbia gia' votato
     * @author Roberto Squillace (Matr. 0000239924)
     */
    public  static boolean elettoreHaVotato(String documento){
    
        if(hannoVotato != null)
        for(String elettore : hannoVotato)
            if(elettore.equals(documento))
                return true;
        return false;
    }
    /* Metodo principale, cuore della classe, si occupa di inizializzare il seggio e le strutture dati
     * utilizzate e mostra allo scrutatore/presidente di seggio il menu' con le operazioni consentite.
     * @author Roberto Squillace (Matr. 0000239924)
     */
    public static void main(String[] args) {
         
        try {

            ServerSeggio seggio = new ServerSeggio();
            if (args.length == 0) {
                 seggio.inizializzaSeggio("Seggio.conf"); 
              
            } else {
                seggio.inizializzaSeggio(args[0]);
            }
            seggio.inizializzaListe();
            seggio.inizializzaElettori();
            seggio.inizializzaCabine();
            stampaElettoriIscritti();
 
            Scanner inputSeggio = new Scanner(System.in);
            
            System.out.println();
            File bakFile = new File("VotazioniSeggio.bak");
            if(bakFile.exists()){
                String choise ="";
             do {
                                    System.out.println("\n Desidera ripristinare i dati di backup? [S/N]");
                                    choise = inputSeggio.nextLine();
                                      
                                } while (!choise.equalsIgnoreCase("s") && !choise.equalsIgnoreCase("n"));
                                
                                if(choise.equalsIgnoreCase("s")){ripristinaBackup();} 
                                if(choise.equalsIgnoreCase("n")){bakFile.delete();} 
            }
             
            System.out.println();
            String scelta = "";
            String conferma = "";
            
 
                
                
                while(!scelta.equalsIgnoreCase("D")){
                    
                System.out.println("\n###################### MENU PRINCIPALE ########################");
                System.out.println("#                                                             #");
                System.out.println("# [A] Accettazione Elettore Per la Votazione                  #");
                System.out.println("# [B] Stampa Statistiche Di Affluenza Al Seggio               #");
                System.out.println("# [C] Termina Election Day Ed Invia i Voti Al Server Centrale #");
                System.out.println("#                                                             #");
                System.out.println("###############################################################");
                
                    scelta = inputSeggio.nextLine();
                    
                    //[A] Accettazione Elettore Per la Votazione
                    if(scelta.equalsIgnoreCase("A")){
                        
                        System.out.println("Inserire documento elettore:");
                        String documento = inputSeggio.nextLine();
                        if(elettoreHaVotato(documento)){
                        System.out.println("L'elettore con documento "+documento+" ha gia' votato");
                        } else {
                            
                            int indiceElettore = cercaElettore(documento);
                            String[] votazioniDaEffettuare = new String[2+referendum.length];
                            
                        if (indiceElettore != -1) {
                        
                                ObjElettore elettore = iscrittiAlSeggio.get(indiceElettore);
                                String nome = elettore.getNome();
                                String cognome = elettore.getCognome();
                                int eta = elettore.getEta();
                                
                                if(listeCamera != null && listeCamera.length > 0){
                                    
                                    while(!conferma.equalsIgnoreCase("s") && !conferma.equalsIgnoreCase("n")){
                                        System.out.println("L'elettore \""+cognome+" "+nome+" accetta la scheda per le votazioni della Camera? [S, N]");
                                        conferma = inputSeggio.nextLine();
                                    }
                                    if(conferma.equalsIgnoreCase("s")){conferma="";  votazioniDaEffettuare[0]="s";}
                                    if(conferma.equalsIgnoreCase("n")){conferma="";  votazioniDaEffettuare[0]="n";}
                                
                                }
                                
                                if(listeSenato != null && listeSenato.length > 0 && eta >= 25){
                                    
                                    while(!conferma.equalsIgnoreCase("s") && !conferma.equalsIgnoreCase("n")){
                                        System.out.println("L'elettore \""+cognome+" "+nome+" accetta la scheda per le votazioni del Senato? [S, N]");
                                        conferma = inputSeggio.nextLine();
                                    }
                                    if(conferma.equalsIgnoreCase("s")){conferma=""; votazioniDaEffettuare[1]="s";}
                                    if(conferma.equalsIgnoreCase("n")){conferma=""; votazioniDaEffettuare[1]="n";}
                                
                                } else {conferma=""; votazioniDaEffettuare[1]="n";}
                                
                                if(referendum != null && referendum.length > 0){
                                    
                                  
                                    for(int x=0; x< referendum.length; x++){
                                       
                                       
                                        while(!conferma.equalsIgnoreCase("s") && !conferma.equalsIgnoreCase("n")){
                                            System.out.println("L'elettore \""+cognome+" "+nome+" accetta la scheda per votare il quesito referendario \" " + referendum[x].getTitolo() + " \" ? [S,N]");
                                            conferma = inputSeggio.nextLine();
                                        } 
                                        if(conferma.equalsIgnoreCase("s")){conferma="";  votazioniDaEffettuare[2+x]="s";}
                                        if(conferma.equalsIgnoreCase("n")){conferma="";  votazioniDaEffettuare[2+x]="n";}
                                    }
                                }
                                 
                                System.out.println("\n Attendere l'assegnazione di una cabina disponibile. L'operazione potrebbe richiedere qualche minuto...");
                                 int idCabina = trovaCabinaDisponibile(votazioniDaEffettuare, elettore);
                                System.out.println("L'elettore "+cognome+" "+nome+" puo' procedere alla votazione nella cabina numero " +idCabina+"\n\n");
                                    
                                    
                                 
                                    
                                 }
                                
                                
                            }

                        }


                    
                    //[B] Stampa Statistiche Di Affluenza Al Seggio
                    if(scelta.equalsIgnoreCase("B")){
                        
                                             stampaAffluenza();
                    
                    }
                    
                    //[B] Stampa Statistiche Di Affluenza Al Seggio
                    if(scelta.equalsIgnoreCase("C")){
                        
                      ArrayList votazioneConclusa = new ArrayList();
                      votazioneConclusa.add(listeCamera);
                      votazioneConclusa.add(listeSenato);
                      votazioneConclusa.add(referendum);
                    serverInterno.calcolaVoti(ID, votazioneConclusa, elettoriVotato, iscrittiAlSeggio.size());
                    System.exit(0);
                    }
                
                }

                System.exit(0);

        } catch(ConnectException e){
        System.out.println("Non e' stato possibile connettersi al Server centrale, si prega di controllare la connessione e riporivare");
        } catch (RemoteException ex) {
            
        } {
            System.out.println("Si e' verificato un errore durante la comunicazione con il server centrale.");
             
        }

    }

 
    /* Metodo che si occupa di trovare tra le cabine loggate, una cabina NON occupata da un elettore.
     * Tale cabina sara' assegnata all'elettore che in questo momento sta chiedendo di votare.
     * @author Lorenzo Guerzoni (Matr. 0000639838)
     */
    private static int trovaCabinaDisponibile(String[] votazioniDaEffettuare, ObjElettore elettore) {
       do{for(ObjCabina cabina : listaCabineLoggate)
           if(cabina.getLibera()){
               cabina.setLibera(false);
               cabina.setVotazioni(votazioniDaEffettuare);
               cabina.setElettore(elettore);
               return cabina.getID();
           }
       } while(true);
    }
    
    /* Metodo utilizzato dalle cabine loggate (client) per attendere e controllare
     * che un elettore sia assegnato ad essa per effettuare la votazione
     * @author Roberto Squillace (Matr. 0000239924)
     */
    @Override
    public   ObjCabina attesaElettore(int ID) {
        for(ObjCabina cabina : listaCabineLoggate)
           if(!cabina.getLibera() && cabina.getID() == ID)
               return cabina;
        return null;
    }
    
   
}
