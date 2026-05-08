const API_URL = "http://localhost:8080/uno/api/utilisateurs";
let stompClient = null;
let nomUtilisateur = "";
let codeSalonActuel = ""; // Déjà présent à la fin, remonte-le au début
let abonnementChatActuel = null;

async function inscription() {
    const pseudo = document.getElementById('pseudo').value;
    const password = document.getElementById('password').value;

    const response = await fetch(`${API_URL}/inscription`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pseudo: pseudo, motDePasse: password })
    });

    const data = await response.json();
    if (response.ok) {
        document.getElementById('message').innerText = "Inscription réussie !";
    } else {
        document.getElementById('message').innerText = "Erreur : " + data;
    }
}

async function connexion() {
    const pseudo = document.getElementById('pseudo').value;
    const password = document.getElementById('password').value;

    const response = await fetch(`${API_URL}/connexion`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pseudo: pseudo, motDePasse: password })
    });

    if (response.ok) {
        localStorage.setItem('uno_pseudo', pseudo);
        
        nomUtilisateur = pseudo;
        connecterAuChat(pseudo); 
    } else {
        document.getElementById('message').innerText = "Identifiants incorrects";
    }
}

window.onload = function() {
    const pseudoSauvegarde = localStorage.getItem('uno_pseudo');
    const salonSauvegarde = localStorage.getItem('uno_salon');

    if (pseudoSauvegarde) {
        nomUtilisateur = pseudoSauvegarde;
        document.getElementById('message').innerText = "Bon retour, " + nomUtilisateur;
        
        // On se connecte et une fois connecté, on tente de rejoindre le salon
        connecterAuChat(nomUtilisateur, salonSauvegarde);
        
        // On attend un tout petit peu que le socket s'ouvre pour rejoindre
        if (salonSauvegarde) {
            setTimeout(() => {
                document.getElementById('code-salon-input').value = salonSauvegarde;
                rejoindrePartieParCode();
            }, 1000);
        }
    }
};

function deconnexion() {
    localStorage.removeItem('uno_pseudo');
    location.reload(); // Recharge la page pour revenir à zéro
}

function connecterAuChat(pseudo, salonARejoindre = null) {
    nomUtilisateur = pseudo;
    const socket = new SockJS('/uno/ws-uno');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connecté au WebSocket : ' + frame);

        document.getElementById('auth-form').style.display = 'none';
        document.getElementById('salon-actions').style.display = 'block';

        stompClient.subscribe('/topic/jeu', function (message) {
            const partie = JSON.parse(message.body);
            codeSalonActuel = partie.codeSalon;
            localStorage.setItem('uno_salon', codeSalonActuel);

            chargerHistoriqueChat(codeSalonActuel);
            configurerChatDuSalon(codeSalonActuel);

            document.getElementById('affichage-code-salon').innerText = "Salon : " + partie.codeSalon;
            document.getElementById('salon-actions').style.display = 'none';
            document.getElementById('jeu-section').style.display = 'block';

            const listeUl = document.getElementById('liste-joueurs-ul');
            if (listeUl) {
                listeUl.innerHTML = "";
                partie.joueurs.forEach(j => {
                    const li = document.createElement('li');
                    li.innerText = "[*] " + j.utilisateur.pseudo;
                    if (j.utilisateur.pseudo === nomUtilisateur) li.style.color = "blue";
                    listeUl.appendChild(li);
                });
            }
            afficherEtatJeu(partie);
        });

        // ← Rejoindre immédiatement si salon sauvegardé
        if (salonARejoindre) {
            stompClient.send("/app/jeu.rejoindre/" + salonARejoindre, {}, nomUtilisateur);
        }
    });
}

function envoyerMessage() {
    const texte = document.getElementById('chat-input').value;
    // On ajoute la vérification du codeSalonActuel
    if (texte && stompClient && codeSalonActuel) {
        const messageChat = {
            pseudoExpediteur: nomUtilisateur,
            contenu: texte
        };
        // On envoie au salon spécifique !
        stompClient.send("/app/chat.envoyer/" + codeSalonActuel, {}, JSON.stringify(messageChat));
        document.getElementById('chat-input').value = "";
    }
}

function afficherMessage(message) {
    const zoneMessages = document.getElementById('chat-messages');
    const messageElement = document.createElement('div');
    messageElement.innerHTML = `<strong>${message.pseudoExpediteur} :</strong> ${message.contenu}`;
    zoneMessages.appendChild(messageElement);
    zoneMessages.scrollTop = zoneMessages.scrollHeight; // Scroll automatique vers le bas
    // --- SAUVEGARDE DANS L'HISTORIQUE ---
    if (codeSalonActuel) {
        let historique = JSON.parse(localStorage.getItem('chat_' + codeSalonActuel)) || [];
        historique.push(message);
        // On ne garde que les 50 derniers messages pour ne pas saturer la mémoire
        if (historique.length > 50) historique.shift(); 
        localStorage.setItem('chat_' + codeSalonActuel, JSON.stringify(historique));
    }
}


