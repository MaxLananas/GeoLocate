package dev.geolocate.util;

import dev.geolocate.GeoLocate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final GeoLocate plugin;

    public MessageUtil(GeoLocate plugin) {
        this.plugin = plugin;
    }

    public void send(CommandSender sender, String messageKey, TagResolver... resolvers) {
        String raw = plugin.getGeoConfig().getPrefix()
                + plugin.getGeoConfig().getMessage(messageKey);
        sender.sendMessage(MM.deserialize(raw, resolvers));
    }

    public void sendRaw(CommandSender sender, String miniMessageString) {
        sender.sendMessage(MM.deserialize(miniMessageString));
    }

    public static Component parse(String miniMessage) {
        return MM.deserialize(miniMessage);
    }

    public static Component parse(String miniMessage, TagResolver... resolvers) {
        return MM.deserialize(miniMessage, resolvers);
    }

    public static TagResolver placeholder(String key, String value) {
        return Placeholder.parsed(key, value);
    }

    public static TagResolver componentPlaceholder(String key, Component value) {
        return Placeholder.component(key, value);
    }
}
