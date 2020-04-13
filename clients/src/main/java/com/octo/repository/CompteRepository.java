package com.octo.repository;

import com.octo.domain.Compte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompteRepository extends JpaRepository<Compte, Long> {
  Compte findByNrCompte(String nrCompte);
  Optional<Compte> findByRib(String rib);
  boolean existsByRib(String rib);
}
