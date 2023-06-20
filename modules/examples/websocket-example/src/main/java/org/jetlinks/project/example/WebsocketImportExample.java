package org.jetlinks.project.example;

import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.io.excel.AbstractImporter;
import org.jetlinks.pro.io.excel.ImportHelper;
import org.jetlinks.pro.io.file.FileManager;
import org.jetlinks.project.example.entity.ExampleEntity;
import org.jetlinks.project.example.service.ExampleService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * <pre>{@code
 * //同一个浏览器中应该只创建一次此websocket连接
 * //通过不同的订阅来获取不同的数据
 *  var ws = new WebSocket("ws://localhost:8080/messaging/{token}");
 *
 *  ws.onmessage= e=> console.log(e.data)
 *
 *  //发起订阅
 *  ws.send(JSON.stringify(
 *      {
 *           "id":"单个会话唯一ID",
 *           "topic":"/example-import",
 *           "parameter":{"fileUrl":"通过文件上传接口得到的文件地址"},
 *           "type":"sub"
 *      }
 * );
 *
 *
 *   //取消订阅，通常在切换页面时取消订阅,取消后会停止导入.
 *   ws.send(JSON.stringify(
 *     {
 *         id:"和请求的ID一致",
 *         "type":"unsub"
 *     }
 *   ))
 *
 *  }</pre>
 */
@Component
public class WebsocketImportExample extends AbstractImporter<ExampleEntity> implements SubscriptionProvider {

    private final ExampleService exampleService;

    public WebsocketImportExample(ExampleService service,
                                  FileManager fileManager,
                                  WebClient.Builder client) {
        super(fileManager, client.build());
        this.exampleService = service;
    }

    @Override
    public String id() {
        return "example-import";
    }

    @Override
    public String name() {
        return "导入数据";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/example-import"
        };
    }

    @Override
    public Flux<ImportResult<ExampleEntity>> subscribe(SubscribeRequest request) {
        String url = request
            .getString("fileUrl")
            .orElseThrow(() -> new IllegalArgumentException("fileUrl can not be null"));
        return doImport(url);
    }

    @Override
    protected void customImport(ImportHelper<ExampleEntity> helper) {
        super.customImport(helper);

        //批量操作数量
        helper.bufferSize(200);

        //当批量操作失败后回退到单个数据进行操作
        helper.fallbackSingle(true);

        //读取数据后校验数据
        helper.afterReadValidate(CreateGroup.class);
    }

    @Override
    protected Mono<Void> handleData(Flux<ExampleEntity> data) {
        //处理数据的逻辑
        return exampleService
            .save(data)
            .then();
    }

    @Override
    protected ExampleEntity newInstance() {
        return new ExampleEntity();
    }
}
