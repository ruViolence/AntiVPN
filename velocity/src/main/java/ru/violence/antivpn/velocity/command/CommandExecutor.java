package ru.violence.antivpn.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ru.violence.antivpn.common.checker.CheckResult;
import ru.violence.antivpn.common.checker.FieldType;
import ru.violence.antivpn.common.util.Utils;
import ru.violence.antivpn.velocity.AntiVPNPlugin;

import java.util.Arrays;
import java.util.Locale;

public class CommandExecutor implements SimpleCommand {
    private final AntiVPNPlugin plugin;

    public CommandExecutor(AntiVPNPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

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
            case "recheck": {
                handleRecheckCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            }
            case "expire": {
                handleExpireCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            }
            case "reload": {
                handleReloadCommand(sender);
                break;
            }
            default: {
                sendHelp(sender);
            }
        }
    }

    private void handleBlockCommand(CommandSource sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /antivpn block <type> <value>").color(NamedTextColor.RED));
            return;
        }

        FieldType type = FieldType.fromString(args[0]);
        String value = Utils.joinArgs(args, 1);

        if (type == null) {
            sender.sendMessage(Component.text("Unknown type.").color(NamedTextColor.RED));
            return;
        }

        boolean isEnabled = plugin.getIpChecker().toggleBlock(type, value);

        if (isEnabled) {
            sender.sendMessage(Component.text("Block for \"" + type + "\" \"" + value + "\" has been enabled.").color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Block for \"" + type + "\" \"" + value + "\" has been disabled.").color(NamedTextColor.GREEN));
        }
    }

    private void handleBypassCommand(CommandSource sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /antivpn bypass <type> <value>").color(NamedTextColor.RED));
            return;
        }

        FieldType type = FieldType.fromString(args[0]);
        String value = Utils.joinArgs(args, 1);

        if (type == null) {
            sender.sendMessage(Component.text("Unknown type.").color(NamedTextColor.RED));
            return;
        }

        boolean isEnabled = plugin.getIpChecker().toggleBypass(type, value);

        if (isEnabled) {
            sender.sendMessage(Component.text("Bypass for \"" + type + "\" \"" + value + "\" has been enabled.").color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Bypass for \"" + type + "\" \"" + value + "\" has been disabled.").color(NamedTextColor.GREEN));
        }
    }

    private void handleCheckCommand(CommandSource sender, String[] args) {
        if (args.length == 0 || args[0].isEmpty()) {
            sender.sendMessage(Component.text("Usage: /antivpn check <ip>").color(NamedTextColor.RED));
            return;
        }

        String ip = args[0];

        new Thread(() -> {
            try {
                CheckResult result = plugin.getIpChecker().check(ip).get();
                sender.sendMessage(Component.text("Result: " + result.getJson().toString()).color(NamedTextColor.GREEN));

                String s;

                s = "Blocks: ";
                s += FieldType.ISP.toKey() + "=" + plugin.getIpChecker().isBlocked(FieldType.ISP, result.getIsp()) + ", ";
                s += FieldType.ORG.toKey() + "=" + plugin.getIpChecker().isBlocked(FieldType.ORG, result.getOrg()) + ", ";
                s += FieldType.AS.toKey() + "=" + plugin.getIpChecker().isBlocked(FieldType.AS, result.getAs()) + ", ";
                s += FieldType.ASNAME.toKey() + "=" + plugin.getIpChecker().isBlocked(FieldType.ASNAME, result.getAsname());
                sender.sendMessage(Component.text(s).color(NamedTextColor.GREEN));

                s = "Bypasses: ";
                s += FieldType.ISP.toKey() + "=" + plugin.getIpChecker().isBypassed(FieldType.ISP, result.getIsp()) + ", ";
                s += FieldType.ORG.toKey() + "=" + plugin.getIpChecker().isBypassed(FieldType.ORG, result.getOrg()) + ", ";
                s += FieldType.AS.toKey() + "=" + plugin.getIpChecker().isBypassed(FieldType.AS, result.getAs()) + ", ";
                s += FieldType.ASNAME.toKey() + "=" + plugin.getIpChecker().isBypassed(FieldType.ASNAME, result.getAsname());
                sender.sendMessage(Component.text(s).color(NamedTextColor.GREEN));
            } catch (Exception e) {
                sender.sendMessage(Component.text("Error: " + e.getMessage()).color(NamedTextColor.RED));
            }
        }).start();
    }

    private void handleRecheckCommand(CommandSource sender, String[] args) {
        if (args.length == 0 || args[0].isEmpty()) {
            sender.sendMessage(Component.text("Usage: /antivpn recheck <ip>").color(NamedTextColor.RED));
            return;
        }

        String ip = args[0];

        new Thread(() -> {
            try {
                plugin.getIpChecker().removeFromDatabaseCache(ip);
            } catch (Exception e) {
                sender.sendMessage(Component.text("Error: " + e.getMessage()).color(NamedTextColor.RED));
            }

            handleCheckCommand(sender, args);
        }).start();
    }

    private void handleExpireCommand(CommandSource sender, String[] args) {
        if (args.length == 0 || args[0].isEmpty()) {
            sender.sendMessage(Component.text("Usage: /antivpn expire <ip>").color(NamedTextColor.RED));
            return;
        }

        String ip = args[0];

        new Thread(() -> {
            try {
                boolean isDeleted = plugin.getIpChecker().removeFromDatabaseCache(ip);

                if (isDeleted) {
                    sender.sendMessage(Component.text("Successfully expired.").color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Nothing to expire.").color(NamedTextColor.RED));
                }
            } catch (Exception e) {
                sender.sendMessage(Component.text("Error: " + e.getMessage()).color(NamedTextColor.RED));
            }
        }).start();
    }

    private void handleReloadCommand(CommandSource sender) {
        plugin.reloadConfig();
        sender.sendMessage(Component.text("Config reloaded.").color(NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("antivpn.command.use");
    }

    private void sendHelp(CommandSource sender) {
        sender.sendMessage(Component.text("Usage: /antivpn <block|bypass|check|recheck|expire|reload>").color(NamedTextColor.RED));
    }
}
