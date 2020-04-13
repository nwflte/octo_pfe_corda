package com.octo.mapper;


import com.octo.domain.Virement;
import com.octo.dto.VirementDto;

public class VirementMapper {

    public static VirementDto map(Virement virement) {
        if (virement == null) return null;
        VirementDto virementDto = new VirementDto();
        virementDto.setMontantVirement(virement.getMontantVirement());
        virementDto.setNrCompteBeneficiaire(virement.getCompteBeneficiaire().getNrCompte());
        virementDto.setNrCompteEmetteur(virement.getCompteEmetteur().getNrCompte());
        virementDto.setDate(virement.getDateExecution());
        virementDto.setMotif(virement.getMotifVirement());

        return virementDto;

    }
}
