

import  java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface InterfacciaInterno extends Remote{
	
	public String Login(String regione, String IP, int ID, String codice) throws RemoteException;
	public ObjLista[] getListeCamera() throws RemoteException;
        public ObjLista[] getListeCameraFriuli() throws RemoteException;
        public ObjLista[] getListeCameraTrentino() throws RemoteException;
	public ObjLista[] getListeSenato() throws RemoteException;
        
	public ObjQuesito[] getListeReferendum() throws RemoteException;
	public boolean affluenza(double iscrittiAlSeggio, double votantiSenato, double elettoriVotato, double elettoriVotatoCamera, double elettoriVotatoSenato, double[] elettoriVotatoReferendum) throws RemoteException;
	public void calcolaVoti(int idSeggio, ArrayList votiSeggio, double elettoriVotato, int iscrittiAlSeggio) throws RemoteException;
}    
