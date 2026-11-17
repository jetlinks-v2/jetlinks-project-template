package org.jetlinks.pro.demo.limiter;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.jetlinks.pro.assets.AssetType;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.tenant.MetricId;
import org.jetlinks.pro.tenant.holder.TenantHolder;
import org.jetlinks.pro.tenant.impl.limiter.EntityEventQuotaLimiter;
import org.jetlinks.pro.tenant.resource.AssetResourceMetric;
import org.jetlinks.pro.tenant.resource.ResourceLimitType;
import org.jetlinks.pro.tenant.resource.ResourceType;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 按租户保有的真实巡检清单计量，新增、修改和删除均沿实体事件进入原业务事务。
 * 只声明指标及差额算法，额度由 SaaS 规格下发，不在应用内设置默认额度。
 */
public class DemoInspectionItemsQuotaLimiter
    extends EntityEventQuotaLimiter<DemoRecordEntity, String>
    implements AssetResourceMetric {

    public static final MetricId METRIC_ID =
        MetricId.of(DemoAssetType.record.getId(), "inspectionItems");

    public DemoInspectionItemsQuotaLimiter(ReactiveRepository<DemoRecordEntity, String> repository,
                                          TransactionalOperator transactionalOperator) {
        super(METRIC_ID, repository, transactionalOperator);
    }

    @Override
    protected double getUsage(DemoRecordEntity entity) {
        return entity.getInspectionItems().size();
    }

    @Override
    protected Mono<Double> countById(Collection<Object> ids) {
        // 与业务写入保持相同的记录锁顺序，再由公共限制器取得租户额度行锁。
        return dataRepository.createQuery()
            .in(DemoRecordEntity::getId, ids)
            .orderBy(SortOrder.asc("id"))
            .forUpdate()
            .fetch()
            .reduce(0D, (total, record) -> total + getUsage(record));
    }

    @Override
    public Mono<Void> handleBeforeSave(String tenantId, List<DemoRecordEntity> entities) {
        if (tenantId == null) {
            return Mono.empty();
        }
        Set<Object> existingIds = new LinkedHashSet<>();
        double total = 0;
        for (DemoRecordEntity entity : entities) {
            total += getUsage(entity);
            if (entity.getId() != null) {
                existingIds.add(entity.getId());
            }
        }
        double after = total;
        Mono<Double> previous = existingIds.isEmpty() ? Mono.just(0D) : countById(existingIds);
        return previous.flatMap(before -> applyDifference(tenantId, after - before));
    }

    private Mono<Void> applyDifference(String tenantId, double difference) {
        if (difference == 0) {
            return Mono.empty();
        }
        // 公共数据库限制器忽略非正 increment；Save 缩减清单时明确归还负差额。
        return TenantHolder.getReactive(tenantId)
            .switchIfEmpty(tenantNotExists(tenantId))
            .flatMap(tenant -> tenant.limiter(METRIC_ID))
            .flatMap(limiter -> difference > 0
                ? limiter.forQuota().increment(difference)
                : limiter.forQuota().decrement(-difference));
    }

    @Override
    public AssetType getAssetType() {
        return DemoAssetType.record;
    }

    @Override
    public String getId() {
        return METRIC_ID.getMetric();
    }

    @Override
    public String getName() {
        return "巡检项保有量";
    }

    @Override
    public String getDescription() {
        return "当前租户所有保有巡检计划的清单项总数，删除计划或缩减清单后释放额度";
    }

    @Override
    public String getUnit() {
        return "项";
    }

    @Override
    public ResourceType getType() {
        return ResourceType.entity;
    }

    @Override
    public ResourceLimitType getLimitType() {
        return ResourceLimitType.quotaLimit;
    }
}
