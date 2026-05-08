package uno.dto;

import java.time.LocalDateTime;

public class MessageChat {
    private String pseudoExpediteur;
    private String contenu;
    private LocalDateTime dateEnvoi;

    public MessageChat() {
        this.dateEnvoi = LocalDateTime.now();
    }

    public MessageChat(String pseudoExpediteur, String contenu) {
        this.pseudoExpediteur = pseudoExpediteur;
        this.contenu = contenu;
        this.dateEnvoi = LocalDateTime.now();
    }

    public String getPseudoExpediteur() {
        return pseudoExpediteur;
    }

    public void setPseudoExpediteur(String pseudoExpediteur) {
        this.pseudoExpediteur = pseudoExpediteur;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }
}