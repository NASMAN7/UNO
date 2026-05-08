package uno.model;

import java.util.ArrayList;
import java.util.List;



public class Partie {
    private String codeSalon;
    private List<Joueur> joueurs;
    private List<Carte> pioche;
    private List<Carte> defausse;
    private int indexJoueurCourant;
    private boolean sensHoraire;
    private boolean partieDemarree;

    public Partie(String codeSalon) {
        this.codeSalon = codeSalon;
        this.joueurs = new ArrayList<>();
        this.pioche = new ArrayList<>();
        this.defausse = new ArrayList<>();
        this.indexJoueurCourant = 0;
        this.sensHoraire = true;
        this.partieDemarree = false;
    }

    public String getCodeSalon() {
        return codeSalon;
    }

    public void setCodeSalon(String codeSalon) {
        this.codeSalon = codeSalon;
    }

    public List<Joueur> getJoueurs() {
        return joueurs;
    }

    public void setJoueurs(List<Joueur> joueurs) {
        this.joueurs = joueurs;
    }

    public List<Carte> getPioche() {
        return pioche;
    }

    public void setPioche(List<Carte> pioche) {
        this.pioche = pioche;
    }

    public List<Carte> getDefausse() {
        return defausse;
    }

    public void setDefausse(List<Carte> defausse) {
        this.defausse = defausse;
    }

    public int getIndexJoueurCourant() {
        return indexJoueurCourant;
    }

    public void setIndexJoueurCourant(int indexJoueurCourant) {
        this.indexJoueurCourant = indexJoueurCourant;
    }

    public boolean isSensHoraire() {
        return sensHoraire;
    }

    public void setSensHoraire(boolean sensHoraire) {
        this.sensHoraire = sensHoraire;
    }

    public boolean isPartieDemarree() {
        return partieDemarree;
    }

    public void setPartieDemarree(boolean partieDemarree) {
        this.partieDemarree = partieDemarree;
    }
}