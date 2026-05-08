package uno.service;

import org.springframework.stereotype.Service;
import uno.dto.ActionJeu;
import uno.entity.Utilisateur;
import uno.model.Carte;
import uno.model.Couleur;
import uno.model.Joueur;
import uno.model.Partie;
import uno.model.Valeur;
import java.util.UUID;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PartieService {

    private Map<String, Partie> partiesEnCours = new ConcurrentHashMap<>();

    public Partie initialiserPartie(List<Utilisateur> utilisateurs) {
        // Pour tes tests, on peut fixer le code ou utiliser un UUID
        String codeSalon = "TEST_SALON"; 
        Partie partie = new Partie(codeSalon);

        for (Utilisateur u : utilisateurs) {
            partie.getJoueurs().add(new Joueur(u));
        }

        List<Carte> deck = creerDeckComplet();
        Collections.shuffle(deck);

        // Distribution
        for (int i = 0; i < 7; i++) {
            for (Joueur joueur : partie.getJoueurs()) {
                joueur.getMain().add(deck.remove(deck.size() - 1));
            }
        }

        Carte premiereCarte = deck.remove(deck.size() - 1);
        partie.getDefausse().add(premiereCarte);
        partie.setPioche(deck);
        partie.setPartieDemarree(true);

        partiesEnCours.put(codeSalon, partie);
        return partie;
    }

    // Méthode pour permettre à d'autres de rejoindre
    public Partie rejoindrePartie(String codeSalon, String pseudo) {
        Partie partie = partiesEnCours.get(codeSalon);
        if (partie == null) return initialiserPartie(new ArrayList<>());

        boolean existe = partie.getJoueurs().stream()
                .anyMatch(j -> j.getUtilisateur().getPseudo().equals(pseudo));

        if (!existe) {
            Utilisateur u = new Utilisateur();
            u.setPseudo(pseudo);
            Joueur nj = new Joueur(u);
            // On lui donne 7 cartes
            for(int i=0; i<7; i++) nj.getMain().add(partie.getPioche().remove(0));
            partie.getJoueurs().add(nj);
        }
        return partie;
    }

    public Partie recupererPartie(String codeSalon) {
        return partiesEnCours.get(codeSalon);
    }

    /**
     * LOGIQUE PRINCIPALE : Jouer une carte
     */
    public Partie jouerCarte(String codeSalon, ActionJeu action) {
        Partie partie = recupererPartie(codeSalon);
        if (partie == null) return null;

        // 1. Trouver le joueur
        Joueur joueur = partie.getJoueurs().stream()
                .filter(j -> j.getUtilisateur().getPseudo().equals(action.getPseudo()))
                .findFirst()
                .orElse(null);

        if (joueur == null) return partie;

        // 2. Vérifier si c'est son tour
        Joueur joueurCourant = partie.getJoueurs().get(partie.getIndexJoueurCourant());
        if (!joueur.getUtilisateur().getPseudo().equals(joueurCourant.getUtilisateur().getPseudo())) {
            // Optionnel : renvoyer une erreur au lieu de juste retourner la partie
            return partie; 
        }

        Carte carteAJouer = action.getCarte();
        Carte carteAuCentre = partie.getDefausse().get(partie.getDefausse().size() - 1);

        // 3. Valider le coup
        if (estCoupValide(carteAJouer, carteAuCentre)) {
            
            // Retirer la carte de la main (on compare valeur et couleur)
            joueur.getMain().removeIf(c -> 
                c.getValeur() == carteAJouer.getValeur() && 
                c.getCouleur() == carteAJouer.getCouleur()
            );
            
            partie.getDefausse().add(carteAJouer);

            // 4. Appliquer les effets (+2, Inversion, etc.)
            appliquerEffetCarte(partie, carteAJouer);

            // 5. Passer au suivant (si l'effet n'a pas déjà fait sauter un tour)
            if (carteAJouer.getValeur() != Valeur.PASSE_TOUR && 
                carteAJouer.getValeur() != Valeur.PLUS_DEUX && 
                carteAJouer.getValeur() != Valeur.PLUS_QUATRE) {
                passerAuJoueurSuivant(partie);
            }
        }

        return partie;
    }

    public boolean estCoupValide(Carte carteAJouer, Carte carteAuCentre) {
        if (carteAJouer.getCouleur() == Couleur.NOIR) return true;
        return carteAJouer.getCouleur() == carteAuCentre.getCouleur() || 
               carteAJouer.getValeur() == carteAuCentre.getValeur();
    }

    private void appliquerEffetCarte(Partie partie, Carte carte) {
        switch (carte.getValeur()) {
            case INVERSION:
                if (partie.getJoueurs().size() == 2) {
                    passerAuJoueurSuivant(partie);
                } else {
                    partie.setSensHoraire(!partie.isSensHoraire());
                }
                break;
            case PASSE_TOUR:
                passerAuJoueurSuivant(partie); 
                passerAuJoueurSuivant(partie); 
                break;
            case PLUS_DEUX:
                fairePiocherProchainJoueur(partie, 2);
                break;
            case PLUS_QUATRE:
                fairePiocherProchainJoueur(partie, 4);
                break;
            default: break;
        }
    }

    private void fairePiocherProchainJoueur(Partie partie, int nombreDeCartes) {
        passerAuJoueurSuivant(partie); 
        Joueur victime = partie.getJoueurs().get(partie.getIndexJoueurCourant());
        
        for (int i = 0; i < nombreDeCartes; i++) {
            if (partie.getPioche().isEmpty()) recreerPioche(partie);
            victime.getMain().add(partie.getPioche().remove(partie.getPioche().size() - 1));
        }
        passerAuJoueurSuivant(partie);
    }

    private void passerAuJoueurSuivant(Partie partie) {
        int nombreJoueurs = partie.getJoueurs().size();
        if (partie.isSensHoraire()) {
            partie.setIndexJoueurCourant((partie.getIndexJoueurCourant() + 1) % nombreJoueurs);
        } else {
            partie.setIndexJoueurCourant((partie.getIndexJoueurCourant() - 1 + nombreJoueurs) % nombreJoueurs);
        }
    }

    private void recreerPioche(Partie partie) {
        Carte top = partie.getDefausse().remove(partie.getDefausse().size() - 1);
        List<Carte> deck = new ArrayList<>(partie.getDefausse());
        Collections.shuffle(deck);
        partie.setPioche(deck);
        partie.getDefausse().clear();
        partie.getDefausse().add(top);
    }

    private List<Carte> creerDeckComplet() {
        List<Carte> deck = new ArrayList<>();
        Couleur[] couleurs = {Couleur.ROUGE, Couleur.BLEU, Couleur.VERT, Couleur.JAUNE};
        for (Couleur c : couleurs) {
            deck.add(new Carte(c, Valeur.ZERO));
            for (int i = 0; i < 2; i++) {
                deck.add(new Carte(c, Valeur.UN));
                deck.add(new Carte(c, Valeur.DEUX));
                deck.add(new Carte(c, Valeur.TROIS));
                deck.add(new Carte(c, Valeur.QUATRE));
                deck.add(new Carte(c, Valeur.CINQ));
                deck.add(new Carte(c, Valeur.SIX));
                deck.add(new Carte(c, Valeur.SEPT));
                deck.add(new Carte(c, Valeur.HUIT));
                deck.add(new Carte(c, Valeur.NEUF));
                deck.add(new Carte(c, Valeur.PASSE_TOUR));
                deck.add(new Carte(c, Valeur.INVERSION));
                deck.add(new Carte(c, Valeur.PLUS_DEUX));
            }
        }
        for (int i = 0; i < 4; i++) {
            deck.add(new Carte(Couleur.NOIR, Valeur.JOKER));
            deck.add(new Carte(Couleur.NOIR, Valeur.PLUS_QUATRE));
        }
        return deck;
    }

    // 1. Nouvelle méthode pour créer une partie avec un code unique
    public Partie creerNouvellePartie(String pseudoCreateur) {
        // Génère un code court de 5 caractères (ex: XJ92L)
        String codeSalon = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        
        Partie partie = new Partie(codeSalon);
        
        // On récupère l'utilisateur (ou on en crée un temporaire pour le Joueur)
        Utilisateur u = new Utilisateur();
        u.setPseudo(pseudoCreateur);
        partie.getJoueurs().add(new Joueur(u));
        
        // On initialise le deck
        List<Carte> deck = creerDeckComplet();
        Collections.shuffle(deck);
        partie.setPioche(deck);
        
        partiesEnCours.put(codeSalon, partie);
        return partie;
    }
    

}