package org.jetlinks.pro.example.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.jetlinks.pro.test.spring.TestJetLinksController;
import org.jetlinks.pro.example.entity.ExampleEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(ExampleController.class)
class ExampleControllerTest extends TestJetLinksController {

    @Test
    void testCrud() {

        client
            .patch()
            .uri("/example/crud")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"name\": \"test\",\"singleEnum\": \"enum1\",\"multiEnum\": [\"enum1\",\"enum3\"]}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        List<ExampleEntity> entities = client
            .get()
            .uri(builder -> builder
                .path("/example/crud/_query/no-paging")
                .queryParam("where", "multiEnum in enum1,enum3")
                .build())
            .exchange()
            .expectBodyList(ExampleEntity.class)
            .returnResult()
            .getResponseBody();
        System.out.println(JSON.toJSONString(entities, SerializerFeature.PrettyFormat));

    }

    @Test
    void testJoin() {
        client
            .patch()
            .uri("/example/crud")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"id\": \"1\",\"name\": \"joinTest\",\"singleEnum\": \"enum1\",\"multiEnum\": [\"enum1\"]}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client
            .patch()
            .uri("/example/crud/_ext")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"exampleId\": \"1\",\"extName\": \"1\"}")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        {
            String result = client
                .get()
                .uri(builder -> builder
                    .path("/example/crud/_detail/_query")
                    .queryParam("where", "name is joinTest and extName is 1")
                    .build())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
            assertNotNull(result);
            System.out.println(JSON.toJSONString(JSON.parse(result), SerializerFeature.PrettyFormat));
        }

        {
            String result = client
                .get()
                .uri(builder -> builder
                    .path("/example/crud/_detail/_query_native")
                    .queryParam("where", "name is joinTest")
                    .build())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
            assertNotNull(result);
            System.out.println(JSON.toJSONString(JSON.parse(result), SerializerFeature.PrettyFormat));
        }

        {
            String result = client
                .get()
                .uri(builder -> builder
                    .path("/example/crud/_detail_logic/_query")
                    .queryParam("where", "name is joinTest")
                    .build())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
            assertNotNull(result);
            System.out.println(JSON.toJSONString(JSON.parse(result), SerializerFeature.PrettyFormat));
        }

        {
            String result = client
                .get()
                .uri(builder -> builder
                    .path("/example/crud/_detail_many/_query")
                    .queryParam("where", "name is joinTest")
                    .build())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
            assertNotNull(result);
            System.out.println(JSON.toJSONString(JSON.parse(result), SerializerFeature.PrettyFormat));
        }
    }

    @Test
    void testExport() {
        client
            .patch()
            .uri("/example/crud")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("[{\"name\": \"test-e\",\"singleEnum\": \"enum1\",\"multiEnum\": [\"enum1\",\"enum3\"]}," +
                           "{\"name\": \"test-e-2\",\"singleEnum\": \"enum1\",\"multiEnum\": [\"enum1\",\"enum3\"]}]")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();


        String body = client
            .get()
            .uri(builder -> builder
                .path("/example/crud/_export/test.csv")
                .queryParam("where", "name like test-e%")
                .build())
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        System.out.println(body);
    }

    @Test
    void testImport() {
        String body = client
            .post()
            .uri(builder -> builder
                .path("/example/crud/_import.csv")
                .build())
            .body(BodyInserters.fromMultipartData("file", new HttpEntity<>(new ClassPathResource("test.csv"))))
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        System.out.println(body);
    }
}