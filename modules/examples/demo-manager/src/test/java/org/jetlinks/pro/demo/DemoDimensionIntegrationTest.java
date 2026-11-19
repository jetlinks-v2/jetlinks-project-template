package org.jetlinks.pro.demo;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityBeforeModifyEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.pro.assets.AssetBindManager;
import org.jetlinks.pro.assets.AssetBindRequest;
import org.jetlinks.pro.assets.AssetUnbindRequest;
import org.jetlinks.pro.assets.AssetsHolder;
import org.jetlinks.pro.assets.AssetsUtils;
import org.jetlinks.pro.assets.crud.CrudAssetPermission;
import org.jetlinks.pro.assets.dynamic.DynamicDimensionState;
import org.jetlinks.pro.assets.dynamic.entity.DynamicDimensionEntity;
import org.jetlinks.pro.assets.dynamic.entity.DynamicDimensionTypeEntity;
import org.jetlinks.pro.assets.impl.entity.DimensionClosureEntity;
import org.jetlinks.pro.assets.scope.AssetScope;
import org.jetlinks.pro.assets.scope.AssetScopeSources;
import org.jetlinks.pro.authorize.BusinessApplicationDimension;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.test.web.TestAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DemoDimensionIntegrationTest extends DemoIntegrationSupport {
    @Autowired FailureProbe failures;
    @Autowired DatabaseClient databaseClient;

    @Test
    void businessTreeProjectsAndMovesItsExistingSubtreeInOneTransaction() {
        DemoInspectionTeamEntity first = team(null);
        DemoInspectionTeamEntity second = team(null);
        DemoInspectionTeamEntity child = team(first.getId());
        DemoInspectionTeamEntity leaf = team(child.getId());
        DynamicDimensionEntity projected = dimensions.findById(DemoAssetType.TEAM_TYPE_ID, child.getId()).block(TIMEOUT);
        assertNotNull(projected);
        assertEquals(child.getName(), projected.getName());
        assertEquals(key(first.getId()), projected.getParentId());
        assertEquals(DynamicDimensionState.enabled, projected.getState());
        assertTrue(closure(first.getId(), leaf.getId()));

        assertEquals(1, teams.createUpdate().set(DemoInspectionTeamEntity::getParentId, second.getId())
            .where(DemoInspectionTeamEntity::getId, child.getId()).execute().block(TIMEOUT));
        assertFalse(closure(first.getId(), leaf.getId()));
        assertTrue(closure(second.getId(), leaf.getId()));
        assertEquals(key(second.getId()), dimensions.findById(DemoAssetType.TEAM_TYPE_ID, child.getId())
            .block(TIMEOUT).getParentId());

        DemoInspectionTeamEntity rename = teams.findById(child.getId()).block(TIMEOUT);
        rename.setName("消防巡检二组");
        teams.save(rename).block(TIMEOUT);
        assertEquals("消防巡检二组", dimensions.findById(DemoAssetType.TEAM_TYPE_ID, child.getId())
            .block(TIMEOUT).getName());
        DemoInspectionTeamEntity rootPatch = new DemoInspectionTeamEntity();
        rootPatch.setParentId(leaf.getId());
        assertThrows(ValidationException.class, () -> teams.updateById(second.getId(), rootPatch).block(TIMEOUT));
        assertNull(teams.findById(second.getId()).block(TIMEOUT).getParentId());
        assertTrue(closure(second.getId(), leaf.getId()));
    }

    @Test
    void realHolderIsolatesSiblingsAndIncludesOnlyTheMembersEnabledSubtree() {
        DemoInspectionTeamEntity parent = team(null);
        DemoInspectionTeamEntity left = team(parent.getId());
        DemoInspectionTeamEntity right = team(parent.getId());
        DemoRecordEntity leftRecord = record("左班消防计划", left.getId());
        DemoRecordEntity rightRecord = record("右班消防计划", right.getId());
        TestAuthentication leftMember = member(left.getId());
        TestAuthentication parentMember = member(parent.getId());
        assertEquals(Set.of(leftRecord.getId()), visible(leftMember));
        assertEquals(Set.of(leftRecord.getId(), rightRecord.getId()), visible(parentMember));
        assertEquals(Set.of(), visible(member(null)));

        DemoRecordEntity patch = new DemoRecordEntity();
        patch.setName("已确认左班计划");
        assertEquals(true, as(leftMember, controller.update(leftRecord.getId(), Mono.just(patch))).block(TIMEOUT));
        assertThrows(AccessDenyException.class, () -> as(leftMember,
            controller.update(rightRecord.getId(), Mono.just(patch))).block(TIMEOUT));
        assertEquals("右班消防计划", service.findById(rightRecord.getId()).block(TIMEOUT).getName());

        AssetsHolder previousHolder = holder(leftMember);
        setState(parent.getId(), DynamicDimensionState.disabled);
        assertEquals(Set.of(), visible(leftMember));
        assertFalse(previousHolder.hasPermission(DemoAssetType.TYPE_ID, List.of(leftRecord.getId()), true,
            CrudAssetPermission.read).block(TIMEOUT));
        setState(parent.getId(), DynamicDimensionState.enabled);
        assertEquals(Set.of(leftRecord.getId()), visible(leftMember));
        setState(left.getId(), DynamicDimensionState.disabled);
        assertEquals(Set.of(rightRecord.getId()), visible(parentMember));
        setState(left.getId(), DynamicDimensionState.enabled);

        DynamicDimensionTypeEntity type = typeRepository.findById(DemoAssetType.TEAM_TYPE_ID).block(TIMEOUT);
        type.setState(DynamicDimensionState.disabled);
        typeRepository.save(type).then(typeRegistry.refresh()).block(TIMEOUT);
        assertEquals(Set.of(), visible(leftMember));
        type.setState(DynamicDimensionState.enabled);
        typeRepository.save(type).then(typeRegistry.refresh()).block(TIMEOUT);
        assertEquals(Set.of(leftRecord.getId()), visible(leftMember));
    }

    @Test
    void changingTeamNeedsTargetManagementWhileOrdinaryEditsKeepExistingPlanPermission() {
        DemoInspectionTeamEntity parent = team(null);
        DemoInspectionTeamEntity source = team(parent.getId());
        DemoInspectionTeamEntity target = team(parent.getId());
        DemoRecordEntity plan = record("日常计划", source.getId());
        TestAuthentication operator = member(parent.getId(), BusinessApplicationDimension.TYPE);
        DemoRecordEntity rename = new DemoRecordEntity();
        rename.setName("日常消防计划");
        assertEquals(true, as(operator, controller.update(plan.getId(), Mono.just(rename))).block(TIMEOUT));
        DemoRecordEntity migrate = new DemoRecordEntity();
        migrate.setInspectionTeamId(target.getId());
        assertThrows(AccessDenyException.class, () -> as(operator,
            controller.update(plan.getId(), Mono.just(migrate))).block(TIMEOUT));
        assertEquals(source.getId(), service.findById(plan.getId()).block(TIMEOUT).getInspectionTeamId());

        bind(BusinessApplicationDimension.TYPE, "inspection-app", DemoAssetType.TEAM_TYPE_ID, target.getId());
        assertEquals(true, as(operator, controller.update(plan.getId(), Mono.just(migrate))).block(TIMEOUT));
        assertEquals(target.getId(), service.findById(plan.getId()).block(TIMEOUT).getInspectionTeamId());
        assertEquals(1, service.findById(plan.getId()).block(TIMEOUT).getInspectionItems().size());

        DemoInspectionTeamEntity newTeam = new DemoInspectionTeamEntity();
        newTeam.setName("下级巡检班组");
        newTeam.setParentId(source.getId());
        assertThrows(AccessDenyException.class, () -> as(operator, teamController.add(Mono.just(newTeam))).block(TIMEOUT));
        bind(BusinessApplicationDimension.TYPE, "inspection-app", DemoAssetType.TEAM_TYPE_ID, source.getId());
        assertNotNull(as(operator, teamController.add(Mono.just(newTeam))).block(TIMEOUT));
    }

    @Test
    void invalidTeamsAndEmptyChecklistsAreRejectedThroughCreateSaveAndModify() {
        DemoInspectionTeamEntity team = team(null);
        assertThrows(BusinessException.class, () -> controller.add(Mono.just(
            recordInput("不存在班组的计划", "missing-team"))).block(TIMEOUT));
        DemoRecordEntity empty = recordInput("空清单", team.getId());
        empty.setInspectionItems(List.of());
        assertThrows(RuntimeException.class, () -> service.save(empty).block(TIMEOUT));
        DemoRecordEntity invalidItem = recordInput("缺少检查要求", team.getId());
        invalidItem.getInspectionItems().get(0).setRequirement(" ");
        assertThrows(RuntimeException.class, () -> service.insert(invalidItem).block(TIMEOUT));

        DemoRecordEntity plan = record("有效计划", team.getId());
        DemoRecordEntity clear = new DemoRecordEntity();
        clear.setInspectionTeamId("");
        assertThrows(RuntimeException.class, () -> service.updateById(plan.getId(), clear).block(TIMEOUT));
        setState(team.getId(), DynamicDimensionState.disabled);
        assertThrows(BusinessException.class, () -> service.save(recordInput("停用班组计划", team.getId())).block(TIMEOUT));
        DemoInspectionTeamEntity invalidChild = new DemoInspectionTeamEntity();
        invalidChild.setName("停用上级的子组");
        invalidChild.setParentId(team.getId());
        assertThrows(BusinessException.class, () -> teams.save(invalidChild).block(TIMEOUT));
        DemoRecordEntity rename = new DemoRecordEntity();
        rename.setName("保留计划");
        assertEquals(1, service.updateById(plan.getId(), Mono.just(rename)).block(TIMEOUT));
        assertEquals(team.getId(), service.findById(plan.getId()).block(TIMEOUT).getInspectionTeamId());
    }

    @Test
    void manualDirectBindingKeepsItsSingleSourceAndRejectsInvalidAssignmentAndClearing() {
        DemoInspectionTeamEntity source = team(null);
        DemoInspectionTeamEntity target = team(null);
        DemoRecordEntity plan = record("班组移交演示", source.getId());
        failures.type = DemoRecordEntity.class;
        try {
            assertThrows(IllegalStateException.class, () -> directBind(plan.getId(), target.getId()).block(TIMEOUT));
        } finally {
            failures.type = null;
        }
        assertEquals(source.getId(), service.findById(plan.getId()).block(TIMEOUT).getInspectionTeamId());
        directBind(plan.getId(), target.getId()).block(TIMEOUT);
        DemoRecordEntity moved = service.findById(plan.getId()).block(TIMEOUT);
        assertEquals(target.getId(), moved.getInspectionTeamId());
        assertEquals("班组移交演示", moved.getName());
        assertEquals(plan.getInspectionItems().get(0).getRequirement(), moved.getInspectionItems().get(0).getRequirement());
        assertEquals(1, bindings.getBindings(DemoAssetType.TEAM_TYPE_ID, List.of(target.getId()),
            DemoAssetType.record, List.of(plan.getId())).count().block(TIMEOUT));
        assertEquals(0, bindings.getBindings(DemoAssetType.TEAM_TYPE_ID, List.of(source.getId()),
            DemoAssetType.record, List.of(plan.getId())).count().block(TIMEOUT));
        directBind(plan.getId(), target.getId()).block(TIMEOUT);
        assertThrows(BusinessException.class, () -> directBind(plan.getId(), "missing-target").block(TIMEOUT));
        setState(source.getId(), DynamicDimensionState.disabled);
        assertThrows(BusinessException.class, () -> directBind(plan.getId(), source.getId()).block(TIMEOUT));
        AssetUnbindRequest unbind = new AssetUnbindRequest();
        unbind.setTargetType(DemoAssetType.TEAM_TYPE_ID);
        unbind.setTargetId(target.getId());
        unbind.setAssetType(DemoAssetType.TYPE_ID);
        unbind.setAssetIdList(List.of(plan.getId()));
        assertThrows(RuntimeException.class, () -> bindings.unbindAssets(Mono.just(unbind))
            .as(AssetsUtils.setMonoBindSource(AssetBindManager.SOURCE_MANUAL)).block(TIMEOUT));
        assertEquals(target.getId(), service.findById(plan.getId()).block(TIMEOUT).getInspectionTeamId());
    }

    @Test
    void deletingReferencedTeamIsRejectedUntilItsChildrenAndPlansAreRemoved() {
        DemoInspectionTeamEntity parent = team(null);
        DemoInspectionTeamEntity child = team(parent.getId());
        DemoRecordEntity plan = record("巡检保留计划", child.getId());
        assertThrows(BusinessException.class, () -> teams.deleteById(parent.getId()).block(TIMEOUT));
        assertThrows(BusinessException.class, () -> teams.createDelete()
            .where(DemoInspectionTeamEntity::getId, child.getId()).execute().block(TIMEOUT));
        assertNotNull(dimensions.findById(DemoAssetType.TEAM_TYPE_ID, child.getId()).block(TIMEOUT));
        service.deleteById(Flux.just(plan.getId())).block(TIMEOUT);
        assertEquals(1, teams.createDelete().where(DemoInspectionTeamEntity::getId, child.getId()).execute().block(TIMEOUT));
        assertNull(dimensions.findById(DemoAssetType.TEAM_TYPE_ID, child.getId()).block(TIMEOUT));
        assertFalse(closure(parent.getId(), child.getId()));
        assertEquals(1, teams.deleteById(parent.getId()).block(TIMEOUT));
    }

    @Test
    void scopeIntersectsDynamicTeamPermissionWithBothTenantAndApplicationMembership() {
        DemoInspectionTeamEntity team = team(null);
        DemoRecordEntity first = record("租户A应用一", team.getId());
        DemoRecordEntity second = record("租户A应用二", team.getId());
        DemoRecordEntity third = record("租户B应用一", team.getId());
        bind("tenant", "tenant-a", DemoAssetType.TYPE_ID, first.getId());
        bind("tenant", "tenant-a", DemoAssetType.TYPE_ID, second.getId());
        bind("tenant", "tenant-b", DemoAssetType.TYPE_ID, third.getId());
        bind(BusinessApplicationDimension.TYPE, "inspection-app", DemoAssetType.TYPE_ID, first.getId());
        bind(BusinessApplicationDimension.TYPE, "other-app", DemoAssetType.TYPE_ID, second.getId());
        bind(BusinessApplicationDimension.TYPE, "inspection-app", DemoAssetType.TYPE_ID, third.getId());
        TestAuthentication user = member(team.getId());
        user.addDimension("tenant", "tenant-a");
        Set<String> ids = scoped(user, controller.query(new QueryParamEntity().noPaging())
                .map(DemoRecordEntity::getId).collect(Collectors.toSet()),
            AssetScope.of("tenant", "tenant-a"),
            AssetScope.of(BusinessApplicationDimension.TYPE, "inspection-app"))
            .block(TIMEOUT);
        assertEquals(Set.of(first.getId()), ids);
    }

    @Test
    void automaticOwnershipBindingPreservesTheExplicitTeamAndCreatesNoSecondTeamBinding() {
        DemoInspectionTeamEntity parent = team(null);
        DemoInspectionTeamEntity selected = team(parent.getId());
        team(parent.getId());
        TestAuthentication user = member(parent.getId());
        user.addDimension("tenant", "tenant-create", Map.of(AssetScopeSources.POLICY_OPTION_KEY,
            Map.of(DemoAssetType.TYPE_ID, "SELECT_AND_BIND", DemoAssetType.TEAM_TYPE_ID, "IGNORE")));
        DemoRecordEntity created = scoped(user,
            controller.add(Mono.just(recordInput("班组归属演示", selected.getId()))),
            AssetScope.of("tenant", "tenant-create"),
            AssetScope.of(BusinessApplicationDimension.TYPE, "inspection-app"))
            .block(TIMEOUT);
        assertNotNull(created);
        assertEquals(selected.getId(), service.findById(created.getId()).block(TIMEOUT).getInspectionTeamId());
        assertEquals(1L, bindings.getBindings("tenant", List.of("tenant-create"), DemoAssetType.record,
            List.of(created.getId())).count().block(TIMEOUT));
        assertEquals(1L, bindings.getBindings(BusinessApplicationDimension.TYPE, List.of("inspection-app"),
            DemoAssetType.record, List.of(created.getId())).count().block(TIMEOUT));
        assertEquals(0, bindingRepository.createQuery().where("assetId", created.getId())
            .and("targetType", DemoAssetType.TEAM_TYPE_ID).count().block(TIMEOUT));
    }

    @Test
    void concurrentDslModificationsReadThePreviousCommittedSnapshotAfterTakingTheRecordLock() {
        DemoInspectionTeamEntity team = team(null);
        DemoRecordEntity plan = record("原计划", team.getId());
        ModifyGate gate = new ModifyGate(plan.getId());
        failures.gate = gate;
        // H2 驱动等待数据库行锁时会阻塞，测试订阅放入允许阻塞的线程。
        CompletableFuture<Integer> first = service.createUpdate().set("name", "第一次修改")
            .where("id", plan.getId()).execute().subscribeOn(Schedulers.boundedElastic()).toFuture();
        CompletableFuture<Integer> second = null;
        try {
            gate.firstReached.asMono().block(TIMEOUT);
            second = service.createUpdate().set("name", "第二次修改")
                .where("id", plan.getId()).execute()
                .doOnSubscribe(subscription -> gate.secondStarted.tryEmitEmpty())
                .subscribeOn(Schedulers.boundedElastic()).toFuture();
            gate.secondStarted.asMono().block(TIMEOUT);
            Mono.delay(java.time.Duration.ofMillis(100)).block(TIMEOUT);
            assertEquals(1, gate.entries.get());
            assertFalse(second.isDone());
            gate.releaseFirst.tryEmitEmpty();
            assertEquals(1, first.orTimeout(10, java.util.concurrent.TimeUnit.SECONDS).join());
            assertEquals(1, second.orTimeout(10, java.util.concurrent.TimeUnit.SECONDS).join());
            assertEquals(List.of("原计划", "第一次修改"), gate.beforeNames);
            assertEquals("第二次修改", service.findById(plan.getId()).block(TIMEOUT).getName());
        } finally {
            gate.releaseFirst.tryEmitEmpty();
            failures.gate = null;
            if (!first.isDone()) {
                first.cancel(true);
            }
            if (second != null && !second.isDone()) {
                second.cancel(true);
            }
        }
    }

    @Test
    void failuresAfterBusinessDirectoryAndClosureWritesRollbackAllThreeTables() {
        DemoInspectionTeamEntity parent = team(null);
        DemoInspectionTeamEntity otherParent = team(null);
        DemoInspectionTeamEntity child = team(parent.getId());
        String oldName = child.getName();
        for (Class<?> failType : List.of(DemoInspectionTeamEntity.class, DynamicDimensionEntity.class)) {
            failures.type = failType;
            try {
                DemoInspectionTeamEntity patch = new DemoInspectionTeamEntity();
                patch.setName("不得提交-" + failType.getSimpleName());
                patch.setParentId(otherParent.getId());
                assertThrows(IllegalStateException.class, () -> teams.updateById(child.getId(), patch).block(TIMEOUT),
                    failType.getName());
            } finally {
                failures.type = null;
            }
            assertEquals(oldName, teams.findById(child.getId()).block(TIMEOUT).getName());
            assertEquals(oldName, dimensions.findById(DemoAssetType.TEAM_TYPE_ID, child.getId()).block(TIMEOUT).getName());
            assertTrue(closure(parent.getId(), child.getId()));
            assertFalse(closure(otherParent.getId(), child.getId()));
        }
        // 公共闭包实体没有实体事件，通过真实数据库约束拒绝新父子关系，验证 SQL 失败回滚。
        databaseClient.sql("alter table s_dimension_closure add constraint demo_closure_reject check (not ("
            + "ancestor_id = '" + otherParent.getId() + "' and descendant_id = '" + child.getId() + "'))")
            .then().block(TIMEOUT);
        try {
            DemoInspectionTeamEntity patch = new DemoInspectionTeamEntity();
            patch.setParentId(otherParent.getId());
            assertThrows(RuntimeException.class, () -> teams.updateById(child.getId(), patch).block(TIMEOUT));
        } finally {
            databaseClient.sql("alter table s_dimension_closure drop constraint demo_closure_reject")
                .then().block(TIMEOUT);
        }
        assertEquals(parent.getId(), teams.findById(child.getId()).block(TIMEOUT).getParentId());
        assertEquals(key(parent.getId()), dimensions.findById(DemoAssetType.TEAM_TYPE_ID, child.getId())
            .block(TIMEOUT).getParentId());
        assertTrue(closure(parent.getId(), child.getId()));
        assertFalse(closure(otherParent.getId(), child.getId()));
        DemoRecordEntity record = record("计划事务回滚", child.getId());
        failures.type = DemoRecordEntity.class;
        try {
            DemoRecordEntity patch = new DemoRecordEntity();
            patch.setName("不得提交");
            assertThrows(IllegalStateException.class, () -> service.createUpdate().set("name", patch.getName())
                .where("id", record.getId()).execute().block(TIMEOUT));
        } finally {
            failures.type = null;
        }
        assertEquals("计划事务回滚", service.findById(record.getId()).block(TIMEOUT).getName());
    }

    @Test
    void saveOverloadsApplyTheSameLifecycleAndBatchPersistence() {
        DemoInspectionTeamEntity team = team(null);
        DemoRecordEntity first = recordInput("保存计划一", team.getId());
        first.setId(UUID.randomUUID().toString());
        service.save(Mono.just(first)).contextWrite(context -> context.put(Authentication.class, fixtureCreator())).block(TIMEOUT);
        DemoRecordEntity second = recordInput("保存计划二", team.getId());
        second.setId(UUID.randomUUID().toString());
        service.save(second).contextWrite(context -> context.put(Authentication.class, fixtureCreator())).block(TIMEOUT);
        first.setDescription("每日检查");
        second.setDescription("每周检查");
        service.save(List.of(first, second)).block(TIMEOUT);
        assertEquals("每日检查", service.findById(first.getId()).block(TIMEOUT).getDescription());
        assertEquals("每周检查", service.findById(second.getId()).block(TIMEOUT).getDescription());
        service.deleteById(Flux.just(first.getId(), second.getId())).block(TIMEOUT);
        assertNull(service.findById(first.getId()).block(TIMEOUT));
        assertNull(service.findById(second.getId()).block(TIMEOUT));
    }

    private Set<String> visible(TestAuthentication authentication) {
        return as(authentication, controller.query(new QueryParamEntity().noPaging())
            .map(DemoRecordEntity::getId).collect(Collectors.toSet())).block(TIMEOUT);
    }

    private void setState(String teamId, DynamicDimensionState state) {
        DemoInspectionTeamEntity patch = new DemoInspectionTeamEntity();
        patch.setState(state);
        teams.updateById(teamId, patch).block(TIMEOUT);
    }

    private boolean closure(String ancestor, String descendant) {
        return closureRepository.createQuery().where(DimensionClosureEntity::getType, DemoAssetType.TEAM_TYPE_ID)
            .and(DimensionClosureEntity::getAncestorId, ancestor)
            .and(DimensionClosureEntity::getDescendantId, descendant).count().map(count -> count > 0).block(TIMEOUT);
    }

    private Mono<Void> directBind(String recordId, String teamId) {
        return bindings.bindAssets(Mono.just(AssetBindRequest.builder()
            .targetType(DemoAssetType.TEAM_TYPE_ID).targetId(teamId).assetType(DemoAssetType.TYPE_ID)
            .assetIdList(List.of(recordId)).allPermission(true).build()))
            .as(AssetsUtils.setMonoBindSource(AssetBindManager.SOURCE_MANUAL));
    }

    private static String key(String teamId) {
        return AssetsUtils.createBindingTargetKey(DemoAssetType.TEAM_TYPE_ID, teamId);
    }

    /** 在正式 ORM 成功事件中注入业务失败，验证调用方事务确实覆盖投影及闭包。 */
    static class FailureProbe {
        volatile Class<?> type;
        volatile ModifyGate gate;

        @EventListener
        public void beforeModify(EntityBeforeModifyEvent<DemoRecordEntity> event) {
            ModifyGate current = gate;
            if (current != null && event.getEntityType() == DemoRecordEntity.class
                && event.getBefore().stream().anyMatch(record -> current.recordId.equals(record.getId()))) {
                event.first(Mono.defer(() -> {
                    current.beforeNames.add(event.getBefore().get(0).getName());
                    if (current.entries.incrementAndGet() == 1) {
                        current.firstReached.tryEmitEmpty();
                        return current.releaseFirst.asMono();
                    }
                    return Mono.empty();
                }));
            }
        }

        @EventListener
        public void saved(EntitySavedEvent<?> event) {
            if (event.getEntityType() == type) {
                event.async(Mono.error(() -> new IllegalStateException("test transaction failure")));
            }
        }

        @EventListener
        public void modified(EntityModifyEvent<?> event) {
            if (event.getEntityType() == type) {
                event.async(Mono.error(() -> new IllegalStateException("test transaction failure")));
            }
        }

        @EventListener
        public void created(EntityCreatedEvent<?> event) {
            if (event.getEntityType() == type) {
                event.async(Mono.error(() -> new IllegalStateException("test transaction failure")));
            }
        }
    }

    private static final class ModifyGate {
        final String recordId;
        final Sinks.Empty<Void> firstReached = Sinks.empty();
        final Sinks.Empty<Void> releaseFirst = Sinks.empty();
        final Sinks.Empty<Void> secondStarted = Sinks.empty();
        final AtomicInteger entries = new AtomicInteger();
        final List<String> beforeNames = new ArrayList<>();

        ModifyGate(String recordId) {
            this.recordId = recordId;
        }
    }
}
