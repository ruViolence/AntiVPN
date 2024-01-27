package ru.violence.antivpn.velocity.listener;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import ru.violence.antivpn.common.checker.CheckResult;
import ru.violence.antivpn.common.checker.FieldType;
import ru.violence.antivpn.common.checker.IPChecker;
import ru.violence.antivpn.velocity.AntiVPNPlugin;

import java.util.concurrent.TimeUnit;

public class PreLoginListener {
    private final AntiVPNPlugin plugin;

    public PreLoginListener(AntiVPNPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event, Continuation continuation) {
        try {
            if (!event.getResult().isAllowed()) return;
        
            IPChecker checker = plugin.getIpChecker();

            String playerName = event.getUsername();
            if (checker.isBypassed(FieldType.PLAYER_NAME, playerName)) return;

            String playerIp = event.getConnection().getRemoteAddress().getAddress().getHostAddress();

            CheckResult result = checker.check(playerIp).get(plugin.getResultAwait(), TimeUnit.MILLISECONDS);

            if (checker.isBypassed(FieldType.ISP, result.getIsp())
                || checker.isBypassed(FieldType.ORG, result.getOrg())
                || checker.isBypassed(FieldType.AS, result.getAs())
                || checker.isBypassed(FieldType.ASNAME, result.getAsname())) {
                return;
            }

            boolean isDenied = result.isProxy();

            if (!isDenied && plugin.isDenyHosting() && result.isHosting()) {
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
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(MiniMessage.miniMessage().deserialize(plugin.getKickReason())));
                plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("[AntiVPN] " + playerName + " detected as a proxy: " + playerIp).color(NamedTextColor.RED));
                notifyStaff(playerName, playerIp, result);
                return;
            }

            if (plugin.isCountryBlockerEnabled()) {
                boolean contains = plugin.getCountryBlockerCountries().contains(result.getCountryCode());
                boolean isBlockedCountry = plugin.isCountryBlockerWhitelist() != contains;

                if (isBlockedCountry) {
                    event.setResult(PreLoginEvent.PreLoginComponentResult.denied(MiniMessage.miniMessage().deserialize(plugin.getCountryBlockerKickReason())));
                    plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("[AntiVPN] " + playerName + " connected from the blocked country: " + result.getCountryCode()).color(NamedTextColor.RED));
                }
            }
        } catch (Exception e) {
            if (plugin.isForceCheckEnabled()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(MiniMessage.miniMessage().deserialize(plugin.getForceCheckKickReason())));
            }
        } finally {
            continuation.resume();
        }
    }

    private void notifyStaff(String playerName, String playerIp, CheckResult result) {
        Component text = null;

        for (Player player : plugin.getProxy().getAllPlayers()) {
            if (!player.hasPermission("antivpn.kick.notify")) continue;

            if (text == null) {
                text = Component.text("[AntiVPN] " + playerName + " tried to join the server, but is disallowed.")
                        .color(NamedTextColor.RED)
                        .hoverEvent(Component.text(result.getJson().toString()))
                        .clickEvent(ClickEvent.suggestCommand(playerIp));
            }

            player.sendMessage(text);
        }
    }
}
