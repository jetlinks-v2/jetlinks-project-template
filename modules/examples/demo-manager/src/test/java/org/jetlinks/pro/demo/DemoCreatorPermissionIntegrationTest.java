package org.jetlinks.pro.demo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.starter.jackson.CustomJackson2JsonDecoder;
import org.hswebframework.web.starter.jackson.CustomJackson2jsonEncoder;
import org.jetlinks.pro.assets.AssetManager;
import org.jetlinks.pro.assets.AssetsHolder;
import org.jetlinks.pro.assets.impl.DimensionAssetsHolderProvider;
import org.jetlinks.pro.assets.impl.ctx.LocalAssetAccessesHolderContext;
import org.jetlinks.pro.assets.impl.supports.BusinessApplicationAssetsAccessSupport;
import org.jetlinks.pro.assets.impl.supports.CreatorAssetsAccessSupport;
import org.jetlinks.pro.assets.scope.AssetScope;
import org.jetlinks.pro.assets.scope.AssetScopeSources;
import org.jetlinks.pro.authorize.BusinessApplicationDimension;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.test.web.TestAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 正式 CRUD、创建信息事件与创建人资产范围共同执行 H2 查询，不替代权限判断。 */
@SpringJUnitConfig({DemoIntegrationTestConfiguration.class, DemoCreatorPermissionIntegrationTest.CreatorFixtures.class})
@TestPropertySource(properties = "spring.r2dbc.url=r2dbc:h2:mem:///demo-creator-permission;DB_CLOSE_DELAY=-1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoCreatorPermissionIntegrationTest extends DemoIntegrationSupport {
    private static final AssetScope APP = AssetScope.of(BusinessApplicationDimension.TYPE, "creator-permission-app");

    @Autowired
    private List<CodecCustomizer> codecCustomizers;

    @Test
    void ordinaryUsersCreateAndPageOnlyTheirOwnTeamsAndPlans() throws Exception {
        TestAuthentication first = operator("inspection-member-first");
        TestAuthentication second = operator("inspection-member-second");
        String firstName = "创建人权限回归-第一组";
        String secondName = "创建人权限回归-第二组";
        DemoInspectionTeamEntity firstTeam = createTeam(first, firstName, second.getUser().getId());
        DemoInspectionTeamEntity secondTeam = createTeam(second, secondName, first.getUser().getId());
        DemoRecordEntity firstPlan = createPlan(first, firstName, firstTeam.getId(), second.getUser().getId());
        DemoRecordEntity secondPlan = createPlan(second, secondName, secondTeam.getId(), first.getUser().getId());

        assertCreation(teams.findById(firstTeam.getId()).block(TIMEOUT), first);
        assertCreation(teams.findById(secondTeam.getId()).block(TIMEOUT), second);
        assertCreation(service.findById(firstPlan.getId()).block(TIMEOUT), first);
        assertCreation(service.findById(secondPlan.getId()).block(TIMEOUT), second);
        assertEquals(Set.of(firstTeam.getId()), teamIds(first, firstName, secondName));
        assertEquals(Set.of(secondTeam.getId()), teamIds(second, firstName, secondName));
        assertEquals(Set.of(firstPlan.getId()), planIds(first, firstName, secondName));
        assertEquals(Set.of(secondPlan.getId()), planIds(second, firstName, secondName));
        assertThrows(AccessDenyException.class, () -> scoped(first,
            controller.getById(secondPlan.getId()), APP).block(TIMEOUT));

        DemoRecordEntity decodedPlan = objectMapper.readValue(
            "{\"creatorId\":\"forged\",\"createTime\":1}", DemoRecordEntity.class);
        DemoInspectionTeamEntity decodedTeam = objectMapper.readValue(
            "{\"creatorId\":\"forged\",\"createTime\":1}", DemoInspectionTeamEntity.class);
        assertNull(decodedPlan.getCreatorId());
        assertNull(decodedPlan.getCreateTime());
        assertNull(decodedTeam.getCreatorId());
        assertNull(decodedTeam.getCreateTime());
        assertNull(decodedPlan.getExtension("creatorId"));
        assertNull(decodedPlan.getExtension("createTime"));
        assertNull(decodedTeam.getExtension("creatorId"));
        assertNull(decodedTeam.getExtension("createTime"));
    }

    @Test
    void webFluxResponsesCannotEchoForgedCreationFieldsThroughExtensions() throws Exception {
        TestAuthentication owner = operator("inspection-http-creation-owner");
        WebTestClient client = WebTestClient.bindToController(teamController, controller)
            .httpMessageCodecs(codecs -> {
                codecCustomizers.forEach(customizer -> customizer.customize(codecs));
                assertTrue(codecs.getReaders().stream().anyMatch(reader ->
                    reader instanceof DecoderHttpMessageReader<?> messageReader
                        && messageReader.getDecoder() instanceof CustomJackson2JsonDecoder decoder
                        && decoder.getObjectMapper() == objectMapper));
                assertTrue(codecs.getWriters().stream().anyMatch(writer ->
                    writer instanceof EncoderHttpMessageWriter<?> messageWriter
                        && messageWriter.getEncoder() instanceof CustomJackson2jsonEncoder encoder
                        && encoder.getObjectMapper() == objectMapper));
            })
            // 与既有 HTTP 测试保持一致，H2 CLOB 读取不在 WebFlux 非阻塞线程执行。
            .webFilter((exchange, chain) -> Mono.defer(() -> scoped(owner, chain.filter(exchange), APP))
                .subscribeOn(Schedulers.boundedElastic()))
            .build();

        JsonNode createdTeam = httpEntity(client, "POST", "/demo/inspection-team", Map.of(
            "name", "HTTP创建信息班组", "creatorId", "forged-http-user", "createTime", 1,
            "workCenter", "service-center-a"));
        String teamId = createdTeam.path("id").asText();
        JsonNode createdPlan = httpEntity(client, "POST", "/demo/record", Map.of(
            "name", "HTTP创建信息计划", "creatorId", "forged-http-user", "createTime", 1,
            "inspectionTeamId", teamId, "inspectionItems", List.of(Map.of(
                "name", "消防设施状态", "requirement", "检查设施完好并记录状态")),
            "workCenter", "service-center-a"));
        for (Map.Entry<String, JsonNode> entry : Map.of(
            "/demo/inspection-team", createdTeam, "/demo/record", createdPlan).entrySet()) {
            JsonNode response = entry.getValue();
            assertFalse(response.path("id").asText().isEmpty());
            assertEquals(owner.getUser().getId(), response.path("creatorId").asText());
            assertTrue(response.path("createTime").asLong() > 1L);
            assertEquals("service-center-a", response.path("workCenter").asText());
            String path = entry.getKey() + "/" + response.path("id").asText();
            JsonNode stored = httpEntity(client, "GET", path, null);
            assertEquals(response.path("creatorId"), stored.path("creatorId"));
            assertEquals(response.path("createTime"), stored.path("createTime"));

            client.put().uri(path).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("description", "修改业务说明", "creatorId", "forged-update-user", "createTime", 1))
                .exchange().expectStatus().is2xxSuccessful().expectBody(Boolean.class).isEqualTo(true);
            JsonNode updated = httpEntity(client, "GET", path, null);
            assertEquals("修改业务说明", updated.path("description").asText());
            assertEquals(response.path("creatorId"), updated.path("creatorId"));
            assertEquals(response.path("createTime"), updated.path("createTime"));
        }
        assertCreation(teams.findById(teamId).block(TIMEOUT), owner);
        assertCreation(service.findById(createdPlan.path("id").asText()).block(TIMEOUT), owner);
    }

    @Test
    void standardUpdatesPreservePersistedCreatorAndCreationTime() {
        TestAuthentication owner = operator("inspection-owner-update");
        DemoInspectionTeamEntity team = createTeam(owner, "创建信息不可修改班组", "forged-create-user");
        DemoRecordEntity plan = createPlan(owner, "创建信息不可修改计划", team.getId(), "forged-create-user");
        Long teamTime = teams.findById(team.getId()).block(TIMEOUT).getCreateTime();
        Long planTime = service.findById(plan.getId()).block(TIMEOUT).getCreateTime();

        DemoInspectionTeamEntity teamPatch = new DemoInspectionTeamEntity();
        teamPatch.setName("修改后班组");
        teamPatch.setCreatorId("forged-update-user");
        teamPatch.setCreateTime(1L);
        assertEquals(true, scoped(owner, teamController.update(team.getId(), Mono.just(teamPatch)), APP).block(TIMEOUT));
        DemoRecordEntity planPatch = new DemoRecordEntity();
        planPatch.setName("修改后计划");
        planPatch.setCreatorId("forged-update-user");
        planPatch.setCreateTime(1L);
        assertEquals(true, scoped(owner, controller.update(plan.getId(), Mono.just(planPatch)), APP).block(TIMEOUT));

        DemoInspectionTeamEntity storedTeam = teams.findById(team.getId()).block(TIMEOUT);
        DemoRecordEntity storedPlan = service.findById(plan.getId()).block(TIMEOUT);
        assertEquals("修改后班组", storedTeam.getName());
        assertEquals("修改后计划", storedPlan.getName());
        assertEquals(owner.getUser().getId(), storedTeam.getCreatorId());
        assertEquals(owner.getUser().getId(), storedPlan.getCreatorId());
        assertEquals(teamTime, storedTeam.getCreateTime());
        assertEquals(planTime, storedPlan.getCreateTime());
    }

    private DemoInspectionTeamEntity createTeam(TestAuthentication owner, String name, String forgedCreator) {
        DemoInspectionTeamEntity input = new DemoInspectionTeamEntity();
        input.setName(name);
        input.setCreatorId(forgedCreator);
        input.setCreateTime(1L);
        return scoped(owner, teamController.add(Mono.just(input)), APP).block(TIMEOUT);
    }

    private DemoRecordEntity createPlan(TestAuthentication owner, String name, String teamId, String forgedCreator) {
        DemoRecordEntity input = recordInput(name, teamId);
        input.setCreatorId(forgedCreator);
        input.setCreateTime(1L);
        return scoped(owner, controller.add(Mono.just(input)), APP).block(TIMEOUT);
    }

    private Set<String> teamIds(TestAuthentication owner, String... names) {
        PagerResult<DemoInspectionTeamEntity> result = scoped(owner,
            teamController.queryPager(named(names)), APP).block(TIMEOUT);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        return result.getData().stream().map(DemoInspectionTeamEntity::getId)
            .collect(java.util.stream.Collectors.toSet());
    }

    private Set<String> planIds(TestAuthentication owner, String... names) {
        PagerResult<DemoRecordEntity> result = scoped(owner,
            controller.queryPager(named(names)), APP).block(TIMEOUT);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        return result.getData().stream().map(DemoRecordEntity::getId)
            .collect(java.util.stream.Collectors.toSet());
    }

    private QueryParamEntity named(String... names) {
        QueryParamEntity query = QueryParamEntity.of();
        query.toQuery().in("name", List.of(names));
        return query;
    }

    private static void assertCreation(RecordCreationEntity created, TestAuthentication owner) {
        assertNotNull(created);
        assertEquals(owner.getUser().getId(), created.getCreatorId());
        assertNotNull(created.getCreateTime());
        assertTrue(created.getCreateTime() > 1L);
    }

    private JsonNode httpEntity(WebTestClient client, String method, String path, Map<String, Object> body)
        throws Exception {
        String response;
        if ("GET".equals(method)) {
            response = client.get().uri(path).exchange().expectStatus().is2xxSuccessful()
                .expectBody(String.class).returnResult().getResponseBody();
        } else {
            response = client.post().uri(path).contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange().expectStatus().is2xxSuccessful().expectBody(String.class).returnResult().getResponseBody();
        }
        assertNotNull(response);
        // 直接解析原始响应并拒绝重复键，避免反序列化实体再次忽略只读属性而漏掉回显污染。
        return objectMapper.reader().with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(response);
    }

    private TestAuthentication operator(String id) {
        TestAuthentication authentication = new TestAuthentication(id);
        authentication.addPermission(DemoAssetType.TEAM_TYPE_ID, "query", "save");
        authentication.addPermission(DemoAssetType.TYPE_ID, "query", "save");
        authentication.addDimension("role", "creator-range-role", Map.of("dataAccess", List.of(
            Map.of("assetType", DemoAssetType.TEAM_TYPE_ID, "accesses", List.of(Map.of("supportId", "creator"))),
            Map.of("assetType", DemoAssetType.TYPE_ID, "accesses", List.of(Map.of("supportId", "creator"))))));
        authentication.addDimension(APP.type(), APP.id(), Map.of(AssetScopeSources.POLICY_OPTION_KEY,
            Map.of(DemoAssetType.TEAM_TYPE_ID, "SELECT_AND_BIND", DemoAssetType.TYPE_ID, "SELECT_AND_BIND")));
        return authentication;
    }

    @Override
    AssetsHolder holder(Authentication authentication) {
        return new DimensionAssetsHolderProvider(assetManager, bindings,
            List.of(new BusinessApplicationAssetsAccessSupport(), new CreatorAssetsAccessSupport(assetManager)),
            new LocalAssetAccessesHolderContext()).createHolder(authentication).single().block(TIMEOUT);
    }

    @Configuration(proxyBeanMethods = false)
    static class CreatorFixtures {
        @Bean
        CreatorAssetsAccessSupport creatorAssetAccess(AssetManager assets) {
            return new CreatorAssetsAccessSupport(assets);
        }

    }
}
