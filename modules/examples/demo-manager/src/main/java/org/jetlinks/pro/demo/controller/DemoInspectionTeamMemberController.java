package org.jetlinks.pro.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.controller.response.DemoInspectionTeamMember;
import org.jetlinks.pro.demo.service.DemoInspectionTeamMemberService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/** 具备成员管理权限的应用用户在当前应用内维护班组成员。 */
@RestController
@RequestMapping("/demo/inspection-team/{teamId}/members")
@Authorize
// 成员查询的 QueryParam 属于远端用户，不能注入班组条件；Service 显式校验目标班组的功能、资产与归属权限。
@AssetsController(type = DemoAssetType.TEAM_TYPE_ID, ignore = true)
@Tag(name = "巡检班组成员")
public class DemoInspectionTeamMemberController {

    private final DemoInspectionTeamMemberService service;

    public DemoInspectionTeamMemberController(DemoInspectionTeamMemberService service) {
        this.service = service;
    }

    @PostMapping("/_query")
    @Operation(summary = "分页查询当前应用内已加入班组的成员")
    public Mono<PagerResult<DemoInspectionTeamMember>> query(@PathVariable String teamId,
                                                            @RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(param -> service.query(teamId, param, false));
    }

    @PostMapping("/_candidates/_query")
    @Operation(summary = "分页查询当前应用内可添加的班组成员")
    public Mono<PagerResult<DemoInspectionTeamMember>> candidates(@PathVariable String teamId,
                                                                 @RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(param -> service.query(teamId, param, true));
    }

    @PostMapping("/_bind")
    @Operation(summary = "将当前应用成员增量加入班组")
    public Mono<Void> bind(@PathVariable String teamId,
                           @RequestBody Mono<List<String>> userIds) {
        return userIds.flatMap(ids -> service.bind(teamId, ids));
    }

    @PostMapping("/_unbind")
    @Operation(summary = "移除指定班组成员")
    public Mono<Integer> unbind(@PathVariable String teamId,
                                @RequestBody Mono<List<String>> userIds) {
        return userIds.flatMap(ids -> service.unbind(teamId, ids));
    }
}
