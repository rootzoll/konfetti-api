package de.konfetti.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by relampago on 20.12.16.
 */
@RestController
@RequestMapping(ServiceController.REST_API_MAPPING)
public class ServiceController {

    @Value("${konfetti.minimumAppVersion.android}")
    private Integer minimumAppVersionAndroid;

    @Value("${konfetti.minimumAppVersion.ios}")
    private Integer minimumAppVersionIos;

    public static final String REST_API_MAPPING = "konfetti/api/service";
    //private Logger logger = LoggerFactory.getLogger(this.getClass());

    @GetMapping(value = "/health")
    ResponseEntity<?> health(){
        return new ResponseEntity<String>("UP", HttpStatus.OK);
    }

    @GetMapping(value = "/minimumAppVersionAndroid")
    ResponseEntity<Integer> minimumAppVersionAndroid(){
        return new ResponseEntity<Integer>(minimumAppVersionAndroid, HttpStatus.OK);
    }

    @GetMapping(value = "/minimumAppVersionIos")
    ResponseEntity<Integer> minimumAppVersionIos(){
        return new ResponseEntity<Integer>(minimumAppVersionIos, HttpStatus.OK);
    }

}
