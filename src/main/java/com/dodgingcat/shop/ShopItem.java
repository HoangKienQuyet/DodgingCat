package com.dodgingcat.shop;

/**
 * An item offered in the cosmetic shop.
 */
public class ShopItem {
    private final String id;
    private final String nameEn;
    private final String nameVi;
    private final String descEn;
    private final String descVi;
    private final ItemCategory category;
    private final SkinRarity rarity;
    private final int price;

    public ShopItem(String id, String nameEn, String nameVi, String descEn, String descVi,
                    ItemCategory category, SkinRarity rarity, int price) {
        this.id = id;
        this.nameEn = nameEn;
        this.nameVi = nameVi;
        this.descEn = descEn;
        this.descVi = descVi;
        this.category = category;
        this.rarity = rarity;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName(boolean isVi) {
        return isVi ? nameVi : nameEn;
    }

    public String getDescription(boolean isVi) {
        return isVi ? descVi : descEn;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public SkinRarity getRarity() {
        return rarity;
    }

    public int getPrice() {
        return price;
    }
}
