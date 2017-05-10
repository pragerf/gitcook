package com.pacificmetrics.unity.scoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.pacificmetrics.unity.rest.QueryParameters;
import com.pacificmetrics.unity.storage.IStorageEngine;

public class AdaptiveManager {

    // List of Item Pool object identified by the Session UUID
    // Each Item Pool is a Map with a key of its Level code and the value is the list of Item UUIDs
    private Map<UUID, Map<String, List<UUID>>> itemPools;

    public AdaptiveManager() {
    	super();
    }

    public void initializeItemPoolForSession(UUID uuidOfTest, IStorageEngine store) {
        if (this.itemPools == null) {
            this.itemPools = new ConcurrentHashMap<>();
        }

        Map<String, List<UUID>> itemPoolForTestInstance = this.itemPools.get(uuidOfTest);
        if (itemPoolForTestInstance != null) {
            return; // already initialized, skip out
        }

        // first time, query for lists of items by level
        itemPoolForTestInstance = new ConcurrentHashMap<>();

        for (int levelN = 1; levelN <= 7; levelN++) {
            // query for items where level = N
            List<Map<String, Object>> itemsFound =
                store.query("item", "level:\"" + levelN + "\"", "uuid,name,level", null, QueryParameters.getDefault());
           

            List<UUID> itemsForLevelN = new ArrayList<>();
            // skip first record of results
            for (int i = 1; i < itemsFound.size(); i++) {
                Map<String, Object> itemFound = itemsFound.get(i);
                Object uuidObj = itemFound.get("uuid");
                itemsForLevelN.add(UUID.fromString((String) uuidObj));
            }

            // randomize the order of the item uuids in each level
            Collections.shuffle(itemsForLevelN);

            // save shuffled list in pool
            itemPoolForTestInstance.put(Integer.toString(levelN), itemsForLevelN);
        }

        this.itemPools.put(uuidOfTest, itemPoolForTestInstance);
    }

    public UUID pullItemFromPool(UUID uuidOfTest, String level) {
        Map<String, List<UUID>> itemPoolForTestInstance = this.itemPools.get(uuidOfTest);
        List<UUID> itemListOfOneLevel = itemPoolForTestInstance.get(level);
        int lastListIndex = itemListOfOneLevel.size() - 1;
        UUID nextItemUuid = itemListOfOneLevel.get(lastListIndex);

        // remove from item list for the given level for the current test
        itemListOfOneLevel.remove(lastListIndex);
        return nextItemUuid;
    }
}
