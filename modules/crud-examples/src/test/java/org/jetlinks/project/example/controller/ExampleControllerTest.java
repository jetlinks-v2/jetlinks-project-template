package org.jetlinks.project.example.controller;

import org.hswebframework.web.starter.jackson.CustomCodecsAutoConfiguration;
import org.hswebframework.web.starter.jackson.CustomJackson2jsonEncoder;
import org.jetlinks.pro.test.spring.TestJetLinksController;
import org.jetlinks.project.example.entity.ExampleEntity;
import org.jetlinks.project.example.enums.ExampleEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(ExampleController.class)
class ExampleControllerTest extends TestJetLinksController {

    @Test
    void testCrud() {

        client
            .post()
            .uri("/example/crud")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"name\": \"test\",\"singleEnum\": \"enum1\",\"multiEnum\": [\"enum1\",\"enum3\"]}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .get()
            .uri(builder-> builder
                .path("/example/crud/_query/no-paging")
                .queryParam("where","multiEnum in enum1,enum3")
                .build())
            .exchange()
            .expectBodyList(ExampleEntity.class)
            .hasSize(1);

    }

}