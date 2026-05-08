package uno.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uno.entity.Utilisateur;
import uno.repository.UtilisateurRepository;

import java.util.Optional;

@Service
public class UtilisateurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // Inscrire un nouvel utilisateur
    public Utilisateur inscrire(String pseudo, String motDePasse) {
        // On vérifie si le pseudo existe déjà
        if (utilisateurRepository.findByPseudo(pseudo).isPresent()) {
            throw new RuntimeException("Ce pseudo est déjà utilisé !");
        }

        // Création du nouvel utilisateur
        Utilisateur nouvelUtilisateur = new Utilisateur(pseudo, motDePasse);
        
        // Sauvegarde dans la base de données via le repository
        return utilisateurRepository.save(nouvelUtilisateur);
    }

    // Méthode pour la connexion (authentification simple)
    public Utilisateur connexion(String pseudo, String motDePasse) {
        Optional<Utilisateur> userOpt = utilisateurRepository.findByPseudo(pseudo);

        if (userOpt.isPresent() && userOpt.get().getMotDePasse().equals(motDePasse)) {
            return userOpt.get(); // Connexion réussie
        } else {
            throw new RuntimeException("Pseudo ou mot de passe incorrect.");
        }
    }

    // Méthode pour mettre à jour les stats après une partie
    public void mettreAJourStats(String pseudo, boolean aGagne) {
        utilisateurRepository.findByPseudo(pseudo).ifPresent(u -> {
            u.setPartiesJouees(u.getPartiesJouees() + 1);
            if (aGagne) {
                u.setVictoires(u.getVictoires() + 1);
            }
            utilisateurRepository.save(u);
        });
    }
}