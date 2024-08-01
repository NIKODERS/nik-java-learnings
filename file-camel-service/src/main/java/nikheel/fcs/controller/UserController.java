package nikheel.fcs.controller;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/camel-rest")
public class UserController {

    @Autowired
    private ProducerTemplate producerTemplate;

    @GetMapping("/user/{id}")
    public String getUser(@PathVariable String id) {
        return producerTemplate.requestBody("direct:getUser", id, String.class);
    }
}
