package org.jetlinks.pro.demo;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.jetlinks.pro.assets.AssetBindManager;
import org.jetlinks.pro.assets.AssetBindRequest;
import org.jetlinks.pro.assets.AssetBinding;
import org.jetlinks.pro.assets.AssetUnbindRequest;
import org.jetlinks.pro.assets.AssetsUtils;
import org.jetlinks.pro.assets.CompositeAssetBindManager;
import org.jetlinks.pro.assets.crud.CrudAssetPermission;
import org.jetlinks.pro.assets.scope.AssetScope;
import org.jetlinks.pro.assets.scope.AssetScopeSources;
import org.jetlinks.pro.authorize.BusinessApplicationDimension;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.asset.DemoInspectionTeamAssetBindProvider;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.test.web.TestAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** 班组自身与巡检计划共用真实动态维度权限，虚拟自身绑定不能写入业务主键。 */
@SpringJUnitConfig({DemoIntegrationTestConfiguration.class, DemoInspectionTeamAssetBindProvider.class})
@TestPropertySource(properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///demo-team-data-access;DB_CLOSE_DELAY=-1",
    "jetlinks.assets.asset-tables[demo_inspection_team]=demo-inspection-team"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoInspectionTeamDataAccessTest extends DemoIntegrationSupport {
    private static final String TEAM = DemoAssetType.TEAM_TYPE_ID;
    private static final String DYNAMIC_TEAM = "dynamic_" + TEAM;
    private static final AssetScope APP = AssetScope.of(BusinessApplicationDimension.TYPE, "team-range-app");
    private static final String OTHER_APP = "team-range-other-app";

    @Test
    void membersReadTheirOwnTeamAndDescendantsWhileSiblingsAndNonmembersAreDenied() {
        DemoInspectionTeamEntity center = team(null);
        DemoInspectionTeamEntity electrical = team(center.getId());
        DemoInspectionTeamEntity electricalChild = team(electrical.getId());
        DemoInspectionTeamEntity fire = team(center.getId());
        TestAuthentication operator = member(electrical.getId(), DYNAMIC_TEAM);

        assertEquals(Set.of(electrical.getId(), electricalChild.getId()), visibleTeams(operator));
        assertTeamReadable(operator, electrical);
        assertTeamReadable(operator, electricalChild);
        assertThrows(AccessDenyException.class, () -> as(operator,
            teamController.getById(center.getId())).block(TIMEOUT));
        assertThrows(AccessDenyException.class, () -> as(operator,
            teamController.getById(fire.getId())).block(TIMEOUT));
        TestAuthentication nonmember = member(null, DYNAMIC_TEAM);
        assertEquals(Set.of(), visibleTeams(nonmember));
        assertThrows(AccessDenyException.class, () -> as(nonmember,
            teamController.getById(electrical.getId())).block(TIMEOUT));

        DemoRecordEntity ownPlan = record("机电班巡检计划", electrical.getId());
        DemoRecordEntity childPlan = record("机电下级班巡检计划", electricalChild.getId());
        DemoRecordEntity siblingPlan = record("消防班巡检计划", fire.getId());
        assertEquals(Set.of(ownPlan.getId(), childPlan.getId()), as(operator, planIds()).block(TIMEOUT));
        assertNotNull(as(operator, controller.getById(childPlan.getId())).block(TIMEOUT));
        assertThrows(AccessDenyException.class, () -> as(operator,
            controller.getById(siblingPlan.getId())).block(TIMEOUT));
    }

    @Test
    void applicationScopeIntersectsTeamSubtreeForTeamAndRecordQueriesAndPointReads() {
        DemoInspectionTeamEntity center = team(null);
        DemoInspectionTeamEntity electrical = team(center.getId());
        DemoInspectionTeamEntity localChild = team(electrical.getId());
        DemoInspectionTeamEntity foreignChild = team(electrical.getId());
        DemoInspectionTeamEntity fire = team(center.getId());
        for (DemoInspectionTeamEntity local : List.of(center, electrical, localChild, fire)) {
            bind(APP.type(), APP.id(), TEAM, local.getId());
        }
        bind(APP.type(), OTHER_APP, TEAM, foreignChild.getId());
        TestAuthentication operator = applicationMember(electrical.getId());

        assertEquals(Set.of(electrical.getId(), localChild.getId()),
            scoped(operator, teamIds(), APP).block(TIMEOUT));
        assertNotNull(scoped(operator, teamController.getById(electrical.getId()), APP).block(TIMEOUT));
        assertNotNull(scoped(operator, teamController.getById(localChild.getId()), APP).block(TIMEOUT));
        for (DemoInspectionTeamEntity hidden : List.of(center, foreignChild, fire)) {
            assertThrows(AccessDenyException.class, () -> scoped(operator,
                teamController.getById(hidden.getId()), APP).block(TIMEOUT));
        }
        assertEquals(Set.of(), scoped(applicationMember(null), teamIds(), APP).block(TIMEOUT));

        DemoRecordEntity ownPlan = record("当前应用机电计划", electrical.getId());
        DemoRecordEntity childPlan = record("当前应用下级计划", localChild.getId());
        DemoRecordEntity foreignPlan = record("其他应用下级计划", foreignChild.getId());
        DemoRecordEntity siblingPlan = record("当前应用消防计划", fire.getId());
        for (DemoRecordEntity local : List.of(ownPlan, childPlan, siblingPlan)) {
            bind(APP.type(), APP.id(), DemoAssetType.TYPE_ID, local.getId());
        }
        bind(APP.type(), OTHER_APP, DemoAssetType.TYPE_ID, foreignPlan.getId());
        assertEquals(Set.of(ownPlan.getId(), childPlan.getId()), scoped(operator, planIds(), APP).block(TIMEOUT));
        assertNotNull(scoped(operator, controller.getById(childPlan.getId()), APP).block(TIMEOUT));
        for (DemoRecordEntity hidden : List.of(foreignPlan, siblingPlan)) {
            assertThrows(AccessDenyException.class, () -> scoped(operator,
                controller.getById(hidden.getId()), APP).block(TIMEOUT));
        }
    }

    @Test
    void virtualSelfBindingsCannotRewritePrimaryKeysOrGrantAnotherTeamsAssets() {
        assertInstanceOf(CompositeAssetBindManager.class, bindings);
        DemoInspectionTeamEntity electrical = team(null);
        DemoInspectionTeamEntity fire = team(null);
        TestAuthentication operator = member(electrical.getId(), DYNAMIC_TEAM);
        List<String> ids = List.of(electrical.getId(), fire.getId());
        assertEquals(List.of(electrical.getId()), bindings.getBindings(TEAM, List.of(electrical.getId()),
            DemoAssetType.inspectionTeam, ids).map(AssetBinding::getAssetId).collectList().block(TIMEOUT));
        assertEquals(0L, bindings.getBindings("organization", List.of(electrical.getId()),
            DemoAssetType.inspectionTeam, ids).count().block(TIMEOUT));

        bindings.bindAssets(Mono.just(AssetBindRequest.builder().targetType(TEAM).targetId(electrical.getId())
                .assetType(TEAM).assetIdList(List.of(fire.getId())).allPermission(true).build()))
            .as(AssetsUtils.setMonoBindSource(AssetBindManager.SOURCE_MANUAL)).block(TIMEOUT);
        assertUnchangedSelfBindings(operator, electrical, fire);

        AssetUnbindRequest unbind = new AssetUnbindRequest();
        unbind.setTargetType(TEAM);
        unbind.setTargetId(electrical.getId());
        unbind.setAssetType(TEAM);
        unbind.setAssetIdList(ids);
        bindings.unbindAssets(Mono.just(unbind))
            .as(AssetsUtils.setMonoBindSource(AssetBindManager.SOURCE_MANUAL)).block(TIMEOUT);
        assertUnchangedSelfBindings(operator, electrical, fire);

        assertEquals(0, bindings.unbindAllAssets(TEAM, ids)
            .as(AssetsUtils.setMonoBindSource(AssetBindManager.SOURCE_MANUAL)).block(TIMEOUT));
        assertUnchangedSelfBindings(operator, electrical, fire);

        bindings.changePermission(TEAM, List.of(electrical.getId()), DemoAssetType.inspectionTeam,
            List.of(fire.getId()), CrudAssetPermission.read).block(TIMEOUT);
        assertUnchangedSelfBindings(operator, electrical, fire);
    }

    private TestAuthentication applicationMember(String teamId) {
        TestAuthentication operator = member(teamId, DYNAMIC_TEAM);
        operator.addDimension(APP.type(), APP.id(), Map.of(AssetScopeSources.POLICY_OPTION_KEY,
            Map.of(TEAM, "SELECT", DemoAssetType.TYPE_ID, "SELECT")));
        return operator;
    }

    private Set<String> visibleTeams(TestAuthentication operator) {
        return as(operator, teamIds()).block(TIMEOUT);
    }

    private Mono<Set<String>> teamIds() {
        return teamController.query(QueryParamEntity.of().noPaging())
            .map(DemoInspectionTeamEntity::getId).collect(Collectors.toSet());
    }

    private Mono<Set<String>> planIds() {
        return controller.query(QueryParamEntity.of().noPaging())
            .map(DemoRecordEntity::getId).collect(Collectors.toSet());
    }

    private void assertTeamReadable(TestAuthentication operator, DemoInspectionTeamEntity expected) {
        DemoInspectionTeamEntity actual = as(operator, teamController.getById(expected.getId())).block(TIMEOUT);
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
    }

    private void assertUnchangedSelfBindings(TestAuthentication operator,
                                             DemoInspectionTeamEntity electrical,
                                             DemoInspectionTeamEntity fire) {
        for (DemoInspectionTeamEntity expected : List.of(electrical, fire)) {
            DemoInspectionTeamEntity stored = teams.findById(expected.getId()).block(TIMEOUT);
            assertNotNull(stored);
            assertEquals(expected.getId(), stored.getId());
            assertEquals(expected.getName(), stored.getName());
        }
        assertEquals(Set.of(electrical.getId()), visibleTeams(operator));
        assertThrows(AccessDenyException.class, () -> as(operator,
            teamController.getById(fire.getId())).block(TIMEOUT));
        assertEquals(0, bindingRepository.createQuery().where("assetType", TEAM)
            .in("assetId", List.of(electrical.getId(), fire.getId())).count().block(TIMEOUT));
    }
}
