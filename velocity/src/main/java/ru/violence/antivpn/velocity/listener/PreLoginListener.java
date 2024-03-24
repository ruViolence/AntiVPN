package ru.violence.antivpn.velocity.listener;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.antivpn.common.config.Config;
import ru.violence.antivpn.common.model.CheckResult;
import ru.violence.antivpn.common.model.exception.BypassedException;
import ru.violence.antivpn.common.model.exception.CheckErrorException;
import ru.violence.antivpn.common.model.exception.CountryBlockedException;
import ru.violence.antivpn.common.model.exception.HostingBlockedException;
import ru.violence.antivpn.common.model.exception.ManuallyBlockedException;
import ru.violence.antivpn.common.model.exception.ProxyBlockedException;
import ru.violence.antivpn.velocity.AntiVPNPlugin;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class PreLoginListener {
    private final AntiVPNPlugin plugin;

    public PreLoginListener(AntiVPNPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event, Continuation continuation) {
        try {
            if (!event.getResult().isAllowed()) return;

            String playerName = event.getUsername();
            String playerIp = event.getConnection().getRemoteAddress().getAddress().getHostAddress();

            try {
                plugin.getIpChecker().checkPlayer(playerName, playerIp);
            } catch (BypassedException e) {
                // NOOP
            } catch (CountryBlockedException e) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(miniMessage().deserialize(Config.IpApi.COUNTRY_BLOCKER_KICK_REASON)));
                notifyStaff(playerIp, "[AntiVPN] " + playerName + " connecting from the blocked country: " + e.getCheckResult().getCountryCode(), e.getCheckResult());
            } catch (ProxyBlockedException e) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(miniMessage().deserialize(Config.KICK_REASON)));
                notifyStaff(playerIp, "[AntiVPN] " + playerName + " detected as a proxy: " + playerIp + " " + (e == ProxyBlockedException.PROXY_LIST ? "(ProxyList)" : "(IPAPI)"), e.getCheckResult());
            } catch (HostingBlockedException e) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(miniMessage().deserialize(Config.KICK_REASON)));
                notifyStaff(playerIp, "[AntiVPN] " + playerName + " detected as a VPN: " + playerIp, e.getCheckResult());
            } catch (ManuallyBlockedException e) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(miniMessage().deserialize(Config.KICK_REASON)));
                notifyStaff(playerIp, "[AntiVPN] " + playerName + " is manually blocked: " + playerIp, e.getCheckResult());
            }
        } catch (CheckErrorException e) {
            if (Config.IpApi.FORCE_CHECK_ENABLED) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(miniMessage().deserialize(Config.IpApi.FORCE_CHECK_KICK_REASON)));
            }
        } finally {
            continuation.resume();
        }
    }

    private void notifyStaff(@NotNull String playerIp, @NotNull String text, @Nullable CheckResult result) {
        Component message = Component.text(text)
                .color(NamedTextColor.RED)
                .hoverEvent(result == null ? null : Component.text(result.getJson().toString()))
                .clickEvent(ClickEvent.suggestCommand(playerIp));

        plugin.getProxy().getConsoleCommandSource().sendMessage(message);
        for (Player player : plugin.getProxy().getAllPlayers()) {
            if (!player.hasPermission("antivpn.kick.notify")) continue;
            player.sendMessage(message);
        }
    }
}
