package com.octo.service;

import com.octo.domain.Virement;
import com.octo.dto.VirementDto;

import java.util.List;
import java.util.Optional;

public interface VirementService {
    List<Virement> loadAll();

    Optional<Virement> findById(Long id);

    void virement(VirementDto virementDto);
}
