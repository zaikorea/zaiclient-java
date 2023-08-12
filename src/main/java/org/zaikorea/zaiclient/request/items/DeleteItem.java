package org.zaikorea.zaiclient.request.items;

import java.util.Collections;
import java.util.List;

public class DeleteItem extends ItemRequest<List<String>> {

    public DeleteItem(String itemId) {
        super(Collections.singletonList(itemId));
    }

    public DeleteItem(List<String> itemIds) {
        super(itemIds);
    }
}
