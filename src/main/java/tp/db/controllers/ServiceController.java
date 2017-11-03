package tp.db.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tp.db.dao.ServiceDAO;

@RestController
@RequestMapping("/api/service")
public class ServiceController {
    private ServiceDAO serviceDAO;

    public ServiceController(ServiceDAO serviceDAO) {
        this.serviceDAO = serviceDAO;
    }


    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(serviceDAO.getStatus());
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clear() {
        serviceDAO.clear();
        return ResponseEntity.ok(null);
    }

}
