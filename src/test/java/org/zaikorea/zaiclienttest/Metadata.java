package org.zaikorea.zaiclienttest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Metadata {

    @JsonAlias("user_id")
    public String userId;

    @JsonAlias("item_id")
    public String itemId;

    @JsonAlias("item_ids")
    public List<String> itemIds;

    @JsonAlias("limit")
    public Integer limit;

    @JsonAlias("offset")
    public Integer offset;

    @JsonAlias("options")
    public Map<String, ?> options;

    @JsonAlias("call_type")
    public String callType;

    @JsonAlias("recommendation_type")
    public String recommendationType;

    public Metadata() {
        this.offset = 0;
        this.options = new HashMap<>();
    }

    @Override
    public boolean equals(Object obj) {

        try {
            for (Field field : obj.getClass().getFields()) {
                if (field.get(obj) != null && field.get(obj).equals(field.get(this)))
                    continue;
                else {
                    if (field.get(obj) == null && field.get(this) == null)
                        continue;
                    else
                        return false;
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            System.out.println(e.getMessage());

            return false;
        }

        return true;
    }
}
