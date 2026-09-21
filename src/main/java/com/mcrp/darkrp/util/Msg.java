package com.mcrp.darkrp.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * Central helper for sending consistently-formatted chat messages.
 */
public final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final String PREFIX = "<dark_gray>[<gold>DarkRP</gold>]</dark_gray> ";

    private Msg() {
    }

    public static Component parse(String miniMessage) {
        return MM.deserialize(miniMessage);
    }

    /** Converts a MiniMessage string to a legacy '§'-coded string, for APIs that don't accept Components. */
    public static String toLegacy(String miniMessage) {
        return LEGACY.serialize(parse(miniMessage));
    }

    public static void send(CommandSender to, String miniMessage) {
        to.sendMessage(MM.deserialize(PREFIX + miniMessage));
    }

    public static void raw(CommandSender to, String miniMessage) {
        to.sendMessage(MM.deserialize(miniMessage));
    }

    public static void error(CommandSender to, String miniMessage) {
        to.sendMessage(MM.deserialize(PREFIX + "<red>" + miniMessage + "</red>"));
    }

    public static void success(CommandSender to, String miniMessage) {
        to.sendMessage(MM.deserialize(PREFIX + "<green>" + miniMessage + "</green>"));
    }
}