function convertirCouleur(nom) {
    if (!nom) return '#333333'; // Noir par défaut si pas de couleur 
    const map = { 
        'ROUGE': '#ff5555', 
        'BLEU': '#5555ff', 
        'VERT': '#55aa55', 
        'JAUNE': '#ffaa00', 
        'NOIR': '#222222' 
    };
    return map[nom.toUpperCase()] || '#cccccc'; // Gris clair si erreur
}

function afficherEtatJeu(partie) {
    // --- 1. Affichage de la carte au centre de la table ---
    if (partie.defausse && partie.defausse.length > 0) {
        const derniereCarte = partie.defausse[partie.defausse.length - 1];
        const divCentre = document.getElementById('carte-centrale');
        if (divCentre) {
            divCentre.innerText = derniereCarte.valeur;
            divCentre.style.backgroundColor = convertirCouleur(derniereCarte.couleur);
            divCentre.style.color = "white"; // Texte en blanc pour bien lire
        }
    }

    const monJoueur = partie.joueurs.find(j => j.utilisateur.pseudo === nomUtilisateur || j.pseudo === nomUtilisateur);
    
    if (monJoueur && monJoueur.main) {
        const divMain = document.getElementById('ma-main');
        if (divMain) {
            divMain.innerHTML = ""; // On vide la main pour redessiner les nouvelles cartes

            monJoueur.main.forEach(carte => {
                const bouton = document.createElement('button');
                bouton.className = "carte-bouton";
                bouton.innerText = carte.valeur;
                
                // On applique la vraie couleur !
                bouton.style.backgroundColor = convertirCouleur(carte.couleur);
                bouton.style.color = "white";
                
                // On prépare l'action : quand on clique, ça joue la carte
                bouton.onclick = () => jouerCetteCarte(carte, partie.codeSalon);
                
                divMain.appendChild(bouton);
            });
        }
    }
}

function jouerCetteCarte(carte, codeSalon) {
    if (!stompClient) return;
    
    const action = {
        pseudo: nomUtilisateur,
        carte: carte,
        codeSalon: codeSalon
    };
    
    console.log("Je joue la carte : ", action);
    // On envoie l'action au serveur
    stompClient.send(`/app/jeu.jouer/${codeSalon}`, {}, JSON.stringify(action));
}

function chargerHistoriqueChat(codeSalon) {
    const zoneMessages = document.getElementById('chat-messages');
    zoneMessages.innerHTML = ""; // On vide l'affichage actuel
    
    const historique = JSON.parse(localStorage.getItem('chat_' + codeSalon)) || [];
    
    historique.forEach(msg => {
        const messageElement = document.createElement('div');
        messageElement.innerHTML = `<strong>${msg.pseudoExpediteur} :</strong> ${msg.contenu}`;
        zoneMessages.appendChild(messageElement);
    });
    
    zoneMessages.scrollTop = zoneMessages.scrollHeight;
}

// FONCTION 1 : Créer un nouveau salon
function creerPartie() {
    if (stompClient && stompClient.connected) {
        console.log("Demande de création de partie...");
        // On envoie juste le pseudo, le serveur s'occupe de générer le code UUID
        stompClient.send("/app/jeu.creer", {}, nomUtilisateur);
    } else {
        alert("Erreur : Connexion WebSocket perdue. Recharge la page.");
    }
}

// FONCTION 2 : Rejoindre un salon existant
function rejoindrePartieParCode() {
    const codeSaisi = document.getElementById('code-salon-input').value.trim().toUpperCase();
    
    if (!codeSaisi) {
        alert("Veuillez entrer un code de salon !");
        return;
    }

    if (stompClient && stompClient.connected) {
        console.log("Tentative de rejoindre le salon : " + codeSaisi);
        // On envoie le pseudo vers l'adresse spécifique du salon
        stompClient.send("/app/jeu.rejoindre/" + codeSaisi, {}, nomUtilisateur);
    }
}

function configurerChatDuSalon(codeSalon) {
    // Si on change de salon, on se désabonne de l'ancien
    if (abonnementChatActuel) {
        abonnementChatActuel.unsubscribe();
    }

    // On s'abonne au flux de messages de CE salon uniquement
    abonnementChatActuel = stompClient.subscribe('/topic/chat/' + codeSalon, function (messageOutput) {
        afficherMessage(JSON.parse(messageOutput.body));
    });
}