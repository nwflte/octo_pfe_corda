package com.octo.web;

import com.octo.service.CompteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/comptes")
public class CompteController {

    private final CompteService compteService;

    @Autowired
    public CompteController(CompteService compteService) {
        this.compteService = compteService;
    }

    @GetMapping("/exists/{id}")
    public ResponseEntity<Boolean> ribExists(@PathVariable String id){
        return ResponseEntity.ok(compteService.existsByRIB(id));
    }
}
