package com.github.games647.changeskin.bungee.listener;

import com.github.games647.changeskin.bungee.ChangeSkinBungee;
import com.github.games647.changeskin.core.SkinData;
import com.github.games647.changeskin.core.UserPreferences;

import java.util.List;
import java.util.Random;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class JoinListener implements Listener {

    private final ChangeSkinBungee plugin;
    private final Random random = new Random();

    public JoinListener(ChangeSkinBungee plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PostLoginEvent postLoginEvent) {
        ProxiedPlayer player = postLoginEvent.getPlayer();

        boolean skinFound = false;
        //try to use the existing and put it in the cache so we use it for others
        InitialHandler initialHandler = (InitialHandler) player.getPendingConnection();
        LoginResult loginProfile = initialHandler.getLoginProfile();
        //this is null on offline mode
        if (loginProfile != null) {
            Property[] properties = loginProfile.getProperties();
            for (Property property : properties) {
                //found a skin
                SkinData skinData = new SkinData(property.getValue(), property.getSignature());
                plugin.getStorage().getSkinUUIDCache().put(player.getUniqueId(), skinData);
                skinFound = true;
                break;
            }
        }

        //updates to the chosen one
        UserPreferences preferences = plugin.getStorage().getPreferences(player.getUniqueId(), true);
        SkinData targetSkin = preferences.getTargetSkin();
        if (targetSkin != null) {
            plugin.applySkin(player, targetSkin);
        } else if (!skinFound) {
            setRandomSkin(player);
        }
    }

    private void setRandomSkin(ProxiedPlayer player) {
        //skin wasn't found and there are no preferences so set a default skin
        List<SkinData> defaultSkins = plugin.getCore().getDefaultSkins();
        if (!defaultSkins.isEmpty()) {
            int randomIndex = random.nextInt(defaultSkins.size());

            final SkinData targetSkin = defaultSkins.get(randomIndex);
            if (targetSkin != null) {
                final UserPreferences preferences = plugin.getStorage().getPreferences(player.getUniqueId(), false);
                preferences.setTargetSkin(targetSkin);
                plugin.applySkin(player, targetSkin);

                ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.getStorage().save(preferences);
                    }
                });
            }
        }
    }
}
