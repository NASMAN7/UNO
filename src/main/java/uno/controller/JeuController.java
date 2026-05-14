package uno.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import uno.model.Partie;
import uno.service.PartieService;
import uno.dto.ActionJeu;

@Controller
public class JeuController {

    @Autowired
    private PartieService partieService;

    // ✅ SimpMessagingTemplate permet d'envoyer des messages à des canaux spécifiques dynamiquement
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/jeu.creer")
    public void creer(String pseudo) {
        System.out.println("CREER OK : " + pseudo);
    
        Partie partie = partieService.creerNouvellePartie(pseudo);
    
        System.out.println("PARTIE CREEE : " + partie.getCodeSalon());
    
        messagingTemplate.convertAndSend("/topic/jeu.init/" + pseudo, partie);
    
        System.out.println("ENVOI TERMINE");
    }

    @MessageMapping("/jeu.rejoindre/{code}")
    public void rejoindre(@DestinationVariable String code, String pseudo) {
        System.out.println("Rejoindre salon : " + code + " par " + pseudo);
        Partie partie = partieService.rejoindrePartie(code, pseudo);
        
        if (partie != null) {
            // ✅ Canal du SALON : On prévient tous ceux qui sont déjà dans ce salon précis
            messagingTemplate.convertAndSend("/topic/jeu/" + code, partie);
            
            // ✅ Réponse PRIVÉE : On confirme au nouveau joueur qu'il est bien entré
            messagingTemplate.convertAndSend("/topic/jeu.init/" + pseudo, partie);
        } else {
            // Optionnel : Envoyer une erreur si le salon n'existe pas
            messagingTemplate.convertAndSend("/topic/jeu.init/" + pseudo, new Partie("ERREUR"));
        }
    }

    @MessageMapping("/jeu.jouer/{codeSalon}")
public void jouer(
        @DestinationVariable String codeSalon,
        ActionJeu action
) {

    Partie partie =
            partieService.jouerCarte(codeSalon, action);

    if (partie != null) {

        messagingTemplate.convertAndSend(
                "/topic/jeu/" + codeSalon,
                partie
        );
    }
}
}