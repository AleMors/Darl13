

/* Classe che descrive le caratteristiche di un avente diritto al voto
 * @author Roberto Squillace (Matr. 0000239924)
 */

import java.io.Serializable;

 

public class ObjElettore implements Serializable{
	String nome;
	String cognome;
	String documento;
	int eta;
        
	public ObjElettore(String nome, String cognome, int eta, String documento ){
		this.nome=nome;
		this.cognome = cognome;
		this.eta = eta;
		this.documento = documento;
	}
	
	public String getNome(){
		return this.nome;
	}
	
	public String getCognome(){
		return this.cognome;
	}
	
	public int getEta(){
		return this.eta;
	}
	
	public String getDocumento(){
		return this.documento;
	}
	
}
