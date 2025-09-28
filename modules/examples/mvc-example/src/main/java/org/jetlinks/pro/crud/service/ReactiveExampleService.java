package org.jetlinks.pro.crud.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.pro.crud.entity.ExampleEntity;
import org.springframework.stereotype.Service;

/**
 * 在springmvc仍然可以使用响应式
 */
@Service
public class ReactiveExampleService extends GenericReactiveCrudService<ExampleEntity, String> {



}
