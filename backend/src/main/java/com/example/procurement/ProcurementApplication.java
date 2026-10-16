package com.example.procurement;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Deploys BPMN/DMN from the repo-root camunda/ folder (mapped onto the classpath, see pom.xml). */
@SpringBootApplication
@EnableScheduling
@Deployment(resources = {"classpath*:camunda/bpmn/*.bpmn", "classpath*:camunda/dmn/*.dmn"})
public class ProcurementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcurementApplication.class, args);
    }
}
