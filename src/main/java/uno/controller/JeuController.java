package uno.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import uno.model.Partie;
import uno.service.PartieService;
import uno.dto.ActionJeu;
import java.util.ArrayList;
import java.util.List;
import uno.entity.Utilisateur;

@Controller
public class JeuController {

    @Autowired
    private PartieService partieService;

    @MessageMapping("/jeu.creer")
    @SendTo("/topic/jeu")
    public Partie creer(String pseudo) {
        System.out.println("Création de partie pour : " + pseudo);
        return partieService.creerNouvellePartie(pseudo);
    }

    @MessageMapping("/jeu.rejoindre/{code}")
    @SendTo("/topic/jeu")
    public Partie rejoindre(@DestinationVariable String code, String pseudo) {
        System.out.println("Rejoindre salon : " + code + " par " + pseudo);
        return partieService.rejoindrePartie(code, pseudo);
    }

    @MessageMapping("/jeu.jouer/{codeSalon}")
    @SendTo("/topic/jeu") 
    public Partie jouer(@DestinationVariable String codeSalon, ActionJeu action) {
        // Appelle la logique complète du service
        return partieService.jouerCarte(codeSalon, action);
    }
}