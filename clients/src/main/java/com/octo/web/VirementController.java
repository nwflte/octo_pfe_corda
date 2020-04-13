package com.octo.web;

import com.octo.dto.VirementDto;
import com.octo.mapper.VirementMapper;
import com.octo.service.VirementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController(value = "api/virements")
class VirementController {

  @Autowired
  private VirementService virementService;

  @GetMapping
  public List<VirementDto> loadAll() {
    return Optional.ofNullable(virementService.loadAll()).orElse(Collections.emptyList())
            .stream().map(VirementMapper::map).collect(Collectors.toList());
  }

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  public void createTransaction(@RequestBody VirementDto virementDto){
    virementService.virement(virementDto);
  }

  @GetMapping("/{id}")
  public Optional<VirementDto> getById(@PathVariable Long id) {
    return Optional.ofNullable(VirementMapper
            .map(virementService.findById(id).orElseThrow(() -> new IllegalArgumentException(""))));
  }


}
