package org.jetlinks.pro.example.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name = "example_crud_ext")
@Getter
@Setter
@Schema(description = "演示拓展表")
@EnableEntityEvent
public class ExtendedEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false,updatable = false)
    private String exampleId;

    @Column
    private String extName;


}
