package ru.violence.antivpn.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import ru.violence.antivpn.AntiVPNPlugin;
import ru.violence.antivpn.checker.CheckResult;
import ru.violence.antivpn.checker.FieldType;
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
            case "block": {
                handleBlockCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            }
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

    private void handleBlockCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextComponent.fromLegacyText("§cUsage: /antivpn block <type> <value>"));
            return;
        }

        FieldType type = FieldType.fromString(args[0]);
        String value = Utils.joinArgs(args, 1);

        if (type == null) {
            sender.sendMessage(TextComponent.fromLegacyText("§cUnknown type."));
            return;
        }

        boolean isEnabled = plugin.getIpChecker().toggleBlock(type, value);

        if (isEnabled) {
            sender.sendMessage(TextComponent.fromLegacyText("§aBlock for \"" + type + "\" \"" + value + "\" has been enabled."));
        } else {
            sender.sendMessage(TextComponent.fromLegacyText("§cBlock for \"" + type + "\" \"" + value + "\" has been disabled."));
        }
    }

    private void handleBypassCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextComponent.fromLegacyText("§cUsage: /antivpn bypass <type> <value>"));
            return;
        }

        FieldType type = FieldType.fromString(args[0]);
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
        if (args.length == 0 || args[0].isEmpty()) {
            sender.sendMessage(TextComponent.fromLegacyText("§cUsage: /antivpn check <ip>"));
            return;
        }

        String ip = args[0];

        try {
            CheckResult result = plugin.getIpChecker().check(ip);
            sender.sendMessage(TextComponent.fromLegacyText("§aResult: " + result.getJson().toString()));

            String s;

            s = "§aBlocks: ";
            s += FieldType.ISP.toKey() + "=" + plugin.getIpChecker().isBlocked(FieldType.ISP, result.getIsp()) + ", ";
            s += FieldType.ORG.toKey() + "=" + plugin.getIpChecker().isBlocked(FieldType.ORG, result.getOrg()) + ", ";
            s += FieldType.AS.toKey() + "=" + plugin.getIpChecker().isBlocked(FieldType.AS, result.getAs()) + ", ";
            s += FieldType.ASNAME.toKey() + "=" + plugin.getIpChecker().isBlocked(FieldType.ASNAME, result.getAsname());
            sender.sendMessage(TextComponent.fromLegacyText(s));

            s = "§aBypasses: ";
            s += FieldType.ISP.toKey() + "=" + plugin.getIpChecker().isBypassed(FieldType.ISP, result.getIsp()) + ", ";
            s += FieldType.ORG.toKey() + "=" + plugin.getIpChecker().isBypassed(FieldType.ORG, result.getOrg()) + ", ";
            s += FieldType.AS.toKey() + "=" + plugin.getIpChecker().isBypassed(FieldType.AS, result.getAs()) + ", ";
            s += FieldType.ASNAME.toKey() + "=" + plugin.getIpChecker().isBypassed(FieldType.ASNAME, result.getAsname());
            sender.sendMessage(TextComponent.fromLegacyText(s));
        } catch (Exception e) {
            sender.sendMessage(TextComponent.fromLegacyText("§cError: " + e.getMessage()));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(TextComponent.fromLegacyText("§cUsage: /antivpn <block|bypass|check>"));
    }
}
