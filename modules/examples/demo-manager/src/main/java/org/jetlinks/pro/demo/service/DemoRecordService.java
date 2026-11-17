package org.jetlinks.pro.demo.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * 示例记录的响应式持久化服务，直接复用平台 CRUD 和实体事件。
 * 功能与资产权限由对外控制器统一校验。
 */
@Service
public class DemoRecordService extends DemoTransactionalCrudService<DemoRecordEntity> {

    public DemoRecordService(ReactiveRepository<DemoRecordEntity, String> repository,
                             TransactionalOperator transactionalOperator) {
        super(repository, transactionalOperator);
    }
}
