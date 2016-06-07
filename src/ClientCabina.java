
import java.rmi.registry.*;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/* Classe che Implementa il client cabina del sistema di votazioni online
 * @author Lorenzo Guerzoni (Matr. 0000639838)
 */
public class ClientCabina
{
    public static void main(String[] args)
    {
        try
        {  //Lettura dei parametri di configurazione e lockup del Seggio a cui la cabina e' assegnata
            Scanner fromServer = new Scanner(new File("cabina.conf"));
            
            String host = fromServer.nextLine();
            int porta = fromServer.nextInt();

            Registry reg = LocateRegistry.getRegistry(host, porta);
            seggioElettorale = (InterfacciaSeggio) reg.lookup("ServerSeggio");

            Scanner daTastiera = new Scanner(System.in);
            
            indirizzoIp = fromServer.next();
            idCabina = Integer.parseInt(fromServer.next());
            
            String choise = ""; // Stringa per gestire l'input dell'elettore riguardo le opzioni del menu o altre domande
            String nomeElettore = ""; //Stringa per stampare "cognome nome" elettore
          
            //Array di stringhe che conterrà le scelte riguardo le votazioni che l'elettore 
            //ha scelto di fare in fase di registrazione (accettazione o meno delle schede)
            // all' indice 0 (votazioniDaEffettuare[0]) c'è la scelta riguardo la camera,
            // all' indice 1 quella riguardo il senato
            // dalla seconda in poi quella effettuata per ogni referendum
            String [] votazioniDaEffettuare; 
            
            // vedi commenti al metodo loginCabina in serverSeggio
            boolean loggato = seggioElettorale.loginCabina(indirizzoIp, idCabina); 
            
            while (true)
            {
                if (loggato)
                {
                    System.out.println("################################################");
                    System.out.println("# Benvenuto nella cabina elettorale            #");
                    System.out.println("# [S] - Procedi con la votazione               #");
                    System.out.println("# [Q] - Arresta il sistema 'Cabina elettorale' #");
                    System.out.println("################################################");
                    
                    choise = daTastiera.nextLine();
                    
                    if (choise.equalsIgnoreCase("s"))
                    {
                        //Fino a che la cabina ha un elettore da far votare
                        while ((cabinaElettorale = seggioElettorale.attesaElettore(idCabina)) != null)
                        {
                            //Prendo le liste dei candidati ed il riferimento all'oggetto che 
                            //rappresenta l'elettore entrato in cabina
                            listeCamera = seggioElettorale.getListeCamera();
                            listeSenato = seggioElettorale.getListeSenato();
                            
                            elettore = cabinaElettorale.getElettore();
                            
                            referendum = seggioElettorale.getReferendum();
                            
                            votazioniDaEffettuare = cabinaElettorale.getVotazioni();
                            //outputs conterra' i voti espressi dall'elettore, rappresentati 
                            //dagli indici che le liste o referendum hanno nella propria lista
                            outputs = new int[votazioniDaEffettuare.length];
                            
                            nomeElettore = elettore.getCognome() + " " + elettore.getNome();

                            System.out.println("Salve " + nomeElettore + "!");
                            //scanner che si occupa solo dei voti espressi dall'elettore
                            Scanner votoElettore = new Scanner(System.in);
                            
                            //Per ogni scelta effettuata dall'elettore in fase di registrazione 
                            //(rivedi commento alla dichiarazione di votazioniDaEffettuare)
                            for (int i=0; i<votazioniDaEffettuare.length; i++)
                            {
                                if (i == 0 && votazioniDaEffettuare[i].equals("s"))
                                {
                                    System.out.println("Di seguito, le liste candidate alla Camera dei Deputati:");
                                    
                                    for (ObjLista lista : listeCamera)
                                    {
                                        System.out.println("[" + lista.getID() + "] " + lista.getNome());
                                    }
                                    
                                    System.out.println("Digiti il numero corrispondente alla lista scelta.");
                                    
                                    int votazione;
                                    //chiedo d'inserire il numero corrispondente finche' non ne viene inserito 
                                    //uno valido
                                    do
                                    {
                                        votazione = votoElettore.nextInt();
                                    } while (votazione<0 || votazione>listeCamera.length - 1);

                                    outputs[i] = votazione;
                                }// se si e' scelto di non votare per la camera, setto -1 per indicarlo
                                else if (i == 0 && votazioniDaEffettuare[i].equals("n"))
                                {
                                    outputs[i] = -1;
                                }  
                                else if (i == 1 && votazioniDaEffettuare[i].equals("s"))
                                {
                                    System.out.println("Di seguito, le liste candidate al Senato della Repubblica:");
                                    
                                    for (ObjLista lista : listeSenato)
                                    {
                                        System.out.println("[" + lista.getID() + "] " + lista.getNome());
                                    }
                                    
                                    System.out.println("Digiti il nome della lista scelta.");
                                    
                                    int votazione2;
                                    
                                    do
                                    {
                                        votazione2 = votoElettore.nextInt();

                                    } while (votazione2 < 0 || votazione2 > listeSenato.length - 1);

                                    outputs[i] = votazione2;
                                }//se si e' scelto di non votare per il senato, lo segno con -1
                                else if (i == 1 && votazioniDaEffettuare[i].equals("n"))
                                {
                                    outputs[i] = -1;
                                }//Per ogni votazione che l'elettore ha scelto di fare, chiedo di 
                                //votare per il referendum corrispondente.
                                else if (i >= 2 && votazioniDaEffettuare[i].equals("s"))
                                {
                                    ObjQuesito quesito = referendum[i - 2];

                                    try
                                    {
                                        System.out.println("Quesito referendario: \" " + quesito.getTitolo() + " \"");
                                        System.out.println("[0] No");
                                        System.out.println("[1] Si");
                                        System.out.println("[2] Bianco");
                                        
                                        int votoR;
                                        
                                        do
                                        {
                                            votoR = votoElettore.nextInt();
                                        } while (votoR < 0 || votoR > 2);
                                        //salvo la scelta sul referendum (1=SI, 0=NO, 2=bianco)
                                        if (votoR == 1) outputs[i] = 1;
                                        else if (votoR == 0) outputs[i] = 0;
                                        else outputs[i] = 2;
                                    }
                                    catch (Exception e)
                                    {
                                        seggioElettorale.logoutCabina(idCabina);
                                         System.out.println("Si e' verificato un errore durante le votazioni dei quesiti referendari, richieda assistenza al personale");
                                    }
                                }//se  si e' scelto di non votare il referendum lo segno con -1
                                else if (i >= 2 && votazioniDaEffettuare[i].equals("n"))
                                {
                                    outputs[i] = -1;
                                }
                            }
                            //Terminata la votazione propongo il riepilogo dei voti e conferma
                            try {
                                System.out.println("######## Riepilogo della sua votazione ########");
                                System.out.println("Alla Camera dei deputati ha votato: " + listeCamera[outputs[0]].getNome());
                                if (outputs[1] != -1) {
                                    System.out.println("Al Senato ha votato: " + listeSenato[outputs[1]].getNome());
                                } else if (elettore.getEta() < 25) {
                                    System.out.println("Al Senato ha votato: NON aveva i requisiti per questa votazione");
                                } else {
                                    System.out.println("Al Senato ha votato: Alla consegna delle schede ha scelto di NON votare per il senato");
                                }
                                for (int i = 0; i < referendum.length; i++) {
                                    int indexRef = i+2;
                                   
                                   switch (outputs[indexRef]) {
                                            case 0:
                                                System.out.println("Al quesito referendario \" " + referendum[i].getTitolo() + " \" ha votato: NO");
                                                break;
                                            case 1:
                                                System.out.println("Al quesito referendario \" " + referendum[i].getTitolo() + " \" ha votato: SI");
                                                break;
                                            case 2:
                                                System.out.println("Al quesito referendario \" " + referendum[i].getTitolo() + " \" ha votato: Scheda Bianca");
                                                break;
                                            default:
                                                break;
                                        }
                                    
                                }
                                 do {
                                    System.out.println("\n Desidera confermare le sue scelte? [S/N]");
                                    choise = daTastiera.nextLine();
                                      
                                } while (!choise.equalsIgnoreCase("s") && !choise.equalsIgnoreCase("n"));
                                //SE la votazione e' confermata
                                if (choise.equalsIgnoreCase("s")) {
                                    //SE riesco a salvare i voti
                                    if(seggioElettorale.salvaVotiElettore(outputs, cabinaElettorale)){
                                    System.out.println("\n La votazione e' terminata puo' lasciare la cabina.\n\n");
                                    } else {
                                    System.out.println("\n Si e' verificato un errore con la sua votazione, la sua votazione sara' riavviata.\n\n");
                                    }
                                    //Altrimenti se NON e' confermata, lo stampo a video ed il ciclo riprende
                                    //quindi e' possibile ri-votare finche' non si conferma la votazione
                                    //e riesco a salvare i voti
                                } else {
                                    System.out.println("Ha scelto di NON confermare questa votazione\n\n");
                                }                                
                            } catch (Exception ex) {
                                seggioElettorale.logoutCabina(idCabina);
                                System.out.println("Si e' verificato un errore durante il riepilogo della sua votazione, richieda assistenza al personale");
                                ex.printStackTrace();
                            }
                           
                        }
                    } //Scelta Q del menu
                    else if (choise.equalsIgnoreCase("q"))
                    {
                        try
                        {
                            seggioElettorale.logoutCabina(idCabina);
                        }
                        catch (RemoteException ex1)
                        {
                            
                        }
                        
                        System.exit(0);
                    }
                    //Prima di riprendere il ciclo e attendere un nuovo elettore
                    //o fare rivotare pulisco la console
                    resetSchermata();
                    
                    System.out.println("In attesa di un Elettore...\n");
                }//Se NON sono loggato provo a riloggarmi al seggio
                else
                {
                    loggato = seggioElettorale.loginCabina(indirizzoIp, idCabina);
                }
            }
        } 
        catch (FileNotFoundException ex)
        {
             
        }
        catch (RemoteException ex)
        {
            try
            {
                seggioElettorale.logoutCabina(idCabina);
            }
            catch (RemoteException ex1)
            {
                
            }
        }
        catch (NotBoundException ex)
        {
            try
            {
                seggioElettorale.logoutCabina(idCabina);
            }
            catch (RemoteException ex1)
            {
                
            }
        }
    }
//Metodo per il reset del terminale, pulisce la schermata
    private static void resetSchermata()
    {
        try
        {
            String os = System.getProperty("os.name");

            if (os.contains("Windows"))
            {
                Runtime.getRuntime().exec("cls");
            }
            else
            {
                Runtime.getRuntime().exec("clear");
            }
        }
        catch (Exception e)
        {
            
        }
    }
    
    //istanze della classe
    private static boolean accesso = false;
    private static String indirizzoIp;
    private static int idCabina;
    
    static ObjLista[] listeCamera;
    static ObjLista[] listeSenato;
    static ObjQuesito[] referendum;
    static int[] outputs;
    
    static ObjElettore elettore;
    static ObjCabina cabinaElettorale;
    
    static InterfacciaSeggio seggioElettorale;
}
