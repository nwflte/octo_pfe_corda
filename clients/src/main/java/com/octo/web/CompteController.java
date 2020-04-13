package com.octo.web;

import com.octo.service.CompteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("comptes")
public class CompteController {

    private final CompteService compteService;

    @Autowired
    public CompteController(CompteService compteService) {
        this.compteService = compteService;
    }

    @PostMapping
    public ResponseEntity<Boolean> ribExists(@RequestBody Map<String, String> request){
        return ResponseEntity.ok(compteService.existsByRIB(request.getOrDefault("rib", "")));
    }
}
