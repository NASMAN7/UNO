package uno.model;

import uno.entity.Utilisateur;
import java.util.ArrayList;
import java.util.List;

public class Joueur {
    private Utilisateur utilisateur;
    private List<Carte> main;
    private boolean aDitUno;

    public Joueur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        this.main = new ArrayList<>();
        this.aDitUno = false;
    }
    

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public List<Carte> getMain() {
        return main;
    }

    public void setMain(List<Carte> main) {
        this.main = main;
    }

    public boolean isADitUno() {
        return aDitUno;
    }

    public void setADitUno(boolean aDitUno) {
        this.aDitUno = aDitUno;
    }
}