package org.jetlinks.pro.crud.event;

import org.hswebframework.web.crud.events.EntityBeforeSaveEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.pro.crud.entity.ExampleEntity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Component
public class ExampleEventHandler {


    @EventListener
    public void handleEvent(EntityBeforeSaveEvent<ExampleEntity> event) {
        for (ExampleEntity exampleEntity : event.getEntity()) {
            System.out.println("保存前:" + exampleEntity);
        }
    }

    @EventListener
    public void handleEvent(EntitySavedEvent<ExampleEntity> event) {
        for (ExampleEntity exampleEntity : event.getEntity()) {
            System.out.println("保存后:" + exampleEntity);

        }
    }

    @EventListener
    public void handleEvent(EntityModifyEvent<ExampleEntity> event) {
        for (ExampleEntity exampleEntity : event.getAfter()) {
            System.out.println("修改:" + exampleEntity);

        }
    }
}
