package org.jetlinks.pro.crud.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.BlockingAssetsHolderCrudController;
import org.jetlinks.pro.crud.entity.ExampleEntity;
import org.jetlinks.pro.crud.service.ExampleService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/example/crud")
@AllArgsConstructor
@Getter
@Resource(id = "example", name = "增删改查演示")//资源定义,用于权限控制
@Tag(name = "增删改查演示") //swagger
@AssetsController(type = "device")
public class ExampleController implements BlockingAssetsHolderCrudController<ExampleEntity, String> {

    private final ExampleService service;


}
