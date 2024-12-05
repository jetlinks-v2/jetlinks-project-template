package org.jetlinks.project.example.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import org.jetlinks.sdk.server.commons.cmd.metadata.QueryParamSpec;

/**
 * 自定义命令
 *
 * @author gyl
 * @since 2.3
 */
public class QueryExampleInfoCommand extends QueryListCommand<ExampleExtendedInfo> {


    public InputSpec getInputSpec() {
        return FastBeanCopier.copy(readable(), new InputSpec());
    }


    @Getter
    @Setter
    protected static class InputSpec extends QueryParamSpec {

        @Schema(title = "拓展个数",
            description = "仅查询拓展个数等于预期值的示例实体数据")
        public int extendedSize;


        public QueryParamEntity toQueryParam() {
            return FastBeanCopier.copy(this, new QueryParamEntity());
        }

    }
}
