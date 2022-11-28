package ru.violence.antivpn.bungee.listener;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import ru.violence.antivpn.bungee.AntiVPNPlugin;
import ru.violence.antivpn.common.checker.CheckResult;
import ru.violence.antivpn.common.checker.FieldType;
import ru.violence.antivpn.common.checker.IPChecker;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PreLoginListener implements Listener {
    private final AntiVPNPlugin plugin;

    public PreLoginListener(AntiVPNPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (event.isCancelled()) return;

        try {
            event.registerIntent(plugin);

            IPChecker checker = plugin.getIpChecker();

            String playerName = event.getConnection().getName();
            if (checker.isBypassed(FieldType.PLAYER_NAME, playerName)) return;

            String playerIp = ((InetSocketAddress) event.getConnection().getSocketAddress()).getAddress().getHostAddress();

            CheckResult result = checker.check(playerIp).get(plugin.getConfig().getLong("result-await"), TimeUnit.MILLISECONDS);

            if (checker.isBypassed(FieldType.ISP, result.getIsp())
                    || checker.isBypassed(FieldType.ORG, result.getOrg())
                    || checker.isBypassed(FieldType.AS, result.getAs())
                    || checker.isBypassed(FieldType.ASNAME, result.getAsname())) {
                return;
            }

            boolean isDenied = result.isProxy();

            if (!isDenied && plugin.getConfig().getBoolean("deny-hosting") && result.isHosting()) {
                isDenied = true;
            }

            if (checker.isBlocked(FieldType.PLAYER_NAME, playerName)
                    || checker.isBlocked(FieldType.ISP, result.getIsp())
                    || checker.isBlocked(FieldType.ORG, result.getOrg())
                    || checker.isBlocked(FieldType.AS, result.getAs())
                    || checker.isBlocked(FieldType.ASNAME, result.getAsname())) {
                isDenied = true;
            }

            if (isDenied) {
                event.setCancelReason(TextComponent.fromLegacyText(plugin.getConfig().getString("kick-reason")));
                event.setCancelled(true);
                ProxyServer.getInstance().getConsole().sendMessage(TextComponent.fromLegacyText("§c[AntiVPN] " + playerName + " detected as a proxy: " + playerIp));
                notifyStaff(playerName, playerIp, result);
                return;
            }

            if (plugin.getConfig().getBoolean("country-blocker.enabled")) {
                List<String> countries = plugin.getConfig().getStringList("country-blocker.countries");
                boolean contains = countries.contains(result.getCountryCode());
                boolean isBlockedCountry = plugin.getConfig().getBoolean("country-blocker.whitelist") != contains;

                if (isBlockedCountry) {
                    event.setCancelReason(TextComponent.fromLegacyText(plugin.getConfig().getString("country-blocker.kick-reason")));
                    event.setCancelled(true);
                    ProxyServer.getInstance().getConsole().sendMessage(TextComponent.fromLegacyText("§c[AntiVPN] " + playerName + " connected from the blocked country: " + result.getCountryCode()));
                }
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("force-check.enabled")) {
                event.setCancelReason(TextComponent.fromLegacyText(plugin.getConfig().getString("force-check.kick-reason")));
                event.setCancelled(true);
            }
        } finally {
            event.completeIntent(plugin);
        }
    }

    private void notifyStaff(String playerName, String playerIp, CheckResult result) {
        BaseComponent[] message = null;

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (!player.hasPermission("antivpn.kick.notify")) continue;

            if (message == null) {
                ComponentBuilder cb = new ComponentBuilder();
                cb.append(TextComponent.fromLegacyText("§c[AntiVPN] " + playerName + " tried to join the server, but is disallowed."));
                cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(result.getJson().toString())));
                cb.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, playerIp));
                message = cb.create();
            }

            player.sendMessage(message);
        }
    }
}
