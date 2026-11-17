package org.jetlinks.pro.demo.event;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.crud.events.EntityBeforeCreateEvent;
import org.hswebframework.web.crud.events.EntityBeforeModifyEvent;
import org.hswebframework.web.crud.events.EntityBeforeSaveEvent;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.pro.assets.AssetsHolder;
import org.jetlinks.pro.assets.crud.CrudAssetPermission;
import org.jetlinks.pro.assets.dynamic.entity.DynamicDimensionEntity;
import org.jetlinks.pro.assets.dynamic.service.DynamicDimensionService;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
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
 * 正式实体事件同时覆盖业务 CRUD 和平台 Direct 字段绑定，避免另设绑定校验链。
 */
@Component
@AllArgsConstructor
public class DemoRecordLifecycleHandler {

    private final ReactiveRepository<DemoRecordEntity, String> records;
    private final ReactiveRepository<DemoInspectionTeamEntity, String> teams;
    private final DynamicDimensionService dimensions;

    @EventListener
    public void beforeCreate(EntityBeforeCreateEvent<DemoRecordEntity> event) {
        if (event.getEntityType() == DemoRecordEntity.class) {
            event.async(validate(event.getEntity(), Map.of()));
        }
    }

    @EventListener
    public void beforeSave(EntityBeforeSaveEvent<DemoRecordEntity> event) {
        if (event.getEntityType() == DemoRecordEntity.class) {
            List<String> ids = event.getEntity().stream()
                .map(DemoRecordEntity::getId).filter(Objects::nonNull).toList();
            event.async((ids.isEmpty() ? Mono.just(Map.<String, DemoRecordEntity>of())
                : records.findById(ids).collectMap(DemoRecordEntity::getId))
                .flatMap(before -> validate(event.getEntity(), before)));
        }
    }

    @EventListener
    public void beforeModify(EntityBeforeModifyEvent<DemoRecordEntity> event) {
        if (event.getEntityType() == DemoRecordEntity.class) {
            Map<String, DemoRecordEntity> before = event.getBefore().stream()
                .collect(Collectors.toMap(DemoRecordEntity::getId, Function.identity()));
            event.async(validate(event.getAfter(), before));
        }
    }

    private Mono<Void> validate(Collection<DemoRecordEntity> records,
                                Map<String, DemoRecordEntity> previous) {
        return Mono.defer(() -> {
            Set<String> changedTeams = new LinkedHashSet<>();
            for (DemoRecordEntity record : records) {
                ValidatorUtils.tryValidate(record);
                DemoRecordEntity before = previous.get(record.getId());
                // 归属未变时，计划已有权限继续支持改名、调整清单，不重复要求班组管理权。
                if (before == null || !Objects.equals(before.getInspectionTeamId(), record.getInspectionTeamId())) {
                    changedTeams.add(record.getInspectionTeamId());
                }
            }
            if (changedTeams.isEmpty()) {
                return Mono.empty();
            }
            // 持有目标班组锁直到计划写入完成，与班组删除的引用检查串行。
            return teams.createQuery().in(DemoInspectionTeamEntity::getId, changedTeams)
                .orderBy(SortOrder.asc("id")).forUpdate().fetch().count()
                .flatMap(count -> count == changedTeams.size() ? Mono.empty()
                    : Mono.error(() -> new BusinessException("error.demo.inspection_team.invalid")))
                .then(dimensions.queryEnabled(DemoAssetType.TEAM_TYPE_ID, changedTeams)
                    .map(DynamicDimensionEntity::getDimensionId).distinct().count())
                .flatMap(count -> count == changedTeams.size() ? Mono.empty()
                    : Mono.error(() -> new BusinessException("error.demo.inspection_team.invalid")))
                .then(AssetsHolder.assertPermission(DemoAssetType.inspectionTeam, changedTeams, CrudAssetPermission.save));
        });
    }
}
