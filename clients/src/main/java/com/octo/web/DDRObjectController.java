package com.octo.web;

import com.octo.service.DDRObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ddrs")
public class DDRObjectController {

    @Autowired
    public DDRObjectService ddrObjectService;

    @GetMapping("balance")
    public long balance(){
        return ddrObjectService.balance();
    }

    @GetMapping("count")
    public long count(){
        return ddrObjectService.count();
    }

    @GetMapping("average")
    public double average(){
        return ddrObjectService.average();
    }
}
