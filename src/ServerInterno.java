

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.io.File;
import java.util.LinkedList;
import java.io.FileNotFoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ServerInterno extends UnicastRemoteObject implements InterfacciaInterno {

    private static int numSeggiVisitati = 0; //numero di seggi che hanno inviato i dati per le statistiche sull'affluenza
    private static double affluenzaIscrittiAiSeggi; //totale degli elettori dei seggi che hanno inviato i dati per l'affluenza
    private static double affluenzaElettoriVotato; // totale degli elettori che hanno votato, tra i seggi che hanno inviato i dati sull'affluenza
    private static double affluenzaElettoriVotatoCamera;// totale degli elettori che hanno votato per la camera, tra i seggi che hanno inviato i dati sull'affluenza
    private static double affluenzaElettoriVotatoSenato;// totale degli elettori che hanno votato per il senato, tra i seggi che hanno inviato i dati sull'affluenza
    private static double[] affluenzaElettoriVotatoReferendum;// totale degli elettori che hanno votato 1 o più referendum, tra i seggi che hanno inviato i dati sull'affluenza
    static LinkedList<ObjLista> listeCamera = new LinkedList<ObjLista>(); // Lista delle liste/coalizioni candidate alla camera
    static LinkedList<ObjLista> listeCameraFriuli = new LinkedList<ObjLista>(); // Lista delle liste/coalizioni candidate alla camera
    static LinkedList<ObjLista> listeCameraTrentino = new LinkedList<ObjLista>(); // Lista delle liste/coalizioni candidate alla camera
    static LinkedList<ObjLista> listeSenato = new LinkedList<ObjLista>();// Lista delle liste/coalizioni candidate al senato
    static LinkedList<ObjQuesito> referendum = new LinkedList<ObjQuesito>();// Lista dei quesiti referendari
    static LinkedList<ObjSeggio> seggi = new LinkedList<ObjSeggio>();// Lista dei seggi
    ArrayList<Integer> idSeggiVotiRicevuti = new ArrayList<Integer>();// Lista degli id dei seggi che hanno inviato i dati per il calcolo dei risultati finali
    static String idElezione = generaIdElezione(); //identificativo univoco per ogni Elezione
    int elezioniElettoriVotato = 0; //totale degli elettori che hanno votato, tra tutti i seggi che hanno inviato i dati per calcolare i risultati finali
    double elezioniElettoriVotatoFriuli = 0; //totale degli elettori che hanno votato, tra tutti i seggi che hanno inviato i dati per calcolare i risultati finali
    double elezioniElettoriVotatoTrentino = 0; //totale degli elettori che hanno votato, tra tutti i seggi che hanno inviato i dati per calcolare i risultati finali
    int elezioniIscrittiAiSeggi = 0;//totale degli elettori iscritti ai seggi, tra tutti i seggi che hanno inviato i dati per calcolare i risultati finali

    public ServerInterno() throws RemoteException {
    }

    /* Metodo che verifica le credenziali di un seggio che tenta di collegarsi al server
     * se il seggio viene riconosciuto, gli viene fornito un idElezione che serve
     * ad identificare univocamente le elezioni.
     * @author Alessandro Morsiani (Matr. 0000639120)
     */
    @Override
    public String Login(String regione, String IP, int ID, String seggioIdElezione) throws RemoteException {
        System.out.println("\nConnessione nuovo seggio in corso...");
        for (ObjSeggio seggio : seggi) {

            //if(!seggio.getConnesso()) 
            if (seggioIdElezione != null && seggioIdElezione == idElezione) {

                if (seggio.getRegione().equalsIgnoreCase(regione)
                        && seggio.getIP().equals(IP)
                        && ID == seggio.getID()) {//
                    seggio.setConnesso(true);//
                    System.out.println("\nSeggio " + seggio.getRegione() + " si e' riconnesso");
                }
            } else {
                if (seggio.getRegione().equalsIgnoreCase(regione)
                        && seggio.getIP().equals(IP)// 
                        && ID == seggio.getID()) {
                    seggio.setConnesso(true);//
                    seggio.setCodice(idElezione);
                    System.out.println("\nSeggio " + seggio.getRegione() + " connesso");
                    return idElezione;
                }

            }

        }
        return null;
    }

    /* Metodo che genera l'id per le elezioni in corso
     * @author Alessandro Morsiani (Matr. 0000639120)
     */
    public static String generaIdElezione() {

        Random r = new Random();
        String nuovoIdElezione = "";
        for (int i = 0; i < 10; i++) {
            char c = (char) (r.nextInt((int) (Character.MAX_VALUE)));
            nuovoIdElezione += c;
        }

        return nuovoIdElezione;
    }

    /* Metodo che somma i dati ricevuti dai seggi finche' tutti i seggi appartenenti al server
     * non inviano i propri dati, una volta che tutti i seggi hanno inviato i dati passa l'ora attuale
     * al metodo stampaAffluenza() per visualizzare a video le statistiche sull'affluenza all'ora x
     * @author Alessandro Morsiani (Matr. 0000639120), Roberto Squillace (Matr. 0000239924)
     */
    @Override
    public boolean affluenza(double iscrittiAlSeggio, double votantiSenato, double elettoriVotato, double elettoriVotatoCamera, double elettoriVotatoSenato, double[] elettoriVotatoReferendum) throws RemoteException {
        if (numSeggiVisitati < seggi.size()) {//
            affluenzaIscrittiAiSeggi += iscrittiAlSeggio;
            affluenzaElettoriVotato += elettoriVotato;
            affluenzaElettoriVotatoCamera += elettoriVotatoCamera;
            affluenzaElettoriVotatoSenato += elettoriVotatoSenato;

            for (int i = 0; i < elettoriVotatoReferendum.length; i++) {
                affluenzaElettoriVotatoReferendum[i] += elettoriVotatoReferendum[i];

            }
            numSeggiVisitati++;
        }


        return true;
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
    /* Metodo che si occupa di effettuare i dovuti controlli (es. divisione per zero) ed i calcoli sulle percentuali
     * d'affluenza e poi li stampa a video. Infine dopo la stampa viene richiamato il metodo resetAffluenza
     * per resettare i calcoli ed attendere nuovamente i dati da tutti i seggi.
     * @author Alessandro Morsiani (Matr. 0000639120), Roberto Squillace (Matr. 0000239924)
     */

    public static void stampaAffluenza(String orario) {

        if (numSeggiVisitati == seggi.size()) {


            if (affluenzaIscrittiAiSeggi > 0) {
                double percentualeAffluenza = arrotondamento((affluenzaElettoriVotato / affluenzaIscrittiAiSeggi) * 100);
                System.out.println("\nL'affluenza alle ore " + orario + " e' del " + percentualeAffluenza + "%");
            } else {
                System.out.println("\nL'affluenza alle ore " + orario + " e' dello 0%");
            }

            if (affluenzaElettoriVotatoCamera > 0) {
                double percentualeAffluenzaCamera = arrotondamento((affluenzaElettoriVotatoCamera / affluenzaElettoriVotato) * 100);
                System.out.println("\nL'affluenza alle ore " + orario + " per la camera e' del " + percentualeAffluenzaCamera + "%");
            } else {
                System.out.println("\nL'affluenza alle ore " + orario + " per la camera e' dello 0%");
            }

            if (affluenzaElettoriVotatoSenato > 0) {
                double percentualeAffluenzaSenato = arrotondamento((affluenzaElettoriVotatoSenato / affluenzaElettoriVotato) * 100);
                System.out.println("\nL'affluenza alle ore " + orario + " per il senato e' del " + percentualeAffluenzaSenato + "%");
            } else {
                System.out.println("\nL'affluenza alle ore " + orario + " per il senato e' dello 0%");
            }



            for (int i = 0; i < referendum.size(); i++) {
                if (affluenzaIscrittiAiSeggi > 0) {
                    double percentualeAffluenzaReferendum = arrotondamento((affluenzaElettoriVotatoReferendum[i] / affluenzaIscrittiAiSeggi) * 100);
                    System.out.println("\nL'affluenza alle ore " + orario + " per il referendum \" " + referendum.get(i).getTitolo() + " \" e' del: " + percentualeAffluenzaReferendum + "%");
                } else {
                    System.out.println("\nL'affluenza alle ore " + orario + " per il referendum \" " + referendum.get(i).getTitolo() + " \" e' dello 0%");
                }
            }



        }

        resetDatiAffluenza();
    }

    /* Metodo che azzera le strutture dati dedite al calcolo dell'affluenza
     * @author Alessandro Morsiani (Matr. 0000639120), Roberto Squillace (Matr. 0000239924)
     */
    public static void resetDatiAffluenza() {

        numSeggiVisitati = 0;
        affluenzaIscrittiAiSeggi = 0;
        affluenzaElettoriVotato = 0;
        affluenzaElettoriVotatoCamera = 0;
        affluenzaElettoriVotatoSenato = 0;
        for (int i = 0; i < affluenzaElettoriVotatoReferendum.length; i++) {
            affluenzaElettoriVotatoReferendum[i] = 0;
        }
    }

    /* Metodo che calcola e stampa a video i risultati finali delle elezioni
     * @author Alessandro Morsiani (Matr. 0000639120), Roberto Squillace (Matr. 0000239924)
     */
    public void stampaVoti() {
        LinkedList<ObjLista> listeCameraCompleta = new LinkedList<ObjLista>();
        listeCameraCompleta.addAll(listeCamera);
        listeCameraCompleta.addAll(listeCameraFriuli);
        listeCameraCompleta.addAll(listeCameraTrentino);
        Iterator<ObjLista> itCamera = listeCameraCompleta.iterator();
        Iterator<ObjLista> itSenato = listeSenato.iterator();
        Iterator<ObjQuesito> itRef = referendum.iterator();

        System.out.println("\n\n######## RISULTATI CAMERA ########");
        for (; itCamera.hasNext();) {
            ObjLista temp = itCamera.next();
            if (isCoalizione(temp.getNome())) {
                if (temp.getRegioneSeggio().equalsIgnoreCase("friuli") && arrotondamento(((double) temp.getVoti() / elezioniElettoriVotatoFriuli) * 100) >= 20.0) {
                    System.out.println("La coalizione del FRIULI " + temp.getNome()
                            + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% ed ENTRA ALLA CAMERA!");

                } else if (temp.getRegioneSeggio().equalsIgnoreCase("trentino") && arrotondamento(((double) temp.getVoti() / elezioniElettoriVotatoTrentino) * 100) >= 20.0) {
                    System.out.println("La coalizione del TRENTINO " + temp.getNome()
                            + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% ed ENTRA ALLA CAMERA!");

                } else if (arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100) >= 10.0) {
                    System.out.println("La coalizione " + temp.getNome()
                            + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% ed ENTRA ALLA CAMERA!");

                } else {
                    System.out.println("La coalizione " + temp.getNome()
                            + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% e NON entra alla camera!");
                }
            } else if (!isCoalizione(temp.getNome())) {
                if (temp.getRegioneSeggio().equalsIgnoreCase("friuli") && arrotondamento(((double) temp.getVoti() / elezioniElettoriVotatoFriuli) * 100) >= 20.0) {
                    System.out.println("La lista del FRIULI " + temp.getNome()
                            + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% ed ENTRA ALLA CAMERA!");

                } else if (temp.getRegioneSeggio().equalsIgnoreCase("trentino") && arrotondamento(((double) temp.getVoti() / elezioniElettoriVotatoTrentino) * 100) >= 20.0) {
                    System.out.println("La lista del TRENTINO " + temp.getNome()
                            + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% ed ENTRA ALLA CAMERA!");

                } else if (arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100) >= 4.0) {
                    System.out.println("La lista " + temp.getNome() + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% ed ENTRA ALLA CAMERA!");
                } else {
                    System.out.println("La lista " + temp.getNome() + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% e NON entra alla camera!");
                }
            }

        }
        System.out.println("\n\n######## RISULTATI SENATO ########");
        for (; itSenato.hasNext();) {
            ObjLista temp = itSenato.next();
            if (isCoalizione(temp.getNome())) {
                if (arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100) >= 20.0) {
                    System.out.println("La coalizione " + temp.getNome()
                            + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% ed ENTRA AL SENATO!");

                } else {
                    System.out.println("La coalizione " + temp.getNome()
                            + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% e NON entra al senato!");
                }
            } else {
                if (arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100) >= 8.0) {
                    System.out.println("La lista " + temp.getNome() + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% ed ENTRA AL SENATO!");
                } else {
                    System.out.println("La lista " + temp.getNome() + " ha ricevuto " + temp.getVoti() + " su " + elezioniElettoriVotato
                            + " quindi  ha preso il " + arrotondamento(((double) temp.getVoti() / elezioniElettoriVotato) * 100)
                            + "% e NON entra al senato!");
                }
            }
        }
        System.out.println("\n\n######## RISULTATI REFERENDUM ########");
        for (; itRef.hasNext();) {
            ObjQuesito temp = itRef.next();
            String esitoQuorum = "NON supera il quorum che si attesta al " + arrotondamento(((double) temp.getVotiSi() / elezioniIscrittiAiSeggi) * 100) + "%";

            if (arrotondamento(((double) temp.getVotiSi() / elezioniIscrittiAiSeggi) * 100) > 50.0) {
                esitoQuorum = "SUPERA il quorum che si attesta al " + arrotondamento(((double) temp.getVotiSi() / elezioniIscrittiAiSeggi) * 100) + "% !!!";
            }
            System.out.println("referendum \" " + temp.getTitolo() + " \" ha ricevuto "
                    + temp.getVotiSi() + " si, " + temp.getVotiNo() + " no, e " + temp.getVotiBianca() + " voti bianchi su " + elezioniIscrittiAiSeggi + " aventi diritto al voto e quindi " + esitoQuorum);
        }

    }

    /* Metodo richiamato dai seggi quando nel loro menu si seleziona l'opzione
     *" [C] Termina Election Day Ed Invia i Voti Al Server Centrale "
     * si occupa di verificare che il seggio non abbia già inviato i dati finali e
     * di sommare i voti ricevuti dal seggio, a quelli delle liste istanziate sul
     * server interno, in modo da raccogliere alla fine, tutti i voti di tutti gli elettori
     * di tutti i seggi, per ogni lista candidata alla camera ed al senato, e per ogni referendum
     * proposto. Per l'estrema delicatezza delle operazioni svolte, il metodo e' stato sincronizzato
     * per garantire la mutua esclusione tra thread concorrenti
     * 
     * @author Alessandro Morsiani (Matr. 0000639120), Roberto Squillace (Matr. 0000239924)
     */
    @Override
    public synchronized void calcolaVoti(int idSeggio, ArrayList votiSeggio, double elettoriVotato, int iscrittiAlSeggio) throws RemoteException {
        try {
            boolean esiste = false;


            Iterator<Integer> itSeggi = idSeggiVotiRicevuti.iterator();
            int id = itSeggi.next();
            if (id == idSeggio) {
                esiste = true;
                itSeggi.remove();

            }



            if (esiste) {

                if (idSeggiVotiRicevuti.size() >= 0) {
                    ObjLista[] cameraSeggio = (ObjLista[]) votiSeggio.get(0);
                    ObjLista[] senatoSeggio = (ObjLista[]) votiSeggio.get(1);
                    ObjQuesito[] referendumSeggio = (ObjQuesito[]) votiSeggio.get(2);

                    elezioniElettoriVotato += elettoriVotato;
                    elezioniIscrittiAiSeggi += iscrittiAlSeggio;
                    if (cameraSeggio[cameraSeggio.length - 1].getRegioneSeggio().equalsIgnoreCase("friuli")) {
                        elezioniElettoriVotatoFriuli = elettoriVotato;
                    }
                    if (cameraSeggio[cameraSeggio.length - 1].getRegioneSeggio().equalsIgnoreCase("trentino")) {
                        elezioniElettoriVotatoTrentino = elettoriVotato;
                    }
                    Iterator<ObjLista> itCamera = listeCamera.iterator();
                    Iterator<ObjLista> itSenato = listeSenato.iterator();
                    Iterator<ObjQuesito> itRef = referendum.iterator();
                     
                    for (; itCamera.hasNext();) {
                        ObjLista temp = itCamera.next();
                        for (ObjLista lista : cameraSeggio) {

                            if (temp.getID() == lista.getID()) {
                                temp.setVoti(lista.getVoti());
                            }
                        }
                    }

                    for (; itSenato.hasNext();) {
                        ObjLista temp = itSenato.next();
                        for (ObjLista lista : senatoSeggio) {

                            if (temp.getID() == lista.getID()) {
                                temp.setVoti(lista.getVoti());
                            }
                        }
                    }

                    for (; itRef.hasNext();) {
                        ObjQuesito temp = itRef.next();
                        for (ObjQuesito quesito : referendumSeggio) {

                            if (temp.getTitolo().equals(quesito.getTitolo())) {
                                temp.setVotiSi(quesito.getVotiSi());
                                temp.setVotiNo(quesito.getVotiNo());
                                temp.setVotiBianca(quesito.getVotiBianca());
                            }
                        }
                    }

                }

                if (idSeggiVotiRicevuti.size() < 1) {
                    stampaVoti();

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    /* Metodo che ritorna la lista dei candidati alla camera
     * @author Alessandro Morsiani (Matr. 0000639120)
     */

    @Override
    public ObjLista[] getListeCamera() throws RemoteException {
        if (listeCamera == null) {
            return null;
        }

        int elementi = listeCamera.size();
        ObjLista[] listeCameraArray = new ObjLista[elementi];
        listeCamera.toArray(listeCameraArray);

        return listeCameraArray;
    }

    /* Metodo che ritorna la lista dei candidati alla camera 
     * per la regione speciale Friuli
     * @author Alessandro Morsiani (Matr. 0000639120)
     */
    @Override
    public ObjLista[] getListeCameraFriuli() throws RemoteException {
        if (listeCamera == null && listeCameraFriuli == null) {
            return null;
        }


        LinkedList<ObjLista> temp1 = listeCamera;
        LinkedList<ObjLista> temp2 = listeCameraFriuli;

        temp1.addAll(temp2);
        int elementi = temp1.size();
        ObjLista[] listeCameraArray = new ObjLista[elementi];
        listeCamera.toArray(listeCameraArray);

        return listeCameraArray;
    }

    /* Metodo che ritorna la lista dei candidati alla camera 
     * per la regione speciale Trentino
     * @author Alessandro Morsiani (Matr. 0000639120)
     */
    @Override
    public ObjLista[] getListeCameraTrentino() throws RemoteException {
        if (listeCamera == null && listeCameraTrentino == null) {
            return null;
        }


        LinkedList<ObjLista> temp1 = listeCamera;
        LinkedList<ObjLista> temp2 = listeCameraTrentino;

        temp1.addAll(temp2);
        int elementi = temp1.size();
        ObjLista[] listeCameraArray = new ObjLista[elementi];
        listeCamera.toArray(listeCameraArray);

        return listeCameraArray;
    }

    /* Metodo che ritorna la lista dei candidati al senato
     * @author Alessandro Morsiani (Matr. 0000639120)
     */
    @Override
    public ObjLista[] getListeSenato() throws RemoteException {
        if (listeSenato == null) {
            return null;
        }

        int elementi = listeSenato.size();
        ObjLista[] listeSenatoArray = new ObjLista[elementi];
        listeSenato.toArray(listeSenatoArray);

        return listeSenatoArray;
    }

    /* Metodo che ritorna la lista dei quesiti referendari
     * @author Alessandro Morsiani (Matr. 0000639120)
     */
    @Override
    public ObjQuesito[] getListeReferendum() throws RemoteException {
        if (referendum == null) {
            return null;
        }

        int elementi = referendum.size();
        ObjQuesito[] listeReferendumArray = new ObjQuesito[elementi];
        referendum.toArray(listeReferendumArray);


        return listeReferendumArray;
    }

    /* Metodo che legge dal file di configurazione la lista dei seggi assegnati a questo server
     * e che sara' utilizzata per verificare le credenziali dei seggi che intendono loggarsi
     * @author Alessandro Morsiani (Matr. 0000639120)
     */
    private void inizializzaSeggi() {
        try {
            Scanner leggiRiga;


            Scanner leggiSeggi = new Scanner(new File("ListaSeggiInterno.conf"));

            while (leggiSeggi.hasNextLine()) {
                try {
                    leggiRiga = new Scanner(leggiSeggi.nextLine());

                    String regione = leggiRiga.next();
                    String ip = leggiRiga.next();
                    int id = Integer.parseInt(leggiRiga.next());
                    seggi.add(new ObjSeggio(regione, ip, id));

                } catch (NumberFormatException e) {
                    System.out.println("Errore nei dati di un seggio; " + e);
                }
            }
            leggiSeggi.close();
            for (ObjSeggio seggio : seggi) {
                idSeggiVotiRicevuti.add(seggio.getID());
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ServerInterno.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /* Metodo che inizializza il server leggendo i parametri di configurazione dal file di default
     * Effettua  il rebind della propria interfaccia per mettere a disposizione dei seggi (client)
     * i propri servizi (metodi)
     * @author Alessandro Morsiani (Matr. 0000639120)
     */
    private static ServerInterno inizializzaServer() {


        try {
            ServerInterno server = new ServerInterno();
            //Da specifiche ci vuole un apposito file per impostare la porta di ascolto del server
            Scanner leggiPorta = new Scanner(new File("PortaInterno.conf"));
            Registry reg = LocateRegistry.createRegistry(leggiPorta.nextInt());
            leggiPorta.close();
            reg.rebind("serverInterno", server);

            return server;
        } catch (RemoteException e) {
            System.out.println("Avvio fallito: " + e);
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Eccezione: " + e);
        }

        return null;
    }

    /* Metodo di supporto che individua se una lista forma o meno una coalizione di piu' liste
     * @author Alessandro Morsiani (Matr. 0000639120) @Roberto Squillace (Matr. 0000239924)
     */
    public static boolean isCoalizione(String riga) {
        int n = 0;
        String token = "+";
        for (int i = 0; i <= riga.length() - token.length(); i++) {
            String str = riga.substring(i, i + token.length());
            if (str.equals(token)) {
                n++;
            }
        }
        if (n > 0) {
            return true;
        }

        return false;
    }

    /* Metodo che inizializza le liste dei candidati e dei requisiti referendari
     * @author Alessandro Morsiani (Matr. 0000639120)
     */
    private void inizializzaListe() {
        try {


            Scanner leggiFile = new Scanner(new File("ListaListe.conf"));
            int idListeCamera = 0;
            while (leggiFile.hasNextLine()) {

                String riga = leggiFile.nextLine();

                if (riga.equalsIgnoreCase("CAMERA")) {

                    riga = leggiFile.nextLine();
                    String[] nomiListeCamera = riga.split(" ");

                    for (String nomeLista : nomiListeCamera) {
                        if (isCoalizione(nomeLista)) {
                            listeCamera.add(new ObjLista(nomeLista, idListeCamera, true));
                        } else {
                            listeCamera.add(new ObjLista(nomeLista, idListeCamera));
                        }
                        idListeCamera++;
                    }

                    listeCamera.add(new ObjLista("Scheda_Bianca", listeCamera.size()));
                }
                if (riga.equalsIgnoreCase("LISTE FRIULI")) {

                    riga = leggiFile.nextLine();
                    String[] nomiListeCamera = riga.split(" ");

                    for (String nomeLista : nomiListeCamera) {
                        idListeCamera++;
                        if (isCoalizione(nomeLista)) {
                            listeCameraFriuli.add(new ObjLista(nomeLista, idListeCamera, true, "friuli"));
                        } else {
                            listeCameraFriuli.add(new ObjLista(nomeLista, idListeCamera, "friuli"));
                        }

                    }
                }

                if (riga.equalsIgnoreCase("LISTE TRENTINO")) {

                    riga = leggiFile.nextLine();
                    String[] nomiListeCamera = riga.split(" ");

                    for (String nomeLista : nomiListeCamera) {
                        idListeCamera++;
                        if (isCoalizione(nomeLista)) {
                            listeCameraTrentino.add(new ObjLista(nomeLista, idListeCamera, "trentino"));
                        } else {
                            listeCameraTrentino.add(new ObjLista(nomeLista, idListeCamera, "trentino"));
                        }

                    }
                }

                if (riga.equalsIgnoreCase("SENATO")) {
                    int idListeSenato = 0;
                    riga = leggiFile.nextLine();
                    String[] nomiListeSenato = riga.split(" ");

                    for (String nomeLista : nomiListeSenato) {
                        listeSenato.add(new ObjLista(nomeLista, idListeSenato));
                        idListeSenato++;
                    }
                    listeSenato.add(new ObjLista("Scheda_Bianca", listeSenato.size()));
                }
                if (riga.equalsIgnoreCase("REFERENDUM")) {
                    int idQuesitiReferendum = 0;
                    while (leggiFile.hasNextLine()) {
                        riga = leggiFile.nextLine();
                        if (riga != "") {
                            referendum.add(new ObjQuesito(riga, idQuesitiReferendum));
                            idQuesitiReferendum++;
                        }
                    }
                    affluenzaElettoriVotatoReferendum = new double[referendum.size()];
                }

            }
            leggiFile.close();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(ServerInterno.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /* Metodo principale nel quale vengono richiamati tutti i metodi necessari all'inizializzazione
     * del server. Fornisce inoltre l'opzione per terminare l'esecuzione del server
     * @author Alessandro Morsiani (Matr. 0000639120)
     */
    public static void main(String[] args) {


        System.out.println("Avvio server in corso...");
        ServerInterno server = inizializzaServer();
        System.out.println("Inizializzazione Seggi...");
        server.inizializzaSeggi();
        System.out.println("Inizializzazione liste...");
        server.inizializzaListe();
        System.out.println("\nIl server e' stato avviato correttamente.\n");


        Scanner scan = new Scanner(System.in);
        String scelta;
        while (true) {

            System.out.println("################################################");
            System.out.println("# Benvenuto nella cabina elettorale            #");
            System.out.println("# [S] - Stampa le statistiche sull'affluenza   #");
            System.out.println("# [Q] - Arresta il sistema 'Cabina elettorale' #");
            System.out.println("################################################");
            scelta = scan.nextLine();
            if (scelta.equals("q")) {
                System.exit(0);
            }
            if (scelta.equals("s")) {
                if (numSeggiVisitati > 1) {


                    java.util.Calendar c = java.util.Calendar.getInstance();
                    String oraMinutiSecondi = c.get(java.util.Calendar.HOUR_OF_DAY) + ":" + c.get(java.util.Calendar.MINUTE);
                    stampaAffluenza(oraMinutiSecondi);

                } else {
                    System.out.println("\nI dati sull'affluenza  non sono al momento disponibili\n\n");
                }
            }
        }
    }
}
