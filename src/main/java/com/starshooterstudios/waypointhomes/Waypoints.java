package com.starshooterstudios.waypointhomes;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Waypoints extends JavaPlugin implements Listener {
    private static NamespacedKey waypointKey;
    private static NamespacedKey waypointDataKey;
    private static NamespacedKey waypointTransporterKey;

    private static NamespacedKey waypointGuiButtonKey;
    private static NamespacedKey waypointGuiDataKey;

    private static Waypoints instance;

    private ItemStack empty;
    private ItemStack waypointItem;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        SupernovaUtils.initialize(this);
        initializeInternal(this);
        initialize(this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws ExecutionException, InterruptedException {
        if (!getConfig().getBoolean("enable-resource-pack")) return;
        ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo()
                .uri(URI.create("https://github.com/cometcake575/WaypointHomes/raw/refs/heads/main/WaypointPack.zip"))
                .computeHashAndBuild().get();
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> event.getPlayer().sendResourcePacks(packInfo), 5);
    }

    public void initialize(JavaPlugin plugin) {
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        empty = SupernovaUtils.createItem(Material.ORANGE_STAINED_GLASS_PANE, meta -> {
            meta.setHideTooltip(true);
            meta.displayName(Component.empty());
            meta.setCustomModelData(1);
        });

        waypointKey = new NamespacedKey(plugin, "waypoint");
        waypointDataKey = new NamespacedKey(plugin, "waypoint-data");
        waypointTransporterKey = new NamespacedKey(plugin, "waypoint-transporter");
        waypointGuiButtonKey = new NamespacedKey(plugin, "waypoint-gui-button");
        waypointGuiDataKey = new NamespacedKey(plugin, "waypoint-gui-data");

        waypointItem = SupernovaUtils.createItem(Material.CLAY_BALL, meta -> {
            meta.setCustomModelData(1);
            meta.getPersistentDataContainer().set(SupernovaUtils.customUsableItemKey, PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(waypointKey, PersistentDataType.BOOLEAN, true);
            meta.displayName(Component.text("Waypoint").decoration(TextDecoration.ITALIC, false));
        });

        ItemStack waypointTransporter = SupernovaUtils.createItem(Material.CLAY_BALL, meta -> {
            meta.setCustomModelData(2);
            meta.getPersistentDataContainer().set(SupernovaUtils.customUsableItemKey, PersistentDataType.BOOLEAN, true);
            meta.setMaxStackSize(1);
            meta.getPersistentDataContainer().set(waypointTransporterKey, PersistentDataType.BOOLEAN, true);
            meta.displayName(Component.text("Waypoint Transporter").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        });

        RecipeBuilder.builder(waypointItem, waypointKey)
                .shape("AGA", "GEG", "AOA")
                .addItem('G', Material.GLASS)
                .addItem('E', Material.ENDER_EYE)
                .addItem('O', Material.OBSIDIAN)
                .buildAndRegister();

        RecipeBuilder.builder(waypointTransporter, waypointTransporterKey)
                .shape("ASA", "ARA", "ARA")
                .addItem('S', Material.NETHER_STAR)
                .addItem('R', Material.BREEZE_ROD)
                .buildAndRegister();


        NamespacedKey placeWaypointKey = new NamespacedKey(plugin, "place_waypoint");
        placeWaypointAdvancement = CustomAdvancements.makeAdvancement(
                placeWaypointKey,
                "I'll be here often.",
                "Place a Waypoint",
                waypointItem,
                CustomAdvancements.AdvancementFrame.TASK,
                new CustomAdvancements.ParentedAdvancementData(Key.key("minecraft:adventure/root")),
                false,
                true,
                true,
                false
        );
        useTransporterAdvancement = CustomAdvancements.makeAdvancement(
                new NamespacedKey(plugin, "use_transporter"),
                "Gone with the Wind",
                "Teleport to a Waypoint with a Waypoint Transporter",
                waypointTransporter,
                CustomAdvancements.AdvancementFrame.GOAL,
                new CustomAdvancements.ParentedAdvancementData(placeWaypointKey),
                false,
                true,
                true,
                false
        );
    }

    private CustomAdvancements.Advancement placeWaypointAdvancement;
    private CustomAdvancements.Advancement useTransporterAdvancement;

    public String getConfigName() {
        return "waypoint_data.yml";
    }

    public void save() {
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileConfiguration getWaypointData() {
        return fileConfiguration;
    }

    private FileConfiguration fileConfiguration;
    private File file;

    public void initializeInternal(JavaPlugin plugin) {
        file = new File(plugin.getDataFolder(), getConfigName());
        if (!file.exists()) {
            boolean ignored = file.getParentFile().mkdirs();
            plugin.saveResource(getConfigName(), false);
        }

        fileConfiguration = new YamlConfiguration();

        try {
            fileConfiguration.load(file);
        } catch (InvalidConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == null) return;
        if (!event.getAction().isRightClick()) return;
        if (event.getClickedBlock() != null) {
            if (!Tag.STAIRS.isTagged(event.getClickedBlock().getType())) {
                if (event.getClickedBlock().getType().isInteractable()) return;
            }
        }
        waypointCheck(event.getPlayer(), event.getItem(), EquipmentSlot.HAND.equals(event.getHand()));
    }

    public void waypointCheck(Player player, ItemStack item, boolean isMainHand) {
        if (item == null || item.getItemMeta() == null) return;
        if (item.getPersistentDataContainer().has(waypointTransporterKey)) {
            if (isMainHand) player.swingMainHand();
            else player.swingOffHand();
            openWaypointTransporter(player, 0);
        } else if (item.getPersistentDataContainer().has(waypointKey)) {
            Waypoint waypoint = new Waypoint(null, item.getItemMeta().displayName(), player.getLocation(), UUID.randomUUID());
            saveWaypoint(player, waypoint);

            item.setAmount(item.getAmount() - 1);
            placeWaypointAdvancement.grant(player);
            if (isMainHand) {
                player.getInventory().setItemInMainHand(item);
                player.swingMainHand();
            }
            else {
                player.getInventory().setItemInOffHand(item);
                player.swingOffHand();
            }
        }
    }

    public void openWaypointTransporter(HumanEntity player, int page) {
        List<Waypoint> waypoints = getWaypoints(player);
        if (page * 21 >= waypoints.size()) page = 0;
        if (page < 0) page = Math.ceilDiv(waypoints.size(), 21) - 1;
        for (int i = 0; i < page * 21; i++) {
            if (!waypoints.isEmpty()) waypoints.removeFirst();
        }

        Inventory inventory = CustomGUI.createInventory(CustomGUI.CustomInventoryType.WAYPOINT_TRANSPORTER, 27, Component.text().append(Component.text("\uF000\uE000\uF001").font(Key.key("supernova:waypoint_transporter")).color(NamedTextColor.WHITE)).append(Component.text("Waypoint Transporter - %s".formatted(page+1))).build());
        int finalPage = page;
        ItemStack left = SupernovaUtils.createItem(Material.ORANGE_STAINED_GLASS_PANE, meta -> {
            meta.setCustomModelData(6);
            meta.displayName(Component.text("Left").decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(waypointGuiButtonKey, PersistentDataType.STRING, "L");
            meta.getPersistentDataContainer().set(waypointGuiDataKey, PersistentDataType.INTEGER, finalPage);
        });
        ItemStack right = SupernovaUtils.createItem(Material.ORANGE_STAINED_GLASS_PANE, meta -> {
            meta.setCustomModelData(7);
            meta.displayName(Component.text("Right").decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(waypointGuiButtonKey, PersistentDataType.STRING, "R");
            meta.getPersistentDataContainer().set(waypointGuiDataKey, PersistentDataType.INTEGER, finalPage);
        });

        inventory.setItem(0, empty);
        inventory.setItem(8, empty);
        inventory.setItem(9, left);
        inventory.setItem(17, right);
        inventory.setItem(18, empty);
        inventory.setItem(26, empty);

        for (Waypoint waypoint : waypoints) {
            if (!inventory.addItem(waypoint.asItem(page)).isEmpty()) break;
        }

        for (int i = 0; i < 27; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().equals(Material.AIR)) continue;
            inventory.setItem(i, empty);
        }

        player.openInventory(inventory);
    }

    public void saveWaypoint(HumanEntity player, Waypoint waypoint) {
        String c = "%s.%s".formatted(player.getUniqueId().toString(), waypoint.uuid.toString());
        if (waypoint.icon != null) {
            getWaypointData().set(c + ".icon", waypoint.icon.serializeAsBytes());
        } else getWaypointData().set(c + ".icon", null);
        getWaypointData().set(c + ".name", JSONComponentSerializer.json().serialize(waypoint.name));
        Map<String, Object> serialisedLoc = waypoint.location.serialize();
        for (String s : serialisedLoc.keySet()) {
            getWaypointData().set(c + ".loc." + s, serialisedLoc.get(s));
        }
        save();
    }

    public void removeWaypoint(HumanEntity player, Waypoint waypoint) {
        getWaypointData().set("%s.%s".formatted(player.getUniqueId().toString(), waypoint.uuid.toString()), null);
        save();
    }

    public Waypoint getWaypoint(HumanEntity player, String id) {
        String c = "%s.%s".formatted(player.getUniqueId().toString(), id);
        ItemStack item = null;
        if (getWaypointData().contains(c + ".icon")) {
            byte[] bytes = (byte[]) getWaypointData().get(c + ".icon");
            item = ItemStack.deserializeBytes(bytes);
        }

        Component display = JSONComponentSerializer.json().deserializeOr(getWaypointData().getString(c + ".name", ""), Component.empty());

        Location loc;
        Map<String, Object> data = new HashMap<>();
        for (String s : List.of("world", "x", "y", "z", "pitch", "yaw")) {
            data.put(s, getWaypointData().get(c + ".loc." + s));
        }
        loc = Location.deserialize(data);

        return new Waypoint(item, display, loc, UUID.fromString(id));
    }

    public List<Waypoint> getWaypoints(HumanEntity player) {
        ConfigurationSection section = getWaypointData().getConfigurationSection(player.getUniqueId().toString());
        if (section == null) return List.of();
        List<Waypoint> waypoints = new ArrayList<>();
        for (String s : section.getKeys(false)) {
            waypoints.add(getWaypoint(player, s));
        }
        return waypoints;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked().getOpenInventory().getTopInventory().getHolder() instanceof CustomGUI gui) {
            if (!gui.getInventoryType().equals(CustomGUI.CustomInventoryType.WAYPOINT_TRANSPORTER)) return;
            if (gui.getInventory().equals(event.getClickedInventory())) {
                event.setCancelled(true);
                if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;
                String s = event.getCurrentItem().getPersistentDataContainer().get(waypointGuiButtonKey, PersistentDataType.STRING);
                if (s != null) {
                    switch (s) {
                        case "L" -> openWaypointTransporter(event.getWhoClicked(), event.getCurrentItem().getPersistentDataContainer().getOrDefault(waypointGuiDataKey, PersistentDataType.INTEGER, 0)-1);
                        case "R" -> openWaypointTransporter(event.getWhoClicked(), event.getCurrentItem().getPersistentDataContainer().getOrDefault(waypointGuiDataKey, PersistentDataType.INTEGER, 0)+1);
                    }
                }
                if (!event.getCurrentItem().getPersistentDataContainer().has(waypointDataKey)) return;
                Waypoint waypoint = Waypoint.fromItem(event.getWhoClicked(), event.getCurrentItem());
                if (event.getClick().isRightClick()) {
                    if (event.getClick().isShiftClick()) {
                        ItemStack w = waypointItem.clone();
                        ItemMeta meta = w.getItemMeta();
                        meta.displayName(waypoint.name);
                        w.setItemMeta(meta);
                        ItemStack i = waypoint.icon;
                        Collection<ItemStack> items;
                        if (i != null) items = event.getWhoClicked().getInventory().addItem(w, i).values();
                        else items = event.getWhoClicked().getInventory().addItem(w).values();
                        for (ItemStack item : items) {
                            ((CraftHumanEntity) event.getWhoClicked()).getHandle().drop(CraftItemStack.asNMSCopy(item), true);
                        }
                        removeWaypoint(event.getWhoClicked(), waypoint);
                        save();
                        openWaypointTransporter(event.getWhoClicked(), event.getCurrentItem().getPersistentDataContainer().getOrDefault(waypointGuiDataKey, PersistentDataType.INTEGER, 0));
                    } else {
                        ItemStack old = waypoint.icon;
                        ItemStack cursor = event.getCursor();
                        if (cursor.getType().isAir()) cursor = null;
                        waypoint.icon = cursor;
                        event.getWhoClicked().setItemOnCursor(old);
                        saveWaypoint(event.getWhoClicked(), waypoint);
                        save();
                        openWaypointTransporter(event.getWhoClicked(), event.getCurrentItem().getPersistentDataContainer().getOrDefault(waypointGuiDataKey, PersistentDataType.INTEGER, 0));
                    }
                    return;
                } else if (event.getClick().isShiftClick()) return;
                event.getWhoClicked().closeInventory();
                useTransporterAdvancement.grant((Player) event.getWhoClicked());
                event.getWhoClicked().teleport(waypoint.location);
            }
        }
    }

    public static class Waypoint {
        private @Nullable ItemStack icon;
        private final Component name;
        private final Location location;
        private final UUID uuid;

        public Waypoint(@Nullable ItemStack icon, Component name, Location location, UUID uuid) {
            this.icon = icon;
            this.name = name;
            this.location = location;
            this.uuid = uuid;
        }

        public ItemStack asItem(int page) {
            ItemStack item;
            if (icon == null) {
                item = SupernovaUtils.createItem(Material.CLAY_BALL, meta -> meta.setCustomModelData(1));
            } else item = icon.clone();
            ItemMeta meta = item.getItemMeta();
            meta.displayName(name.decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
            meta.getPersistentDataContainer().set(waypointGuiDataKey, PersistentDataType.INTEGER, page);
            meta.getPersistentDataContainer().set(waypointDataKey, PersistentDataType.STRING, uuid.toString());
            String worldName = switch (location.getWorld().getEnvironment()) {
                case NORMAL -> "Overworld";
                case NETHER -> "Nether";
                case THE_END -> "End";
                case CUSTOM -> "Portal Network";
            };
            meta.lore(List.of(
                    Component.text("X: %d, Y: %d, Z: %d in The %s".formatted(Math.round(location.x()), Math.round(location.y()), Math.round(location.z()), worldName)).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to ").color(NamedTextColor.AQUA).append(Component.text("Teleport").color(NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Right click to ").color(NamedTextColor.AQUA).append(Component.text("Set Display").color(NamedTextColor.YELLOW)).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Shift right-click to ").color(NamedTextColor.AQUA).append(Component.text("Delete").color(NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
            return item;
        }

        public static Waypoint fromItem(HumanEntity player, ItemStack item) {
            String s = item.getPersistentDataContainer().get(waypointDataKey, PersistentDataType.STRING);
            return Waypoints.instance.getWaypoint(player, s);
        }
    }


    /*
    Sort options:
    - Closest (filters out different dimensions)
    - Furthest (filters out different dimensions)
    - Newest
    - Oldest
    - Most visited
    - Least visited
     */

    /*
    Filter options:
    - Name (exact - golden tick on far right of anvil, contains - green tick in middle, cancel - far left button)
    - Tag (same as name)
    - Overworld
    - Nether
    - End


    public interface WaypointFilter {
        List<WaypointFilter> NONE = List.of((waypoints, location) -> waypoints);
        WaypointFilter CLOSEST = (waypoints, location) -> {
            List<Waypoint> newWaypoints = new ArrayList<>();
            for (Waypoint waypoint : waypoints) {
                if (!waypoint.location.getWorld().equals(location.getWorld())) continue;
                newWaypoints.add(waypoint);
            }
            newWaypoints.sort(Comparator.comparingDouble(o -> o.location.distance(location)));
            return newWaypoints;
        };
        WaypointFilter FURTHEST = (waypoints, location) -> {
            List<Waypoint> newWaypoints = new ArrayList<>();
            for (Waypoint waypoint : waypoints) {
                if (!waypoint.location.getWorld().equals(location.getWorld())) continue;
                newWaypoints.add(waypoint);
            }
            newWaypoints.sort(Comparator.comparingDouble(o -> -o.location.distance(location)));
            return newWaypoints;
        };
        WaypointFilter OVERWORLD = (waypoints, location) -> {
            List<Waypoint> newWaypoints = new ArrayList<>(waypoints);
            newWaypoints.removeIf(waypoint -> !waypoint.location.getWorld().equals(SupernovaUtils.overworld));
            return newWaypoints;
        };
        WaypointFilter NETHER = (waypoints, location) -> {
            List<Waypoint> newWaypoints = new ArrayList<>(waypoints);
            newWaypoints.removeIf(waypoint -> !waypoint.location.getWorld().equals(SupernovaUtils.nether));
            return newWaypoints;
        };
        WaypointFilter END = (waypoints, location) -> {
            List<Waypoint> newWaypoints = new ArrayList<>(waypoints);
            newWaypoints.removeIf(waypoint -> !waypoint.location.getWorld().equals(SupernovaUtils.end));
            return newWaypoints;
        };

        List<Waypoint> filter(List<Waypoint> waypoints, Location location);
    }
     */
}
