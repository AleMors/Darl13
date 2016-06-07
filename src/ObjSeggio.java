

public class ObjSeggio {

	private String regione;
	private String IP;
	private int ID;
	private boolean connesso = false;
	private String codiceAccesso = "";

	public ObjSeggio(String r, String ind, int ID){
		this.regione = r;
		this.IP = ind;
		this.ID = ID;
	}
	
	public String getRegione(){
		return regione;
	}
	
	public String getIP(){
		return IP;
	}
	
	public int getID(){
		return ID;
	}
	
	public boolean getConnesso(){
		return connesso;
	}
	
	public void setConnesso(boolean u){
		this.connesso = u;
	}
	
	public String getCodice(){
		return this.codiceAccesso;
	}
	
	public void setCodice(String c){
		this.codiceAccesso = c;
	}
}
