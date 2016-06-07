

import  java.rmi.Remote;
import java.rmi.RemoteException;

/* Interfaccia Server Interno -> Server Seggio
 * @author Lorenzo Guerzoni (Matr. 0000639838), Roberto Squillace (Matr. 0000239924)
 */
public interface InterfacciaSeggio extends Remote{
	
	public boolean salvaVotiElettore(int[] risultati, ObjCabina cabina) throws RemoteException;
	public ObjLista[] getListeCamera() throws RemoteException;
	public ObjLista[] getListeSenato() throws RemoteException;
	public ObjQuesito[] getReferendum() throws RemoteException;
       
        /*public ObjElettore getElettoreCabina(int ID) throws RemoteException;*/
	public boolean loginCabina(String IP, int ID) throws RemoteException; 
	/*public int richiestaDatiCabina(int ID) throws RemoteException;*/
	public boolean lasciaCabina(ObjCabina cabina) throws RemoteException;
	public ObjCabina attesaElettore(int ID) throws RemoteException;
        public void logoutCabina(int ID) throws RemoteException;
}    
