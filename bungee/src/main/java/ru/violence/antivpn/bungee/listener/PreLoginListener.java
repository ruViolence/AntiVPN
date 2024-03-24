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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.antivpn.bungee.AntiVPNPlugin;
import ru.violence.antivpn.common.config.Config;
import ru.violence.antivpn.common.model.CheckResult;
import ru.violence.antivpn.common.model.exception.BypassedException;
import ru.violence.antivpn.common.model.exception.CheckErrorException;
import ru.violence.antivpn.common.model.exception.CountryBlockedException;
import ru.violence.antivpn.common.model.exception.HostingBlockedException;
import ru.violence.antivpn.common.model.exception.ManuallyBlockedException;
import ru.violence.antivpn.common.model.exception.ProxyBlockedException;

import java.net.InetSocketAddress;

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

            String playerName = event.getConnection().getName();
            String playerIp = ((InetSocketAddress) event.getConnection().getSocketAddress()).getAddress().getHostAddress();

            try {
                plugin.getIpChecker().checkPlayer(playerName, playerIp);
            } catch (BypassedException e) {
                // NOOP
            } catch (CountryBlockedException e) {
                event.setCancelReason(TextComponent.fromLegacyText(Config.IpApi.COUNTRY_BLOCKER_KICK_REASON));
                event.setCancelled(true);
                notifyStaff(playerIp, "§c[AntiVPN] " + playerName + " connecting from the blocked country: " + e.getCheckResult().getCountryCode(), e.getCheckResult());
            } catch (ProxyBlockedException e) {
                event.setCancelReason(TextComponent.fromLegacyText(Config.KICK_REASON));
                event.setCancelled(true);
                notifyStaff(playerIp, "§c[AntiVPN] " + playerName + " detected as a proxy: " + playerIp + " " + (e == ProxyBlockedException.PROXY_LIST ? "(ProxyList)" : "(IPAPI)"), e.getCheckResult());
            } catch (HostingBlockedException e) {
                event.setCancelReason(TextComponent.fromLegacyText(Config.KICK_REASON));
                event.setCancelled(true);
                notifyStaff(playerIp, "§c[AntiVPN] " + playerName + " detected as a VPN: " + playerIp, e.getCheckResult());
            } catch (ManuallyBlockedException e) {
                event.setCancelReason(TextComponent.fromLegacyText(Config.KICK_REASON));
                event.setCancelled(true);
                notifyStaff(playerIp, "§c[AntiVPN] " + playerName + " is manually blocked: " + playerIp, e.getCheckResult());
            }
        } catch (CheckErrorException e) {
            if (Config.IpApi.FORCE_CHECK_ENABLED) {
                event.setCancelReason(TextComponent.fromLegacyText(Config.IpApi.FORCE_CHECK_KICK_REASON));
                event.setCancelled(true);
            }
        } finally {
            event.completeIntent(plugin);
        }
    }

    private void notifyStaff(@NotNull String playerIp, @NotNull String text, @Nullable CheckResult result) {
        ComponentBuilder cb = new ComponentBuilder();
        cb.append(TextComponent.fromLegacyText(text));
        cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, result == null ? null : new Text(result.getJson().toString())));
        cb.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, playerIp));

        BaseComponent[] message = cb.create();

        ProxyServer.getInstance().getConsole().sendMessage(message);
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (!player.hasPermission("antivpn.kick.notify")) continue;
            player.sendMessage(message);
        }
    }
}
