package com.octo.repository;

import com.octo.domain.Virement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VirementRepository extends JpaRepository<Virement, Long> {
}
