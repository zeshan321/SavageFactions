package com.massivecraft.factions.zcore.fperms;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.SavageFactions;
import com.massivecraft.factions.util.XMaterial;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public enum PermissableAction {
    BAN("ban"),
    BUILD("build"),
    DESTROY("destroy"),
    FROST_WALK("frostwalk"),
    PAIN_BUILD("painbuild"),
    DOOR("door"),
    BUTTON("button"),
    LEVER("lever"),
    CONTAINER("container"),
    INVITE("invite"),
    KICK("kick"),
    ITEM("items"), // generic for most items
    SETHOME("sethome"),
    TERRITORY("territory"),
    ACCESS("access"),
    HOME("home"),
    DISBAND("disband"),
    PROMOTE("promote"),
    SETWARP("setwarp"),
    WARP("warp"),
    FLY("fly"),
    VAULT("vault"),
    TNTBANK("tntbank"),
    TNTFILL("tntfill"),
    WITHDRAW("withdraw"),
    CHEST("chest"),
    SPAWNER("spawner");

    private String name;

    PermissableAction(String name) {
        this.name = name;
    }

    /**
     * Case insensitive check for action.
     *
     * @param check
     * @return - action
     */
    public static PermissableAction fromString(String check) {
        for (PermissableAction permissableAction : values()) {
            if (permissableAction.name().equalsIgnoreCase(check)) {
                return permissableAction;
            }
        }

        return null;
    }

    public static Map<PermissableAction, Access> fromDefaults(DefaultPermissions defaultPermissions) {
        Map<PermissableAction, Access> defaultMap = new HashMap<>();
        for (PermissableAction permissableAction : PermissableAction.values()) {
            defaultMap.put(permissableAction, defaultPermissions.getbyName(permissableAction.name) ? Access.ALLOW : Access.DENY);
        }
        return defaultMap;
    }

    /**
     * Get the friendly name of this action. Used for editing in commands.
     *
     * @return friendly name of the action as a String.
     */
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return name;
    }

    // Utility method to build items for F Perm GUI
    public ItemStack buildItem(FPlayer fme, Permissable permissable) {
        final ConfigurationSection section = SavageFactions.plugin.getConfig().getConfigurationSection("fperm-gui.action");

        if (section == null) {
            SavageFactions.plugin.log(Level.WARNING, "Attempted to build f perm GUI but config section not present.");
            SavageFactions.plugin.log(Level.WARNING, "Copy your config, allow the section to generate, then copy it back to your old config.");
            return new ItemStack(Material.AIR);
        }

        String displayName = replacePlaceholders(section.getString("placeholder-item.name"), fme, permissable);
        List<String> lore = new ArrayList<>();

        if (section.getString("materials." + name().toLowerCase().replace('_', '-')) == null) return null;

        Material material = XMaterial.matchXMaterial(section.getString("materials." + name().toLowerCase().replace('_', '-'))).parseMaterial();

        Access access = fme.getFaction().getAccess(permissable, this);
        if (access == null) access = Access.UNDEFINED;

        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();

        String accessValue = null;

        switch (access) {
            case ALLOW:
                accessValue = "allow";
                break;
            case DENY:
                accessValue = "deny";
                break;
            case UNDEFINED:
                accessValue = "undefined";
                break;
        }

        // If under the 1.13 version we will use the colorable option.
        if (!SavageFactions.plugin.mc113 && !SavageFactions.plugin.mc114) {
            //TODO see if it's working in other version than 1.13 and 1.14
            DyeColor dyeColor = null;

            try {
                dyeColor = DyeColor.valueOf(section.getString("access." + access.name().toLowerCase()));
            } catch (Exception exception) {
            }
            accessValue = "deny";

            if (dyeColor != null) {
                item.setDurability(dyeColor.getWoolData());
            }
        } else {
            Material mat = XMaterial.CYAN_GLAZED_TERRACOTTA.parseMaterial();
            switch (accessValue) {
               case "deny": mat = XMaterial.RED_GLAZED_TERRACOTTA.parseMaterial(); break;
               case "allow": mat = XMaterial.GREEN_GLAZED_TERRACOTTA.parseMaterial(); break;
               case "undefined": mat = XMaterial.CYAN_GLAZED_TERRACOTTA.parseMaterial(); break;
            }
            item.setType(mat);
        }

        for (String loreLine : section.getStringList("placeholder-item.lore")) lore.add(replacePlaceholders(loreLine, fme, permissable));
        if (!SavageFactions.plugin.mc17) itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        itemMeta.setDisplayName(displayName);
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);

        return item;
    }

    public String replacePlaceholders(String string, FPlayer fme, Permissable permissable) {
        // Run Permissable placeholders
        string = permissable.replacePlaceholders(string);

        String actionName = name.substring(0, 1).toUpperCase() + name.substring(1);
        string = string.replace("{action}", actionName);

        Access access = fme.getFaction().getAccess(permissable, this);
        if (access == null) {
            access = Access.UNDEFINED;
        }
        String actionAccess = access.getName();
        string = string.replace("{action-access}", actionAccess);
        string = string.replace("{action-access-color}", access.getColor().toString());

        return string;
    }

}
