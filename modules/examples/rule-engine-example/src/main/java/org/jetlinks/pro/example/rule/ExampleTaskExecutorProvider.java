package org.jetlinks.pro.example.rule;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.pro.rule.engine.editor.annotation.EditorResource;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
//node-red 可视化规则编排设计器需要的资源文件
@EditorResource(
    id = "example",
    name = "Example",
    editor = "rule-engine/editor/example.html",
    helper = "rule-engine/i18n/zh/example.html",
    order = 500
)
public class ExampleTaskExecutorProvider implements TaskExecutorProvider {

    @Override
    public String getExecutor() {
        return "example";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new ExampleTaskExecutor(context));
    }

    @Getter
    @Setter
    public static class Properties {
        private String text;
    }

    static class ExampleTaskExecutor extends FunctionTaskExecutor {

        private Properties properties;

        public ExampleTaskExecutor(ExecutionContext context) {
            super("Example", context);
            reload();
        }

        @Override
        public void reload() {
            super.reload();
            this.properties = FastBeanCopier.copy(
                context.getJob().getConfiguration(),
                new Properties()
            );
        }

        //上游节点输入时此方法将会被调用并返回新的数据给下游节点
        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            //执行后返回数据给下游节点
            Map<String, Object> data = new HashMap<>();
            data.put("text", properties.getText());

            return Mono.just(
                context.newRuleData(input.newData(data))
            );
        }
    }
}
