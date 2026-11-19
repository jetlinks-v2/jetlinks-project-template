package org.jetlinks.pro.demo;

import com.fasterxml.jackson.databind.JsonNode;
import org.hswebframework.web.authorization.Authentication;
import org.jetlinks.pro.assets.Asset;
import org.jetlinks.pro.assets.AssetType;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DemoRecordTest extends DemoIntegrationSupport {
    private WebTestClient client;

    @BeforeEach
    void prepareClient() {
        // H2 CLOB 驱动内部包含阻塞读取，测试 WebFlux 入口使用允许该驱动行为的线程。
        client = WebTestClient.bindToController(controller)
            .webFilter((exchange, chain) -> chain.filter(exchange)
                .contextWrite(context -> context.put(Authentication.class, fixtureCreator()))
                .subscribeOn(Schedulers.boundedElastic()))
            .build();
    }

    @Test
    void crudUsesServerCreationTimeAndPreservesItOnUpdate() {
        DemoInspectionTeamEntity team = team(null);
        long started = System.currentTimeMillis();
        DemoRecordEntity created = client.post().uri("/demo/record")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("name", "test-record", "description", "初始说明", "createTime", 1,
                "inspectionTeamId", team.getId(), "inspectionItems", List.of(Map.of(
                    "name", "消防通道检查", "requirement", "通道无堆放"))))
            .exchange().expectStatus().is2xxSuccessful()
            .expectBody(DemoRecordEntity.class).returnResult().getResponseBody();
        assertNotNull(created);
        assertNotNull(created.getId());

        DemoRecordEntity persisted = service.findById(created.getId()).block(TIMEOUT);
        assertNotNull(persisted);
        assertNotNull(persisted.getCreateTime());
        assertTrue(persisted.getCreateTime() >= started);
        assertEquals(team.getId(), persisted.getInspectionTeamId());
        assertEquals("消防通道检查", persisted.getInspectionItems().get(0).getName());
        assertEquals("通道无堆放", persisted.getInspectionItems().get(0).getRequirement());
        long createTime = persisted.getCreateTime();

        client.put().uri("/demo/record/{id}", created.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("name", "test-updated", "description", "更新说明", "createTime", 2))
            .exchange().expectStatus().is2xxSuccessful().expectBody(Boolean.class).isEqualTo(true);

        DemoRecordEntity updated = service.findById(created.getId()).block(TIMEOUT);
        assertNotNull(updated);
        assertEquals("test-updated", updated.getName());
        assertEquals("更新说明", updated.getDescription());
        assertEquals(createTime, updated.getCreateTime());
        assertEquals(team.getId(), updated.getInspectionTeamId());
        assertEquals(1, updated.getInspectionItems().size());

        JsonNode page = client.post().uri("/demo/record/_query")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("pageIndex", 0, "pageSize", 10,
                "terms", List.of(Map.of("column", "id", "termType", "eq", "value", created.getId()))))
            .exchange().expectStatus().is2xxSuccessful()
            .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertNotNull(page);
        assertEquals(1, page.path("total").asInt());
        assertEquals(created.getId(), page.path("data").get(0).path("id").asText());

        client.delete().uri("/demo/record/{id}", created.getId())
            .exchange().expectStatus().is2xxSuccessful();
        assertNull(service.findById(created.getId()).block(TIMEOUT));
    }

    @Test
    void registeredAssetSupplierResolvesBothBusinessAssetTypes() {
        DemoInspectionTeamEntity team = team(null);
        DemoRecordEntity created = record("test-asset", team.getId());
        Asset resolved = assetManager.getAsset(DemoAssetType.record, created.getId()).block(TIMEOUT);
        assertNotNull(resolved);
        assertEquals(created.getId(), resolved.getId());
        assertEquals("test-asset", resolved.getName());
        assertEquals(DemoAssetType.TYPE_ID, resolved.getType().getId());
        assertSame(DemoAssetType.record, AssetType.of(DemoAssetType.TYPE_ID));
        Asset resolvedTeam = assetManager.getAsset(DemoAssetType.inspectionTeam, team.getId()).block(TIMEOUT);
        assertNotNull(resolvedTeam);
        assertEquals(team.getName(), resolvedTeam.getName());
        assertSame(DemoAssetType.inspectionTeam, AssetType.of(DemoAssetType.TEAM_TYPE_ID));
        assertNull(assetManager.getAsset(DemoAssetType.record, "missing-record").block(TIMEOUT));
        assertNull(assetManager.getAsset(DemoAssetType.inspectionTeam, "missing-team").block(TIMEOUT));
    }
}
