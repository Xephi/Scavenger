package me.cnaude.plugin.Scavenger;

import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.api.GroupManager;
import com.onarandombox.multiverseinventories.api.profile.WorldGroupProfile;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.milkbowl.vault.economy.EconomyResponse;
import net.slipcor.pvparena.api.PVPArenaAPI;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class RestorationManager implements Serializable {

    private static HashMap<String, Restoration> restorations = new HashMap<String, Restoration>();

    public static void save() {
        HashMap<String, RestorationS> res_s = new HashMap<String, RestorationS>();
        for (Map.Entry<String, Restoration> entry : restorations.entrySet()) {
            String key = entry.getKey();
            Restoration value = entry.getValue();
            RestorationS restoration_s = new RestorationS();
            for (ItemStack i : value.inventory) {
                if (i instanceof ItemStack) {
                    Scavenger.get().debugMessage("Serializing: " + i.toString());
                    restoration_s.inventory.add(i.serialize());
                    Scavenger.get().debugMessage("Done: " + i.toString());
                }
            }
            for (ItemStack i : value.armour) {
                if (i instanceof ItemStack) {
                    Scavenger.get().debugMessage("Serializing: " + i.toString());
                    restoration_s.armour.add(i.serialize());
                    Scavenger.get().debugMessage("Done: " + i.toString());
                }
            }
            restoration_s.enabled = value.enabled;
            restoration_s.level = value.level;
            restoration_s.exp = value.exp;
            res_s.put(key, restoration_s);
            Scavenger.get().logInfo("Saving " + key + "'s inventory to disk.");
        }
        try {
            File file = new File("plugins/Scavenger/inv.ser");
            FileOutputStream f_out = new FileOutputStream(file);
            ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
            obj_out.writeObject(res_s);
            obj_out.close();
        } catch (Exception e) {
            Scavenger.get().logError(e.getMessage());
        }
    }

    public static void load() {
        HashMap<String, RestorationS> res_s;
        File file = new File("plugins/Scavenger/inv.ser");
        if (!file.exists()) {
            Scavenger.get().logInfo("Recovery file '" + file.getAbsolutePath() + "' does not exist.");
            return;
        }
        try {
            FileInputStream f_in = new FileInputStream(file);
            ObjectInputStream obj_in = new ObjectInputStream(f_in);
            res_s = (HashMap<String, RestorationS>) obj_in.readObject();
            obj_in.close();
        } catch (Exception e) {
            Scavenger.get().logError(e.getMessage());
            return;
        }

        for (Map.Entry<String, RestorationS> entry : res_s.entrySet()) {
            String key = entry.getKey();
            RestorationS value = entry.getValue();
            Restoration restoration = new Restoration();
            restoration.inventory = new ItemStack[value.inventory.size()];
            restoration.armour = new ItemStack[value.armour.size()];

            for (int i = 0; i < value.inventory.size(); i++) {
                if (value.inventory.get(i) instanceof Map) {
                    Scavenger.get().debugMessage("Deserializing: " + value.inventory.get(i).toString());
                    restoration.inventory[i] = ItemStack.deserialize(value.inventory.get(i));
                    Scavenger.get().debugMessage("Done: " + restoration.inventory[i].toString());
                }
            }
            for (int i = 0; i < value.armour.size(); i++) {
                if (value.armour.get(i) instanceof Map) {
                    Scavenger.get().debugMessage("Deserializing: " + value.armour.get(i).toString());
                    restoration.armour[i] = ItemStack.deserialize(value.armour.get(i));
                    Scavenger.get().debugMessage("Done: " + restoration.armour[i].toString());
                }
            }
            restoration.enabled = value.enabled;
            restoration.level = value.level;
            restoration.exp = value.exp;

            restorations.put(key, restoration);
            Scavenger.get().logInfo("Loading " + key + "'s inventory from disk.");
        }
    }

    public static boolean hasRestoration(Player _player) {
        if (getRestoration(_player) != null) {
            return true;
        } else {
            return false;
        }
    }

    private static Restoration getRestoration(Player _player) {
        if (Scavenger.get().getMultiverseInventories() != null) {
            List<WorldGroupProfile> groupProfiles = Scavenger.get().getMultiverseInventories().getGroupManager().getGroupsForWorld(_player.getWorld().getName());
            if (restorations.containsKey(_player.getName() + groupProfiles.get(0).getName())) {
                return restorations.get(_player.getName() + groupProfiles.get(0).getName());
            } else {
                return restorations.get(_player.getName());
            }
        } else {
            return restorations.get(_player.getName());
        }
    }

    public static void collect(Player _player, List<ItemStack> _drops, EntityDeathEvent event) {
        if (_drops.isEmpty() && _player.getExp() == 0 && _player.getLevel() == 0) {
            return;
        }

        if (Scavenger.get().getWorldGuard() != null) {
            Scavenger.get().logDebug("Checking region support for '" + _player.getWorld().getName() + "'");
            if (Scavenger.get().getWorldGuard().getRegionManager(_player.getWorld()) != null) {
                RegionManager regionManager = Scavenger.get().getWorldGuard().getRegionManager(_player.getWorld());
                ApplicableRegionSet set = regionManager.getApplicableRegions(_player.getLocation());
                if (set.allows(DefaultFlag.PVP) && Scavenger.getSConfig().wgPVPIgnore()) {
                    Scavenger.get().logDebug("This is a WorldGuard PVP zone and WorldGuardPVPIgnore is " + Scavenger.getSConfig().wgPVPIgnore());
                    if (!Scavenger.getSConfig().msgInsideWGPVP().isEmpty()) {
                        Scavenger.get().message(_player, Scavenger.getSConfig().msgInsideWGPVP());
                    }
                    return;
                }
                if (!set.allows(DefaultFlag.PVP) && Scavenger.getSConfig().wgGuardPVPOnly()) {
                    Scavenger.get().logDebug("This is NOT a WorldGuard PVP zone and WorldGuardPVPOnly is " + Scavenger.getSConfig().wgGuardPVPOnly());
                    if (!Scavenger.getSConfig().msgInsideWGPVP().isEmpty()) {
                        Scavenger.get().message(_player, Scavenger.getSConfig().msgInsideWGPVPOnly());
                    }
                    return;
                }
            } else {
                Scavenger.get().logDebug("Region support disabled for '" + _player.getWorld().getName() + "'");
            }
        }

        if (Scavenger.get().getUltimateArena() != null) {
            if (Scavenger.get().getUltimateArena().isInArena(_player)) {
                if (!Scavenger.getSConfig().msgInsideUA().isEmpty()) {
                    Scavenger.get().message(_player, Scavenger.getSConfig().msgInsideUA());
                }
                return;
            }
        }

        if (Scavenger.maHandler != null && Scavenger.maHandler.isPlayerInArena(_player)) {
            if (!Scavenger.getSConfig().msgInsideMA().isEmpty()) {
                Scavenger.get().message(_player, Scavenger.getSConfig().msgInsideMA());
            }
            return;
        }

        if (Scavenger.pvpHandler != null && !PVPArenaAPI.getArenaName(_player).equals("")) {
            String x = Scavenger.getSConfig().msgInsidePA();
            if (!x.isEmpty()) {
                x = x.replaceAll("%ARENA%", PVPArenaAPI.getArenaName(_player));
                Scavenger.get().message(_player, x);
            }
            return;
        }

        List<String> tempRespawnGroups = getWorldGroups(_player.getWorld());

        if (hasRestoration(_player)) {
            Scavenger.get().error(_player, "Restoration already exists, ignoring.");
            return;
        }

        if (Scavenger.get().getEconomy() != null
                && !(_player.hasPermission("scavenger.free")
                || (_player.isOp() && Scavenger.getSConfig().opsAllPerms()))
                && Scavenger.getSConfig().economyEnabled()) {
            double restore_cost = Scavenger.getSConfig().restoreCost();
            double withdraw_amount;
            double player_balance = Scavenger.get().getEconomy().getBalance(_player.getName());
            double percent_cost = Scavenger.getSConfig().percentCost();
            double min_cost = Scavenger.getSConfig().minCost();
            double max_cost = Scavenger.getSConfig().maxCost();
            EconomyResponse er;
            String currency;
            if (Scavenger.getSConfig().percent()) {
                withdraw_amount = player_balance * (percent_cost / 100.0);
                if (Scavenger.getSConfig().addMin()) {
                    withdraw_amount = withdraw_amount + min_cost;
                } else if (withdraw_amount < min_cost) {
                    withdraw_amount = min_cost;
                }
                if (withdraw_amount > max_cost && max_cost > 0) {
                    withdraw_amount = max_cost;
                }
            } else {
                withdraw_amount = restore_cost;
            }
            er = Scavenger.get().getEconomy().withdrawPlayer(_player.getName(), withdraw_amount);
            if (er.transactionSuccess()) {
                if (withdraw_amount == 1) {
                    currency = Scavenger.get().getEconomy().currencyNameSingular();
                } else {
                    currency = Scavenger.get().getEconomy().currencyNamePlural();
                }
                String x = Scavenger.getSConfig().msgSaveForFee();
                if (!x.isEmpty()) {
                    x = x.replaceAll("%COST%", String.format("%.2f", withdraw_amount));
                    x = x.replaceAll("%CURRENCY%", currency);
                    Scavenger.get().message(_player, x);
                }
            } else {
                if (player_balance == 1) {
                    currency = Scavenger.get().getEconomy().currencyNameSingular();
                } else {
                    currency = Scavenger.get().getEconomy().currencyNamePlural();
                }
                String x = Scavenger.getSConfig().msgNotEnoughMoney();
                if (!x.isEmpty()) {
                    x = x.replaceAll("%BALANCE%", String.format("%.2f", player_balance));
                    x = x.replaceAll("%COST%", String.format("%.2f", withdraw_amount));
                    x = x.replaceAll("%CURRENCY%", currency);
                    Scavenger.get().message(_player, x);
                }
                return;
            }
        } else {
            Scavenger.get().message(_player, Scavenger.getSConfig().msgSaving());
        }
        Restoration restoration = new Restoration();

        restoration.enabled = false;

        restoration.inventory = _player.getInventory().getContents();
        restoration.armour = _player.getInventory().getArmorContents();



        if (_player.hasPermission("scavenger.level")
                || !Scavenger.getSConfig().permsEnabled()
                || (_player.isOp() && Scavenger.getSConfig().opsAllPerms())) {
            restoration.level = _player.getLevel();
        }
        if (_player.hasPermission("scavenger.exp")
                || !Scavenger.getSConfig().permsEnabled()
                || (_player.isOp() && Scavenger.getSConfig().opsAllPerms())) {
            restoration.exp = _player.getExp();
            event.setDroppedExp(0);
        }

        _drops.clear();

        if (Scavenger.getSConfig().singleItemDrops()) {
            ItemStack[][] invAndArmour = {restoration.inventory, restoration.armour};
            for (ItemStack[] a : invAndArmour) {
                for (ItemStack i : a) {
                    boolean dropIt;
                    if (i instanceof ItemStack && !i.getType().equals(Material.AIR)) {
                        if (Scavenger.getSConfig().singleItemDropsOnly() == true) {
                            if (_player.hasPermission("scavenger.drop." + i.getTypeId())) {
                                dropIt = false;
                            } else {
                                dropIt = true;
                            }
                        } else {
                            if (!_player.hasPermission("scavenger.drop." + i.getTypeId())) {
                                dropIt = false;
                            } else {
                                dropIt = true;
                            }
                        }
                        if (dropIt) {
                            Scavenger.get().debugMessage(_player, "Dropping item " + i.getType());
                            _drops.add(i.clone());
                            i.setAmount(0);
                        } else {
                            Scavenger.get().debugMessage(_player, "Keeping item " + i.getType());
                        }
                    }
                }
            }
        }
        if (Scavenger.get().getMultiverseInventories() != null) {
            restorations.put(_player.getName() + tempRespawnGroups.get(0), restoration);
        } else {
            restorations.put(_player.getName(), restoration);
        }
    }

    public static void enable(Player _player) {
        if (hasRestoration(_player)) {
            Restoration restoration = getRestoration(_player);
            restoration.enabled = true;
        }

    }

    public static void restore(Player _player) {
        Restoration restoration = getRestoration(_player);

        if (restoration.enabled) {
            _player.getInventory().clear();

            _player.getInventory().setContents(restoration.inventory);
            _player.getInventory().setArmorContents(restoration.armour);
            if (_player.hasPermission("scavenger.level")
                    || !Scavenger.getSConfig().permsEnabled()
                    || (_player.isOp() && Scavenger.getSConfig().opsAllPerms())) {
                _player.setLevel(restoration.level);
            }
            if (_player.hasPermission("scavenger.exp")
                    || !Scavenger.getSConfig().permsEnabled()
                    || (_player.isOp() && Scavenger.getSConfig().opsAllPerms())) {
                _player.setExp(restoration.exp);
            }
            if (Scavenger.getSConfig().shouldNotify()) {
                Scavenger.get().message(_player, Scavenger.getSConfig().msgRecovered());
            }
            removeRestoration(_player);
        }

    }
    
    public static void removeRestoration(Player _player) {
        if (Scavenger.get().getMultiverseInventories() != null) {
            List<WorldGroupProfile> groupProfiles = Scavenger.get().getMultiverseInventories().getGroupManager().getGroupsForWorld(_player.getWorld().getName());
            if (restorations.containsKey(_player.getName() + groupProfiles.get(0).getName())) {
                restorations.remove(_player.getName() + groupProfiles.get(0).getName());
            } else {
                restorations.remove(_player.getName());
            }
        } else {
            restorations.remove(_player.getName());
        }
    }

    public static List<String> getWorldGroups(World world) {
        List<String> returnData = new ArrayList<String>();
        if (Scavenger.get().getMultiverseInventories() != null) {
            MultiverseInventories multiInv = Scavenger.get().getMultiverseInventories();
            if (multiInv.getGroupManager() != null) {
                GroupManager groupManager = multiInv.getGroupManager();
                if (groupManager.getGroupsForWorld(world.getName()) != null) {
                    List<WorldGroupProfile> worldGroupProfiles = groupManager.getGroupsForWorld(world.getName());
                    if (worldGroupProfiles != null) {
                        for (WorldGroupProfile i : worldGroupProfiles) {
                            returnData.add(i.getName());
                        }
                    }
                }

            }
        }
        return returnData;
    }
}