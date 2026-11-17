package org.jetlinks.pro.demo.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.jetlinks.pro.demo.service.DemoInspectionTeamService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo/inspection-team")
@Resource(id = DemoAssetType.TEAM_TYPE_ID, name = "巡检班组")
@AssetsController(type = DemoAssetType.TEAM_TYPE_ID)
@Tag(name = "巡检班组")
@AllArgsConstructor
@Getter
public class DemoInspectionTeamController implements AssetsHolderCrudController<DemoInspectionTeamEntity, String> {

    private final DemoInspectionTeamService service;
}
