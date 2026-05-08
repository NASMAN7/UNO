package uno.dto;

import uno.model.Carte;

public class ActionJeu {
    private String pseudo;
    private Carte carte;
    private String codeSalon;

    public ActionJeu() {} // Constructeur par défaut 

    public String getPseudo() { return pseudo; }
    public void setPseudo(String pseudo) { this.pseudo = pseudo; }
    public Carte getCarte() { return carte; }
    public void setCarte(Carte carte) { this.carte = carte; }
    public String getCodeSalon() { return codeSalon; }
    public void setCodeSalon(String codeSalon) { this.codeSalon = codeSalon; }
}