package org.jetlinks.project.example.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
public enum ExampleEnum implements I18nEnumDict<String> {

    enum1("枚举1"),
    enum2("枚举2"),
    enum3("枚举3");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

}
