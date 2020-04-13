package com.octo.service.impl;

import com.octo.repository.CompteRepository;
import com.octo.service.CompteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class CompteServiceImpl implements CompteService {

    private final CompteRepository compteRepository;

    @Autowired
    public CompteServiceImpl(CompteRepository compteRepository) {
        this.compteRepository = compteRepository;
    }

    @Override
    public boolean existsByRIB(String rib) {
        return compteRepository.existsByRib(rib);
    }
}
