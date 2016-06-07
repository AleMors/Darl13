
/* Classe che descrive le caratteristiche di una cabina elettorale online
 * @author Lorenzo Guerzoni (Matr. 0000639838)
 */
import java.io.Serializable;

public class ObjCabina implements Serializable{
	String IP;
	int ID;
	boolean libera = false;
	String[] votazioniDaEffettuare;

    public String[] getVotazioni() {
        return votazioniDaEffettuare;
    }

    public void setVotazioni(String[] votazioni) {
        this.votazioniDaEffettuare = votazioni;
    }
	ObjElettore elettoreAssociato;
	String codiceAccesso = "";
	
	public ObjCabina(String IP, int ID, boolean libera){
		this.IP = IP;
		this.ID = ID;
		this.libera= libera;
	}
        
        public ObjCabina(String IP, int ID, String[] votazioni, boolean libera){
		this.IP = IP;
		this.ID = ID;
		this.votazioniDaEffettuare = votazioni;
                this.libera = true;
	}
	
	public String getIP(){
		return this.IP;
	}
	
	public int getID(){
		return this.ID;
	}
	
	public void setID(int ID){
		this.ID = ID;
	}
 
        
        public ObjElettore getElettore(){
		return this.elettoreAssociato;
	}
	
	public void setElettore(ObjElettore elettore){
		this.elettoreAssociato = elettore;
	}
	
	public void resetVotazioni(){
		 
			votazioniDaEffettuare=null;
	}
	
 
	 
	
	public boolean getLibera(){
		return this.libera;
	}
	
	public void setLibera(boolean b){
		this.libera = b;
	}
	
	 
	
 
	
}
