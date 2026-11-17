package org.jetlinks.pro.demo.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

@Service
public class DemoInspectionTeamService extends DemoTransactionalCrudService<DemoInspectionTeamEntity> {

    public DemoInspectionTeamService(ReactiveRepository<DemoInspectionTeamEntity, String> repository,
                                     TransactionalOperator transactionalOperator) {
        super(repository, transactionalOperator);
    }
}
