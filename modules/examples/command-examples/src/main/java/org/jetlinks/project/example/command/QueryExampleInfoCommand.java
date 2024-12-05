package org.jetlinks.project.example.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;

/**
 * 自定义命令
 *
 * @author gyl
 * @since 2.3
 */
@Schema(title = "查询示例信息", description = "可指定查询条件，拓展个数等，返回信息包含拓展列表")
public class QueryExampleInfoCommand extends QueryListCommand<ExampleExtendedInfo> {


    @Getter
    @Setter
    protected static class InputSpec extends QueryListCommand.InputSpec {

        @Schema(title = "拓展个数", description = "仅查询拓展个数等于预期值的示例实体数据")
        public int extendedSize;

    }
}
