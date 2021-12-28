package ru.violence.antivpn.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import ru.violence.antivpn.AntiVPNPlugin;
import ru.violence.antivpn.checker.BypassType;
import ru.violence.antivpn.checker.CheckResult;
import ru.violence.antivpn.util.Utils;

import java.util.Arrays;
import java.util.Locale;

public class CommandExecutor extends Command {
    private final AntiVPNPlugin plugin;

    public CommandExecutor(AntiVPNPlugin plugin) {
        super("antivpn", "antivpn.command.use");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        switch (args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "") {
            case "bypass": {
                handleBypassCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            }
            case "check": {
                handleCheckCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            }
            default: {
                sendHelp(sender);
            }
        }
    }

    private void handleBypassCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextComponent.fromLegacyText("§cUsage: /antivpn bypass <type> <value>"));
            return;
        }

        BypassType type = BypassType.fromString(args[0]);
        String value = Utils.joinArgs(args, 1);

        if (type == null) {
            sender.sendMessage(TextComponent.fromLegacyText("§cUnknown type."));
            return;
        }

        boolean isEnabled = plugin.getIpChecker().toggleBypass(type, value);

        if (isEnabled) {
            sender.sendMessage(TextComponent.fromLegacyText("§aBypass for \"" + type + "\" \"" + value + "\" has been enabled."));
        } else {
            sender.sendMessage(TextComponent.fromLegacyText("§cBypass for \"" + type + "\" \"" + value + "\" has been disabled."));
        }
    }

    private void handleCheckCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(TextComponent.fromLegacyText("§cUsage: /antivpn check <ip>"));
            return;
        }

        String ip = args[0];

        try {
            CheckResult result = plugin.getIpChecker().check(ip);
            sender.sendMessage(TextComponent.fromLegacyText("§aResult: " + result.getJson().toString()));

            String sb = "§aBypasses: ";
            sb += BypassType.ISP.toKey() + "=" + plugin.getIpChecker().isBypassed(BypassType.ISP, result.getIsp()) + ", ";
            sb += BypassType.ORG.toKey() + "=" + plugin.getIpChecker().isBypassed(BypassType.ORG, result.getOrg()) + ", ";
            sb += BypassType.AS.toKey() + "=" + plugin.getIpChecker().isBypassed(BypassType.AS, result.getAs()) + ", ";
            sb += BypassType.ASNAME.toKey() + "=" + plugin.getIpChecker().isBypassed(BypassType.ASNAME, result.getAsname());
            sender.sendMessage(TextComponent.fromLegacyText(sb));
        } catch (Exception e) {
            sender.sendMessage(TextComponent.fromLegacyText("§cError: " + e.getMessage()));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(TextComponent.fromLegacyText("§cUsage: /antivpn <bypass|check>"));
    }
}
