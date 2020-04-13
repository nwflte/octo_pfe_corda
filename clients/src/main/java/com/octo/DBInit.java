package com.octo;

import com.octo.domain.Compte;
import com.octo.domain.Utilisateur;
import com.octo.repository.CompteRepository;
import com.octo.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.db-init", havingValue = "true")
public class DBInit implements CommandLineRunner {

    @Autowired
    private CompteRepository compteRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Override
    public void run(String... args) throws Exception {
        Utilisateur utilisateur1 = new Utilisateur();
        utilisateur1.setUsername("user1");
        utilisateur1.setLastname("last1");
        utilisateur1.setFirstname("first1");
        utilisateur1.setGender("Male");

        utilisateurRepository.save(utilisateur1);


        Utilisateur utilisateur2 = new Utilisateur();
        utilisateur2.setUsername("user2");
        utilisateur2.setLastname("last2");
        utilisateur2.setFirstname("first2");
        utilisateur2.setGender("Female");

        utilisateurRepository.save(utilisateur2);

        Compte compte1 = new Compte();
        compte1.setNrCompte("010000A000001000");
        compte1.setRib("RIB111111");
        compte1.setSolde(BigDecimal.valueOf(200000L));
        compte1.setUtilisateur(utilisateur1);

        compteRepository.save(compte1);

        Compte compte2 = new Compte();
        compte2.setNrCompte("010000B025001000");
        compte2.setRib("RIB222222");
        compte2.setSolde(BigDecimal.valueOf(140000L));
        compte2.setUtilisateur(utilisateur2);

        compteRepository.save(compte2);
    }
}
