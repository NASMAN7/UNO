const API_URL = "http://localhost:8080/uno/api/utilisateurs";
let stompClient = null;
let nomUtilisateur = "";
let codeSalonActuel = "";
let abonnementChatActuel = null;
// ✅ 1. Nouvelle variable pour écouter les cartes jouées dans CE salon précis
let abonnementJeuActuel = null; 

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
        document.getElementById('message').innerText = "Inscription reussie !";
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
        sessionStorage.setItem('uno_pseudo', pseudo);
        nomUtilisateur = pseudo;
        connecterAuChat(pseudo);
    } else {
        document.getElementById('message').innerText = "Identifiants incorrects";
    }
}

window.onload = function () {
    const pseudoSauvegarde = sessionStorage.getItem('uno_pseudo');
    const salonSauvegarde = sessionStorage.getItem('uno_salon');

    if (pseudoSauvegarde) {
        document.getElementById('auth-form').style.display = 'none';
        nomUtilisateur = pseudoSauvegarde;
        document.getElementById('message').innerText = "Bon retour, " + nomUtilisateur;
        connecterAuChat(nomUtilisateur, salonSauvegarde);
    } else {
        document.getElementById('auth-form').style.display = 'block';
        document.getElementById('salon-actions').style.display = 'none';
        document.getElementById('jeu-section').style.display = 'none';
    }
};

function deconnexion() {
    sessionStorage.removeItem('uno_pseudo');
    sessionStorage.removeItem('uno_salon');
    location.reload();
}

function connecterAuChat(pseudo, salonARejoindre = null) {
    nomUtilisateur = pseudo;
    const socket = new SockJS('/uno/ws-uno');
    stompClient = Stomp.over(socket);
    console.log("pseudo utilisé INIT =", pseudo);
    stompClient.connect({}, function (frame) {
        console.log('Connecte au WebSocket : ' + frame);

        document.getElementById('auth-form').style.display = 'none';
        document.getElementById('salon-actions').style.display = 'block';

        // ✅ 2. ABONNEMENT PRIVÉ : On écoute uniquement la réponse du serveur pour NOUS
        stompClient.subscribe('/topic/jeu.init/' + pseudo, function (message) {
            console.log("MESSAGE INIT RECU");
            const partie = JSON.parse(message.body);
            
            // Si le serveur a renvoyé une erreur (code faux)
            if (!partie || partie.codeSalon === "ERREUR" || partie.codeSalon === "") {
                alert("Salon introuvable ou expiré !");
                sessionStorage.removeItem('uno_salon');
                location.reload(); // On recharge pour revenir au menu
                return;
            }
            
            // Si tout est bon, on entre dans le salon
            entrerDansLeSalon(partie);
        });

        // Si c'est un rafraîchissement (F5), on demande à rejoindre l'ancien salon
        if (salonARejoindre) {
            stompClient.send("/app/jeu.rejoindre/" + salonARejoindre, {}, nomUtilisateur);
        }
    });
}

function entrerDansLeSalon(partie) {
    // 1. On enregistre les infos du salon
    codeSalonActuel = partie.codeSalon;
    sessionStorage.setItem('uno_salon', codeSalonActuel);

    // 2. On change l'affichage (On cache le menu, on montre la table)
    document.getElementById('affichage-code-salon').innerText = "Salon : " + codeSalonActuel;
    document.getElementById('salon-actions').style.display = 'none';
    document.getElementById('jeu-section').style.display = 'block';

    // 3. On nettoie les anciens abonnements (Indispensable si on change de salon ou fait F5)
    if (abonnementChatActuel) abonnementChatActuel.unsubscribe();
    if (abonnementJeuActuel) abonnementJeuActuel.unsubscribe();
    
    // 4. On s'abonne au CHAT de ce salon
    configurerChatDuSalon(codeSalonActuel);

    // ✅ 5. L'ÉTAPE CRUCIALE : On s'abonne au JEU de ce salon !
    // Sans ça, tu ne verrais pas les cartes jouées par les autres.
    abonnementJeuActuel = stompClient.subscribe('/topic/jeu/' + codeSalonActuel, function (message) {
        mettreAJourInterface(JSON.parse(message.body));
    });
    
    // 6. On affiche l'état initial (historique, joueurs et cartes)
    chargerHistoriqueChat(codeSalonActuel);
    mettreAJourInterface(partie);
}

