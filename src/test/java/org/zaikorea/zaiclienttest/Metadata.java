package org.zaikorea.zaiclienttest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.google.gson.annotations.SerializedName;

public class Metadata {

        @SerializedName("user_id")
        public String userId;

        @SerializedName("item_id")
        public String itemId;

        @SerializedName("item_ids")
        public List<String> itemIds;

        @SerializedName("limit")
        public Integer limit;

        @SerializedName("offset")
        public Integer offset;

        @SerializedName("options")
        public Map<String, Integer> options;

        @SerializedName("call_type")
        public String callType;

        @SerializedName("recommendation_type")
        public String recommendationType;

        public Metadata() {
            this.offset = 0;
            this.options = new HashMap<>();
        }

        @Override
        public boolean equals(Object obj) {

            try {
                for (Field field: obj.getClass().getFields()) {
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

