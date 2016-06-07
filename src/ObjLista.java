

/* Classe che descrive le caratteristiche di una lista/coalizione candidata
 * @author Alessandro Morsiani (Matr. 0000639120)
 */

import java.io.Serializable;

public class ObjLista implements Serializable {

    String nome;
    String regioneSeggio = "";
    int voti = 0;
    int ID; 
    boolean coalizione=false;
    
    public String getRegioneSeggio() {
        return regioneSeggio;
    }

    public void setRegioneSeggio(String regioneSeggio) {
        this.regioneSeggio = regioneSeggio;
    }

    public ObjLista(String n, int id, String regioneSeggio) {
        this.nome = n;
        this.ID = id;
        this.regioneSeggio = regioneSeggio;
    }

    public ObjLista(String nome, int id) {
        this.nome = nome;
        this.ID = id;
    }
    
    public ObjLista(String nome, int id, boolean coalizione) {
        this.nome = nome;
        this.ID = id;
        this.coalizione = coalizione;
    }

 
    public ObjLista(String nome, int id, boolean coalizione, String regioneSeggio) {
        this.nome = nome;
        this.ID = id;
        this.coalizione = coalizione;
        this.regioneSeggio = regioneSeggio;
    }

    public String getNome() {
        return this.nome;
    }

    public int getVoti() {
        return this.voti;
    }

    public int getID() {
        return this.ID;
    }

    public void setVoti(int voti) {
        this.voti += voti;
    }

    public void incrementaVoti() {
        this.voti++;
    }

    public void incrementaVoti(int voti) {
        this.voti += voti;
    }
}