// ✅ 6. J'ai créé cette petite fonction pour éviter de répéter du code
function mettreAJourInterface(partie) {
    afficherEtatJeu(partie);
    
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
}

function creerPartie() {
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/jeu.creer", {}, nomUtilisateur);
    } else {
        alert("Connexion WebSocket perdue. Recharge la page.");
    }
}

function rejoindrePartieParCode() {
    const codeSaisi = document.getElementById('code-salon-input').value.trim().toUpperCase();
    if (!codeSaisi) { alert("Veuillez entrer un code de salon !"); return; }
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/jeu.rejoindre/" + codeSaisi, {}, nomUtilisateur);
    }
}

function envoyerMessage() {
    const texte = document.getElementById('chat-input').value;
    if (texte && stompClient && codeSalonActuel) {
        const messageChat = { pseudoExpediteur: nomUtilisateur, contenu: texte };
        stompClient.send("/app/chat.envoyer/" + codeSalonActuel, {}, JSON.stringify(messageChat));
        document.getElementById('chat-input').value = "";
    }
}

function afficherMessage(message) {
    const zoneMessages = document.getElementById('chat-messages');
    const div = document.createElement('div');
    div.innerHTML = `<strong>${message.pseudoExpediteur} :</strong> ${message.contenu}`;
    zoneMessages.appendChild(div);
    zoneMessages.scrollTop = zoneMessages.scrollHeight;
    if (codeSalonActuel) {
        let historique = JSON.parse(sessionStorage.getItem('chat_' + codeSalonActuel)) || [];
        historique.push(message);
        if (historique.length > 50) historique.shift();
        sessionStorage.setItem('chat_' + codeSalonActuel, JSON.stringify(historique));
    }
}

function configurerChatDuSalon(codeSalon) {
    if (abonnementChatActuel) abonnementChatActuel.unsubscribe();
    abonnementChatActuel = stompClient.subscribe('/topic/chat/' + codeSalon, function (msg) {
        afficherMessage(JSON.parse(msg.body));
    });
}

function chargerHistoriqueChat(codeSalon) {
    const zone = document.getElementById('chat-messages');
    zone.innerHTML = "";
    const historique = JSON.parse(sessionStorage.getItem('chat_' + codeSalon)) || [];
    historique.forEach(msg => {
        const div = document.createElement('div');
        div.innerHTML = `<strong>${msg.pseudoExpediteur} :</strong> ${msg.contenu}`;
        zone.appendChild(div);
    });
    zone.scrollTop = zone.scrollHeight;
}

function convertirCouleur(nom) {
    if (!nom) return '#333333';
    const map = { 'ROUGE': '#ff5555', 'BLEU': '#5555ff', 'VERT': '#55aa55', 'JAUNE': '#ffaa00', 'NOIR': '#222222' };
    return map[nom.toUpperCase()] || '#cccccc';
}

function afficherEtatJeu(partie) {
    if (partie.defausse && partie.defausse.length > 0) {
        const derniere = partie.defausse[partie.defausse.length - 1];
        const divCentre = document.getElementById('carte-centrale');
        if (divCentre) {
            divCentre.innerText = derniere.valeur;
            divCentre.style.backgroundColor = convertirCouleur(derniere.couleur);
            divCentre.style.color = "white";
        }
    }
    const monJoueur = partie.joueurs.find(j => j.utilisateur.pseudo === nomUtilisateur || j.pseudo === nomUtilisateur);
    if (monJoueur && monJoueur.main) {
        const divMain = document.getElementById('ma-main');
        if (divMain) {
            divMain.innerHTML = "";
            monJoueur.main.forEach(carte => {
                const btn = document.createElement('button');
                btn.className = "carte-bouton";
                btn.innerText = carte.valeur;
                btn.style.backgroundColor = convertirCouleur(carte.couleur);
                btn.style.color = "white";
                btn.onclick = () => jouerCetteCarte(carte, partie.codeSalon);
                divMain.appendChild(btn);
            });
        }
    }
}

function jouerCetteCarte(carte, codeSalon) {
    if (!stompClient) return;
    stompClient.send(`/app/jeu.jouer/${codeSalon}`, {}, JSON.stringify({
        pseudo: nomUtilisateur, carte: carte, codeSalon: codeSalon
    }));
}