
/* Classe che descrive le caratteristiche di un quesito referendario
 * @author Alessandro Morsiani (Matr. 0000639120)
 */
import java.io.Serializable;

public class ObjQuesito implements Serializable{
	String titolo;
	int ID;
	int votiSi = 0;
	int votiNo = 0;
	int votiBianco = 0;

	public ObjQuesito(String t, int id){
		this.titolo = t;
		this.ID = id;
	}
	
	public String getTitolo(){
		return this.titolo;
	}
	
	public int getVotiSi(){
		return this.votiSi;
	}
	
	public int getVotiNo(){
		return this.votiNo;
	}
	
	public int getVotiBianca(){
		return this.votiBianco;
	}
	
	public void votatoSi(){
		this.votiSi++;
	}
	
	public void votatoNo(){
		this.votiNo++;
	}
	
	public void votatoBianca(){
		this.votiBianco++;
	}
	
	 
	 public void setVotiSi(int voti){
		  this.votiSi+=voti;
	}
	
	public void setVotiNo(int voti){
		 this.votiNo+=voti;
	}
	
	public void setVotiBianca(int voti){
		this.votiBianco+=voti;
	}
	
	 
}
