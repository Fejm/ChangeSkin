package com.github.games647.changeskin.bukkit;

import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.games647.changeskin.bukkit.listener.AsyncPlayerLoginListener;
import com.github.games647.changeskin.bukkit.listener.BungeeCordListener;
import com.github.games647.changeskin.bukkit.listener.PlayerLoginListener;
import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.SkinData;
import com.github.games647.changeskin.core.SkinStorage;
import com.github.games647.changeskin.core.UserPreferences;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ChangeSkinBukkit extends JavaPlugin {

    private boolean bungeeCord;

    protected ChangeSkinCore core;

    @Override
    public void onEnable() {
        try {
            bungeeCord = Bukkit.spigot().getConfig().getBoolean("settings.bungeecord");
        } catch (Exception | NoSuchMethodError ex) {
            getLogger().warning("Cannot check bungeecord support. You use a non-spigot build");
        }

        if (bungeeCord) {
            getLogger().info("BungeeCord detected. Activating BungeeCord support");
            getLogger().info("Make sure you installed the plugin on BungeeCord too");

            getServer().getMessenger().registerIncomingPluginChannel(this, getName(), new BungeeCordListener(this));
        } else {
            saveDefaultConfig();

            String driver = getConfig().getString("storage.driver");
            String host = getConfig().getString("storage.host", "");
            int port = getConfig().getInt("storage.port", 3306);
            String database = getConfig().getString("storage.database");

            String username = getConfig().getString("storage.username", "");
            String password = getConfig().getString("storage.password", "");

            this.core = new ChangeSkinCore(getLogger(), getDataFolder());
            SkinStorage storage = new SkinStorage(core, driver, host, port, database, username, password);
            core.setStorage(storage);
            try {
                storage.createTables();
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, "Failed to setup database. Disabling plugin...", ex);
                setEnabled(false);
                return;
            }

            core.loadDefaultSkins(getConfig().getStringList("default-skins"));

            getCommand("setskin").setExecutor(new SetSkinCommand(this));

            getServer().getPluginManager().registerEvents(new PlayerLoginListener(this), this);
            getServer().getPluginManager().registerEvents(new AsyncPlayerLoginListener(this), this);
        }
    }

    public WrappedSignedProperty convertToProperty(SkinData skinData) {
        return WrappedSignedProperty.fromValues(ChangeSkinCore.SKIN_KEY
                , skinData.getEncodedData(), skinData.getEncodedSignature());
    }

    public ChangeSkinCore getCore() {
        return core;
    }

    @Override
    public void onDisable() {
        if (core != null) {
            this.core.onDisable();
        }
    }

    public SkinStorage getStorage() {
        if (core == null) {
            return null;
        }

        return core.getStorage();
    }

    //you should call this method async
    public void setSkin(Player player, final SkinData newSkin, boolean applyNow) {
        final UserPreferences preferences = core.getStorage().getPreferences(player.getUniqueId(), false);
        preferences.setTargetSkin(newSkin);
        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                if (core.getStorage().save(newSkin)) {
                    core.getStorage().save(preferences);
                }
            }
        });
    }

    //you should call this method async
    public void setSkin(Player player, UUID targetSkin, boolean applyNow) {
        SkinData newSkin = core.getStorage().getSkin(targetSkin, true);
        if (newSkin == null) {
            newSkin = core.downloadSkin(targetSkin);
        }

        setSkin(player, newSkin, applyNow);
    }

    public boolean checkPermission(CommandSender invoker, UUID uuid) {
        if (invoker.hasPermission(getName().toLowerCase() + ".skin.whitelist." + uuid.toString())) {
            return true;
        }
        
        //disallow - not whitelisted or blacklisted
        invoker.sendMessage(ChatColor.DARK_RED + "You don't have the permission to set this skin");
        return false;
    }
}
