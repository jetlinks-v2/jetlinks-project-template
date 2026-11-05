package org.jetlinks.pro.demo.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.demo.service.DemoRecordService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例记录的统一 CRUD 入口，复用功能鉴权、资产范围注入及创建/删除自动绑定。
 * 请求中的应用作用域沿平台认证上下文传递，内部服务不重复校验同一权限。
 */
@RestController
@RequestMapping("/demo/record")
@Resource(id = DemoAssetType.TYPE_ID, name = "示例记录")
@AssetsController(type = DemoAssetType.TYPE_ID)
@Tag(name = "示例记录")
@AllArgsConstructor
@Getter
public class DemoRecordController implements AssetsHolderCrudController<DemoRecordEntity, String> {

    private final DemoRecordService service;
}
