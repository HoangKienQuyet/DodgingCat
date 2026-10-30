package com.dodgingcat.shop;

import com.dodgingcat.persistence.GameData;
import com.dodgingcat.persistence.PersistenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Manages the shop catalog, buying items with Fishes, and ownership state.
 */
public class ShopManager {
    private static final int[] GACHA_PRICES = {50, 100, 200, 350, 500, 750, 1000, 1350, 1750, 2200, 2800, 3500, 4500, 6000, 8000};
    private static final Map<SkinRarity, Integer> GACHA_WEIGHTS = Map.of(
            SkinRarity.COMMON, 100, SkinRarity.UNCOMMON, 40,
            SkinRarity.RARE, 20, SkinRarity.LEGENDARY, 7);
    private final PersistenceManager persistenceManager;
    private final List<ShopItem> skinCatalog = new ArrayList<>();
    private final List<ShopItem> mapCatalog = new ArrayList<>();

    public ShopManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
        initializeCatalog();
    }

    private void initializeCatalog() {
        // Maps Catalog
        mapCatalog.add(new ShopItem("kitchen", "Kitchen", "Nhà Bếp",
                "Warm kitchen tiles, countertops and copper pots", "Gạch ấm, mặt bếp và nồi đồng sáng bóng",
                ItemCategory.THEME, SkinRarity.COMMON, 2500));

        mapCatalog.add(new ShopItem("living_room", "Living Room", "Phòng Khách",
                "Cozy parquet floors, comfy sofas and bookshelves", "Sàn gỗ ấm cúng, sofa êm ái và kệ sách",
                ItemCategory.THEME, SkinRarity.COMMON, 2500));

        mapCatalog.add(new ShopItem("bedroom", "Bedroom", "Phòng Ngủ",
                "Soft pastel carpets, cozy beds and starry windows", "Thảm êm ái, giường ngủ và cửa sổ ngắm sao",
                ItemCategory.THEME, SkinRarity.COMMON, 2500));

        mapCatalog.add(new ShopItem("garden", "Garden", "Sân Vườn",
                "Lush green lawns, picket fences and flower beds", "Bãi cỏ xanh mát, hàng rào gỗ và luống hoa",
                ItemCategory.THEME, SkinRarity.COMMON, 2500));

        mapCatalog.add(new ShopItem("rooftop", "Rooftop", "Mái Nhà",
                "Chimney brick tiles, full moon and glowing skyline", "Ngói ống khói, trăng sáng và thành phố đêm",
                ItemCategory.THEME, SkinRarity.COMMON, 2500));

        // Skins Catalog
        // Common
        skinCatalog.add(new ShopItem("ginger", "Ginger Tabby", "Mèo Mướp Cam",
                "Classic energetic ginger cat with warm orange stripes", "Mèo cam tinh nghịch với sọc vằn ấm áp",
                ItemCategory.SKIN, SkinRarity.COMMON, 0));

        skinCatalog.add(new ShopItem("tuxedo", "Tuxedo", "Mèo Tuxedo",
                "Dapper black and white coat dressed like a gentleman", "Bộ lông đen trắng quý phái như mặc tuxedo",
                ItemCategory.SKIN, SkinRarity.COMMON, 0));

        skinCatalog.add(new ShopItem("snow_white", "Snow White", "Mèo Tuyết Trắng",
                "Fluffy pure white feline with piercing sapphire eyes", "Lông trắng muốt như tuyết cùng mắt xanh biếc",
                ItemCategory.SKIN, SkinRarity.COMMON, 200));

        skinCatalog.add(new ShopItem("classic_gray", "Classic Gray", "Mèo Tro Xám",
                "Sleek silver-gray coat with subtle tabby markings", "Bộ lông xám bạc mượt mà và trầm tĩnh",
                ItemCategory.SKIN, SkinRarity.COMMON, 200));

        // Uncommon
        skinCatalog.add(new ShopItem("calico", "Calico Tri-color", "Mèo Tam Thể",
                "Lucky tri-color patched coat of ginger, black and white", "Bộ lông tam thể may mắn đỏ, đen và trắng",
                ItemCategory.SKIN, SkinRarity.COMMON, 450));

        skinCatalog.add(new ShopItem("siamese", "Siamese", "Mèo Xiêm",
                "Cream body with chocolate mask, pointed ears and paws", "Thân màu kem với mặt nạ sô-cô-la quý tộc",
                ItemCategory.SKIN, SkinRarity.UNCOMMON, 450));

        skinCatalog.add(new ShopItem("bengal", "Bengal Leopard", "Mèo Báo Bengal",
                "Exotic wild coat covered in distinct leopard rosettes", "Lớp lông hoa mai đốm báo hoang dã dũng mãnh",
                ItemCategory.SKIN, SkinRarity.UNCOMMON, 500));

        skinCatalog.add(new ShopItem("mochi", "Mochi", "Mochi",
                "Cream-and-peach kitten with a tiny bell collar", "Mèo kem pha đào với chiếc vòng cổ chuông nhỏ",
                ItemCategory.SKIN, SkinRarity.UNCOMMON, 500));

        skinCatalog.add(new ShopItem("blue_russian", "Blue Russian", "Mèo Nga Xanh",
                "Sleek blue-gray coat and bright emerald eyes", "Bộ lông xanh xám mượt cùng đôi mắt lục bảo",
                ItemCategory.SKIN, SkinRarity.UNCOMMON, 550));

        // Rare
        skinCatalog.add(new ShopItem("lava", "Lava Tabby", "Mèo Nham Thạch",
                "Volcanic obsidian coat with flowing animated magma stripes", "Bộ lông đá núi lửa với những đường nham thạch rực đỏ",
                ItemCategory.SKIN, SkinRarity.RARE, 850));

        skinCatalog.add(new ShopItem("super", "Super Cat", "Siêu Mèo",
                "Heroic cape and golden emblem with a speed trail", "Áo choàng siêu anh hùng và huy hiệu vàng cùng vệt tốc độ",
                ItemCategory.SKIN, SkinRarity.RARE, 1700));

        skinCatalog.add(new ShopItem("pirate", "Pirate Cat", "Mèo Cướp Biển",
                "Black tricorn captain hat, skull insignia, eye patch & bandana", "Mũ thuyền trưởng cướp biển, bịt mắt và khăn đỏ ngầu",
                ItemCategory.SKIN, SkinRarity.RARE, 1800));

        // Legendary
        skinCatalog.add(new ShopItem("cyber", "Neon Cat", "Mèo Neon",
                "Dark high-tech feline with glowing cyan visor & circuit aura", "Mèo công nghệ tương lai với kính neon phát sáng",
                ItemCategory.SKIN, SkinRarity.LEGENDARY, 800));

        skinCatalog.add(new ShopItem("ghost", "Spirit Cat", "Mèo Linh Hồn",
                "Ethereal translucent spirit cat surrounded by spectral wisps", "Linh hồn mèo trong suốt huyền ảo lơ lửng",
                ItemCategory.SKIN, SkinRarity.LEGENDARY, 900));

        // Legendary
        skinCatalog.add(new ShopItem("santa", "Santa Cat", "Mèo Ông Già Noel",
                "Cream-furred cat wearing a red Santa cape and matching hat", "Mèo lông kem khoác áo choàng và mũ Noel đỏ",
                ItemCategory.SKIN, SkinRarity.LEGENDARY, 1600));

    }

    public List<ShopItem> getSkinCatalog() {
        return Collections.unmodifiableList(skinCatalog);
    }

    public List<ShopItem> getMapCatalog() {
        return Collections.unmodifiableList(mapCatalog);
    }

    public ShopItem findItemById(String id) {
        for (ShopItem item : skinCatalog) {
            if (item.getId().equalsIgnoreCase(id)) return item;
        }
        for (ShopItem item : mapCatalog) {
            if (item.getId().equalsIgnoreCase(id)) return item;
        }
        return null;
    }

    public boolean isUnlocked(String itemId, ItemCategory category) {
        GameData data = persistenceManager.getData();
        if (category == ItemCategory.THEME) {
            return data.getUnlockedMaps().contains(itemId);
        } else {
            return data.getUnlockedSkins().contains(itemId);
        }
    }

    public boolean buyItem(ShopItem item) {
        if (isUnlocked(item.getId(), item.getCategory())) {
            return false; // Already owned
        }
        GameData data = persistenceManager.getData();
        if (data.spendFishes(item.getPrice())) {
            if (item.getCategory() == ItemCategory.THEME) {
                data.getUnlockedMaps().add(item.getId());
            } else {
                data.getUnlockedSkins().add(item.getId());
            }
            persistenceManager.save();
            return true;
        }
        return false;
    }

    public int getNextGachaPrice() {
        int pulls = persistenceManager.getData().getSkinGachaPulls();
        return pulls >= GACHA_PRICES.length ? GACHA_PRICES[GACHA_PRICES.length - 1] : GACHA_PRICES[pulls];
    }

    /** Charges one pull and awards one unowned skin using rarity weight × remaining count. */
    public ShopItem pullSkin(Random random) {
        GameData data = persistenceManager.getData();
        List<ShopItem> remaining = skinCatalog.stream()
                .filter(item -> !isUnlocked(item.getId(), ItemCategory.SKIN)).toList();
        if (remaining.isEmpty()) return null;
        int price = getNextGachaPrice();
        if (!data.spendFishes(price)) return null;

        Map<SkinRarity, Long> counts = remaining.stream().collect(java.util.stream.Collectors.groupingBy(
                ShopItem::getRarity, java.util.stream.Collectors.counting()));
        double totalWeight = counts.entrySet().stream()
                .mapToDouble(entry -> entry.getValue() * GACHA_WEIGHTS.getOrDefault(entry.getKey(), 1))
                .sum();
        double roll = random.nextDouble() * totalWeight;
        SkinRarity selectedRarity = SkinRarity.COMMON;
        for (SkinRarity rarity : List.of(SkinRarity.COMMON, SkinRarity.UNCOMMON, SkinRarity.RARE, SkinRarity.LEGENDARY)) {
            double weight = counts.getOrDefault(rarity, 0L) * GACHA_WEIGHTS.getOrDefault(rarity, 1);
            if (roll < weight) { selectedRarity = rarity; break; }
            roll -= weight;
        }
        final SkinRarity awardedRarity = selectedRarity;
        List<ShopItem> rarityPool = remaining.stream().filter(item -> item.getRarity() == awardedRarity).toList();
        ShopItem winner = rarityPool.get(random.nextInt(rarityPool.size()));
        data.getUnlockedSkins().add(winner.getId());
        data.setSkinGachaPulls(data.getSkinGachaPulls() + 1);
        persistenceManager.save();
        return winner;
    }
}
