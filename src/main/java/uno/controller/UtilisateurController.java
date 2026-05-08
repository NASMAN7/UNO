package uno.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uno.dto.AuthRequest;
import uno.entity.Utilisateur;
import uno.service.UtilisateurService;

@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin(origins = "*") 
public class UtilisateurController {

    @Autowired
    private UtilisateurService utilisateurService;

    // Endpoint pour l'inscription : POST http://localhost:8080/api/utilisateurs/inscription
    @PostMapping("/inscription")
    public ResponseEntity<?> inscrire(@RequestBody AuthRequest requete) {
        try {
            Utilisateur nouvelUtilisateur = utilisateurService.inscrire(requete.getPseudo(), requete.getMotDePasse());
            return ResponseEntity.ok(nouvelUtilisateur);
        } catch (RuntimeException e) {
            // Si le pseudo est déjà pris, on renvoie une erreur 400 
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Endpoint pour la connexion : POST http://localhost:8080/api/utilisateurs/connexion
    @PostMapping("/connexion")
    public ResponseEntity<?> connecter(@RequestBody AuthRequest requete) {
        try {
            Utilisateur utilisateur = utilisateurService.connexion(requete.getPseudo(), requete.getMotDePasse());
            return ResponseEntity.ok(utilisateur);
        } catch (RuntimeException e) {
            // Si mauvais mot de passe ou pseudo, erreur 401 (Unauthorized)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}