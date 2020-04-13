package com.octo.service.impl;

import com.octo.domain.Compte;
import com.octo.domain.Virement;
import com.octo.dto.VirementDto;
import com.octo.exceptions.CompteNonExistantException;
import com.octo.exceptions.SoldeDisponibleInsuffisantException;
import com.octo.repository.CompteRepository;
import com.octo.repository.VirementRepository;
import com.octo.service.VirementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VirementServiceImpl implements VirementService {

  private final CompteRepository compteRepository;
  private final VirementRepository virementRepository;

  @Autowired
  public VirementServiceImpl(CompteRepository compteRepository,
                         VirementRepository virementRepository) {
    this.compteRepository = compteRepository;
    this.virementRepository = virementRepository;
  }

  @Override
  public List<Virement> loadAll() {
    return virementRepository.findAll();
  }

  @Override
  public Optional<Virement> findById(Long id) {
    return  virementRepository.findById(id);
  }

  @Override
  public void virement(VirementDto virementDto) {
    Compte compteEmetteur = compteRepository.findByNrCompte(virementDto.getNrCompteEmetteur());
    Compte compteBeneficiaire = compteRepository
        .findByNrCompte(virementDto.getNrCompteBeneficiaire());

    if(compteEmetteur == null || compteBeneficiaire == null) {
      throw new CompteNonExistantException("Compte Non existant");
    }


    if (compteEmetteur.getSolde().compareTo(virementDto.getMontantVirement()) < 0) {
      throw new SoldeDisponibleInsuffisantException(
          "Solde insuffisant pour l'utilisateur " + compteEmetteur.getUtilisateur().getUsername());
    }

    compteEmetteur.setSolde(compteEmetteur.getSolde().subtract(virementDto.getMontantVirement()));
    compteRepository.save(compteEmetteur);

    compteBeneficiaire
        .setSolde(compteBeneficiaire.getSolde().add(virementDto.getMontantVirement()));
    compteRepository.save(compteBeneficiaire);

    Virement virement = new Virement();
    virement.setDateExecution(virementDto.getDate());
    virement.setCompteBeneficiaire(compteBeneficiaire);
    virement.setCompteEmetteur(compteEmetteur);
    virement.setMontantVirement(virementDto.getMontantVirement());

    virementRepository.save(virement);
  }
}