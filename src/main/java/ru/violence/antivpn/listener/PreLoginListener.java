package ru.violence.antivpn.listener;

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
import ru.violence.antivpn.AntiVPNPlugin;
import ru.violence.antivpn.checker.BypassType;
import ru.violence.antivpn.checker.CheckResult;
import ru.violence.antivpn.checker.IPChecker;

import java.net.InetSocketAddress;

public class PreLoginListener implements Listener {
    private final AntiVPNPlugin plugin;

    public PreLoginListener(AntiVPNPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        try {
            event.registerIntent(plugin);

            String playerName = event.getConnection().getName();
            String playerIp = ((InetSocketAddress) event.getConnection().getSocketAddress()).getAddress().getHostAddress();

            IPChecker checker = plugin.getIpChecker();
            CheckResult result = checker.check(playerIp);

            if (checker.isBypassed(BypassType.PLAYER_NAME, playerName)
                    || checker.isBypassed(BypassType.ISP, result.getIsp())
                    || checker.isBypassed(BypassType.ORG, result.getOrg())
                    || checker.isBypassed(BypassType.AS, result.getAs())
                    || checker.isBypassed(BypassType.ASNAME, result.getAsname())) {
                return;
            }

            boolean isDenied = result.isProxy();

            if (!isDenied && plugin.getConfig().getBoolean("deny-hostings") && result.isHosting()) {
                isDenied = true;
            }

            if (isDenied) {
                event.setCancelReason(TextComponent.fromLegacyText(plugin.getConfig().getString("kick-reason")));
                event.setCancelled(true);
                ProxyServer.getInstance().getConsole().sendMessage(TextComponent.fromLegacyText("§c[AntiVPN] " + playerName + " detected as a proxy: " + playerIp));
                notifyStaff(playerName, playerIp, result);
            }
        } catch (Exception ignored) {
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
