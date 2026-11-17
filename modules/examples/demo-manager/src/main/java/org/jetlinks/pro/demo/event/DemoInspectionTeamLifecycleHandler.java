package org.jetlinks.pro.demo.event;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.crud.events.EntityBeforeCreateEvent;
import org.hswebframework.web.crud.events.EntityBeforeDeleteEvent;
import org.hswebframework.web.crud.events.EntityBeforeModifyEvent;
import org.hswebframework.web.crud.events.EntityBeforeSaveEvent;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.pro.assets.AssetsHolder;
import org.jetlinks.pro.assets.AssetsUtils;
import org.jetlinks.pro.assets.crud.CrudAssetPermission;
import org.jetlinks.pro.assets.dynamic.entity.DynamicDimensionEntity;
import org.jetlinks.pro.assets.dynamic.service.DynamicDimensionService;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 班组业务表是事实来源，公共实例与树闭包由正式生命周期在原事务内同步。
 */
@Component
@AllArgsConstructor
public class DemoInspectionTeamLifecycleHandler {

    private final ReactiveRepository<DemoInspectionTeamEntity, String> teams;
    private final ReactiveRepository<DemoRecordEntity, String> records;
    private final DynamicDimensionService dimensions;

    @EventListener
    public void beforeCreate(EntityBeforeCreateEvent<DemoInspectionTeamEntity> event) {
        if (event.getEntityType() == DemoInspectionTeamEntity.class) {
            event.async(validate(event.getEntity(), Map.of()));
        }
    }

    @EventListener
    public void beforeSave(EntityBeforeSaveEvent<DemoInspectionTeamEntity> event) {
        if (event.getEntityType() == DemoInspectionTeamEntity.class) {
            List<String> ids = event.getEntity().stream()
                .map(DemoInspectionTeamEntity::getId).filter(Objects::nonNull).toList();
            event.async((ids.isEmpty() ? Mono.just(Map.<String, DemoInspectionTeamEntity>of())
                : teams.findById(ids).collectMap(DemoInspectionTeamEntity::getId))
                .flatMap(before -> validate(event.getEntity(), before)));
        }
    }

    @EventListener
    public void beforeModify(EntityBeforeModifyEvent<DemoInspectionTeamEntity> event) {
        if (event.getEntityType() == DemoInspectionTeamEntity.class) {
            Map<String, DemoInspectionTeamEntity> before = event.getBefore().stream()
                .collect(Collectors.toMap(DemoInspectionTeamEntity::getId, Function.identity()));
            event.async(validate(event.getAfter(), before));
        }
    }

    @EventListener
    public void beforeDelete(EntityBeforeDeleteEvent<DemoInspectionTeamEntity> event) {
        if (event.getEntityType() == DemoInspectionTeamEntity.class) {
            List<String> ids = event.getEntity().stream().map(DemoInspectionTeamEntity::getId).toList();
            event.async(teams.createQuery().in(DemoInspectionTeamEntity::getParentId, ids).count()
                .flatMap(count -> count == 0 ? Mono.empty()
                    : Mono.error(() -> new BusinessException("error.demo.inspection_team.children_exist")))
                .then(records.createQuery().in(DemoRecordEntity::getInspectionTeamId, ids).count())
                .flatMap(count -> count == 0 ? Mono.empty()
                    : Mono.error(() -> new BusinessException("error.demo.inspection_team.records_exist"))));
        }
    }

    @EventListener
    public void afterCreate(EntityCreatedEvent<DemoInspectionTeamEntity> event) {
        if (event.getEntityType() == DemoInspectionTeamEntity.class) {
            event.async(synchronize(event.getEntity()));
        }
    }

    @EventListener
    public void afterSave(EntitySavedEvent<DemoInspectionTeamEntity> event) {
        if (event.getEntityType() == DemoInspectionTeamEntity.class) {
            event.async(synchronize(event.getEntity()));
        }
    }

    @EventListener
    public void afterModify(EntityModifyEvent<DemoInspectionTeamEntity> event) {
        if (event.getEntityType() == DemoInspectionTeamEntity.class) {
            event.async(synchronize(event.getAfter()));
        }
    }

    @EventListener
    public void afterDelete(EntityDeletedEvent<DemoInspectionTeamEntity> event) {
        if (event.getEntityType() == DemoInspectionTeamEntity.class) {
            event.async(dimensions.deleteById(Flux.fromIterable(event.getEntity())
                .map(team -> AssetsUtils.createBindingTargetKey(DemoAssetType.TEAM_TYPE_ID, team.getId()))).then());
        }
    }

    private Mono<Void> validate(Collection<DemoInspectionTeamEntity> input,
                                Map<String, DemoInspectionTeamEntity> previous) {
        return Mono.defer(() -> {
            Set<String> changedParents = new LinkedHashSet<>();
            for (DemoInspectionTeamEntity team : input) {
                ValidatorUtils.tryValidate(team);
                DemoInspectionTeamEntity before = previous.get(team.getId());
                if (StringUtils.isNotBlank(team.getParentId())
                    && (before == null || !Objects.equals(before.getParentId(), team.getParentId()))) {
                    changedParents.add(team.getParentId());
                }
            }
            if (changedParents.isEmpty()) {
                return Mono.empty();
            }
            return teams.createQuery().in(DemoInspectionTeamEntity::getId, changedParents)
                .orderBy(SortOrder.asc("id")).forUpdate().fetch().count()
                .flatMap(count -> count == changedParents.size() ? Mono.empty()
                    : Mono.error(() -> new BusinessException("error.demo.inspection_team.parent_invalid")))
                .then(dimensions.queryEnabled(DemoAssetType.TEAM_TYPE_ID, changedParents).count())
                .flatMap(count -> count == changedParents.size() ? Mono.empty()
                    : Mono.error(() -> new BusinessException("error.demo.inspection_team.parent_invalid")))
                .then(AssetsHolder.assertPermission(DemoAssetType.inspectionTeam, changedParents, CrudAssetPermission.save));
        });
    }

    private Mono<Void> synchronize(Collection<DemoInspectionTeamEntity> input) {
        List<String> ids = input.stream().map(DemoInspectionTeamEntity::getId).distinct().toList();
        // 重读实际持久值，避免部分字段更新或数据库默认值造成权限投影丢失。
        return teams.findById(ids).map(team -> {
            DynamicDimensionEntity dimension = new DynamicDimensionEntity();
            dimension.setTypeId(DemoAssetType.TEAM_TYPE_ID);
            dimension.setDimensionId(team.getId());
            dimension.setName(team.getName());
            dimension.setState(team.getState());
            dimension.setParentId(StringUtils.isBlank(team.getParentId()) ? ""
                : AssetsUtils.createBindingTargetKey(DemoAssetType.TEAM_TYPE_ID, team.getParentId()));
            return dimension;
        }).collectList().flatMap(dimensions::save).then();
    }
}
