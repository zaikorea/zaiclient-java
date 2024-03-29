package org.zaikorea.zaiclient.request.items;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.zaikorea.zaiclient.utils.Validator;

import com.google.gson.annotations.SerializedName;


public class Item {
    @SerializedName("item_id")
    protected String itemId;

    @SerializedName("item_name")
    protected String itemName = null;

    @SerializedName("category_id_1")
    protected String categoryId1 = null;

    @SerializedName("category_name_1")
    protected String categoryName1;

    @SerializedName("category_id_2")
    protected String categoryId2;

    @SerializedName("category_name_2")
    protected String categoryName2;

    @SerializedName("category_id_3")
    protected String categoryId3;

    @SerializedName("category_name_3")
    protected String categoryName3;

    @SerializedName("brand_id")
    protected String brandId;

    @SerializedName("brand_name")
    protected String brandName;

    @SerializedName("description")
    protected String description;

    @SerializedName("created_timestamp")
    protected String createdTimestamp;

    @SerializedName("updated_timestamp")
    protected String updatedTimestamp;

    @SerializedName("is_active")
    protected boolean isActive = true;

    @SerializedName("is_soldout")
    protected boolean isSoldout = false;

    @SerializedName("promote_on")
    protected String promoteOn;

    @SerializedName("item_group")
    protected String itemGroup;

    @SerializedName("rating")
    protected Float rating;

    @SerializedName("price")
    protected Float price;

    @SerializedName("click_counts")
    protected Integer clickCounts;

    @SerializedName("purchase_counts")
    protected Integer purchaseCounts;

    @SerializedName("image_url")
    protected String imageUrl;

    @SerializedName("item_url")
    protected String itemUrl;

    @SerializedName("miscellaneous")
    protected String miscellaneous;

    public Item(String itemId) {
        this.setItemId(itemId);
    }

    public Item(String itemId, String itemName) {
        this.setItemId(itemId)
            .setItemName(itemName);
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategoryId1() {
        return categoryId1;
    }

    public String getCategoryName1() {
        return categoryName1;
    }

    public String getCategoryId2() {
        return categoryId2;
    }

    public String getCategoryName2() {
        return categoryName2;
    }

    public String getCategoryId3() {
        return categoryId3;
    }

    public String getCategoryName3() {
        return categoryName3;
    }

    public String getBrandId() {
        return brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedTimestamp() {
        return createdTimestamp;
    }

    public String getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public boolean getIsSoldout() {
        return isSoldout;
    }

    public String getPromoteOn() {
        return promoteOn;
    }

    public String getItemGroup() {
        return itemGroup;
    }

    public Float getRating() {
        return rating;
    }

    public Float getPrice() {
        return price;
    }

    public Integer getClickCounts() {
        return clickCounts;
    }

    public Integer getPurchaseCounts() {
        return purchaseCounts;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getItemUrl() {
        return itemUrl;
    }

    public String getMiscellaneous() {
        return miscellaneous;
    }

    public Item setItemId(String itemId) {
        this.itemId = Validator.validateString(itemId, 1, 500, false, "itemId");

        return this;
    }

    public Item setItemName(String itemName) {
        this.itemName = Validator.validateString(itemName, 1, 500, true, "itemName");

        return this;
    }

    public Item setCategoryId1(String categoryId1) {
        this.categoryId1 = Validator.validateString(categoryId1, 1, 500, true, "categoryId1");

        return this;
    }

    public Item setCategoryName1(String categoryName1) {
        this.categoryName1 = Validator.validateString(categoryName1, 1, 500, true, "categoryName1");

        return this;
    }

    public Item setCategoryId2(String categoryId2) {
        this.categoryId2 = Validator.validateString(categoryId2, 1, 500, true, "categoryId2");

        return this;
    }

    public Item setCategoryName2(String categoryName2) {
        this.categoryName2 = Validator.validateString(categoryName2, 1, 500, true, "categoryName2");

        return this;
    }

    public Item setCategoryId3(String categoryId3) {
        this.categoryId3 = Validator.validateString(categoryId3, 1, 500, true, "categoryId3");

        return this;
    }

    public Item setCategoryName3(String categoryName3) {
        this.categoryName3 = Validator.validateString(categoryName3, 1, 500, true, "categoryName3");

        return this;
    }

    public Item setBrandId(String brandId) {
        this.brandId = Validator.validateString(brandId, 1, 500, true, "brandId");

        return this;
    }

    public Item setBrandName(String brandName) {
        this.brandName = Validator.validateString(brandName, 1, 500, true, "brandName");

        return this;
    }

    public Item setDescription(String description) {
        this.description = Validator.validateString(description, 1, 500, true, "description");

        return this;
    }

    public Item setCreatedTimestamp(String createdTimestamp) {
        this.createdTimestamp = Validator.validateISODateTime(createdTimestamp, "createdTimestamp");

        return this;
    }

    public Item setCreatedTimestamp(Long createdTimestamp) {
        // BackLog: The upper bound for timestamp is Tue, 19 Jan 2038 04:14:07 +0100.
        //          This is the maximum value for a 32-bit signed integer in seconds since Unix epoch.
        //          Should use long instead... might need change in all other places as well.
        //          Also make sure that the other SDKs are using UTC timestamp when sending time to API server.
        Validator.validateNumber(createdTimestamp, 1_648_871_097_000L, true, "createdTimestamp");

        Instant time = Instant.ofEpochMilli(createdTimestamp);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        this.createdTimestamp = formatter.format(time);

        return this;
    }

    public Item setUpdatedTimestamp(String updatedTimestamp) {
        this.updatedTimestamp = Validator.validateISODateTime(updatedTimestamp, "updatedTimestamp");

        return this;
    }

    public Item setUpdatedTimestamp(Long updatedTimestamp) {
        Validator.validateNumber(updatedTimestamp, 1_648_871_097_000L, true, "updatedTimestamp");

        Instant time = Instant.ofEpochMilli(updatedTimestamp);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        this.updatedTimestamp = formatter.format(time);

        return this;
    }

    public Item setIsActive(boolean isActive) {
        this.isActive = isActive;

        return this;
    }

    public Item setIsSoldout(boolean isSoldout) {
        this.isSoldout = isSoldout;

        return this;
    }

    public Item setPromoteOn(String promoteOn) {
        this.promoteOn = Validator.validateString(promoteOn, 1, 500, true, "promoteOn");

        return this;
    }

    public Item setItemGroup(String itemGroup) {
        this.itemGroup = Validator.validateString(itemGroup, 1, 500, true, "itemGroup");

        return this;
    }

    public Item setRating(Float rating) {
        this.rating = rating;

        return this;
    }

    public Item setPrice(Float price) {
        this.price = price;

        return this;
    }

    public Item setClickCounts(Integer clickCounts) {
        this.clickCounts = clickCounts;

        return this;
    }

    public Item setPurchaseCounts(Integer purchaseCounts) {
        this.purchaseCounts = purchaseCounts;

        return this;
    }

    public Item setImageUrl(String imageUrl) {
        this.imageUrl = Validator.validateString(imageUrl, 1, 500, true, "imageUrl");

        return this;
    }

    public Item setItemUrl(String itemUrl) {
        this.itemUrl = Validator.validateString(itemUrl, 1, 500, true, "itemUrl");

        return this;
    }

    public Item setMiscellaneous(String miscellaneous) {
        this.miscellaneous = Validator.validateString(miscellaneous, 1, 500, true, "miscellaneous");

        return this;
    }
}

