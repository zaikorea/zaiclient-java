package org.zaikorea.zaiclient.request.items;

import java.util.Collections;
import java.util.List;

public class UpdateItem extends ItemRequest<List<Item>>{
    public UpdateItem(Item item) {
        super(Collections.singletonList(item));
    }

    public UpdateItem(List<Item> items) {
        super(items);
    }
}
