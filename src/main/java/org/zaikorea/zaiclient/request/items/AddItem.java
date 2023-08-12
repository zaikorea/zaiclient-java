package org.zaikorea.zaiclient.request.items;

import java.util.Collections;
import java.util.List;

public class AddItem extends ItemRequest<List<Item>> {

    public AddItem(Item item) {
        super(Collections.singletonList(item));

        validate(item);
    }

    public AddItem(List<Item> items) {
        super(items);

        validate(items);
    }

    private void validate(Item item) throws IllegalArgumentException {
        if (item.getItemName() == null) {
            throw new IllegalArgumentException("Item name is required");
        }
    }

    private void validate(List<Item> items) throws IllegalArgumentException {
        for (Item item : items) {
            try {
                validate(item);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    String.format("Index %d of items throws error: %s", items.indexOf(item), e.getMessage())
                );
            }
        }
    }
}
