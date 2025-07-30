package org.jetlinks.pro.example.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.io.excel.ExcelUtils;
import org.jetlinks.pro.io.excel.ImportHelper;
import org.jetlinks.pro.io.file.FileInfo;
import org.jetlinks.pro.io.file.FileManager;
import org.jetlinks.pro.io.file.FileOption;
import org.jetlinks.pro.io.utils.FileUtils;
import org.jetlinks.pro.example.asset.ExampleAssetType;
import org.jetlinks.pro.example.entity.ExampleEntity;
import org.jetlinks.pro.example.entity.ExtendedEntity;
import org.jetlinks.pro.example.enums.ExampleEnum;
import org.jetlinks.pro.example.service.ExampleService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/example/crud")
@AllArgsConstructor
@Getter
@Resource(id = "example", name = "增删改查演示")//资源定义,用于权限控制
@Tag(name = "增删改查演示") //swagger
@AssetsController(type = ExampleAssetType.TYPE_ID)
public class ExampleController implements AssetsHolderCrudController<ExampleEntity, String> {

    private final ExampleService service;
    private final ReactiveRepository<ExtendedEntity, String> extRepository;

    private final QueryHelper queryHelper;

    private final FileManager fileManager;

    @PatchMapping("/_ext")
    public Mono<Void> addExtended(@RequestBody Flux<ExtendedEntity> extendedEntityFlux) {
        return extRepository
            .save(extendedEntityFlux)
            .then();
    }

    @GetMapping("/_detail/_query")
    public Mono<PagerResult<ExampleInfo>> joinExample(QueryParamEntity query) {
        return queryHelper
            .select(ExampleInfo.class)
            .all(ExampleEntity.class)
            .all(ExtendedEntity.class, ExampleInfo::setExt)
            .from(ExampleEntity.class)
            .leftJoin(ExtendedEntity.class, spec -> spec.is(ExtendedEntity::getExampleId, ExampleEntity::getId))
            //根据前端的动态条件参数自动构造查询条件以及分页排序等信息
            .where(query)
            .fetchPaged();
    }

    /**
     * OneToMany的场景
     */
    @GetMapping("/_detail_many/_query")
    public Mono<PagerResult<ExampleInfo>> joinManyExample(QueryParamEntity query) {
        return queryHelper
            .select(ExampleInfo.class)
            .all(ExampleEntity.class)
            //属性类型为list自动处理为OneToMany
            .all(ExtendedEntity.class, ExampleInfo::setExtList)
            .from(ExampleEntity.class)
            .leftJoin(ExtendedEntity.class, spec -> spec.is(ExtendedEntity::getExampleId, ExampleEntity::getId))
            //1对多只能查询主表条件
            .where(query)
            .fetchPaged();
    }

    /**
     * 程序逻辑处理OneToMany的场景
     */
    @GetMapping("/_detail_logic/_query")
    public Mono<PagerResult<ExampleInfo>> joinLogicExample(QueryParamEntity query) {
        return QueryHelper
            .transformPageResult(
                //分页查询原始数据
                service.queryPager(query),
                list -> QueryHelper
                    .combineOneToMany(
                        //原始数据转为 ExampleInfo
                        Flux.fromIterable(list).map(e -> e.copyTo(new ExampleInfo())),
                        //主表数据ID
                        ExampleInfo::getId,
                        //查询关联表
                        extRepository.createQuery(),
                        //关联表中的主表ID属性
                        ExtendedEntity::getExampleId,
                        //关联数据设置到主表中
                        ExampleInfo::setExtList
                    )
                    .collectList());
    }

    @GetMapping("/_detail/_query_native")
    public Mono<PagerResult<ExampleInfo>> nativeJoinExample(QueryParamEntity query) {
        return queryHelper
            .select("select t.*,ext.* from example_crud t" +
                        " left join example_crud_ext ext on ext.example_id = t.id",
                    ExampleInfo::new)
            //根据前端的动态条件参数自动构造查询条件以及分页排序等信息
            .where(query)
            .fetchPaged();
    }


    //根据上传的文件来导入数据并将导入结果保存到文件中返回结果文件地址
    @PostMapping("/_import.{format}")
    public Mono<String> importByFileUpload(@PathVariable String format,
                                           @RequestPart("file") Mono<FilePart> file) {


        return FileUtils
            .dataBufferToInputStream(file.flatMapMany(FilePart::content))
            .flatMap(inputstream -> new ImportHelper<>(
                ExampleEntity::new,
                //数据处理逻辑
                flux -> service.save(flux).then())
                //批量处理数量
                .bufferSize(200)
                //当批量处理失败时,是否回退到单条数据处理
                .fallbackSingle(true)
                .doImport(inputstream,
                          format,
                          info -> null,
                          //将导入的结果保存为临时文件
                          result -> fileManager
                              .saveFile("import." + format, result, FileOption.tempFile)
                              .map(FileInfo::getAccessUrl))
                .last()
            );
    }

    //导出数据
    @GetMapping("/_export/{name}.{format}")
    public Mono<Void> export(QueryParamEntity param,
                             @PathVariable String name,
                             //文件格式: 支持csv,xlsx
                             @PathVariable String format,
                             ServerWebExchange exchange) {

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
        //文件名
        exchange.getResponse().getHeaders().setContentDisposition(
            ContentDisposition
                .attachment()
                .filename(name + "." + format, StandardCharsets.UTF_8)
                .build()
        );
        return exchange
            .getResponse()
            .writeWith(
                ExcelUtils.write(ExampleEntity.class, service.query(param.noPaging()), format)
            );
    }


    @Getter
    @Setter
    public static class ExampleInfo {
        private String id;

        private String name;

        private ExampleEnum singleEnum;

        private ExampleEnum[] multiEnum;

        private ExtendedEntity ext;

        private List<ExtendedEntity> extList;

        public void setExtList(List<ExtendedEntity> extList) {
            this.extList = extList;
        }
    }
}
