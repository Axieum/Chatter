package me.axieum.mcmod.chatter.impl.discord.command;

import com.jagrosh.jdautilities.command.CommandBuilder;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.CommandEvent;
import me.axieum.mcmod.chatter.api.event.discord.JDAEvents;
import me.axieum.mcmod.chatter.impl.discord.callback.discord.DiscordCommandListener;
import me.axieum.mcmod.chatter.impl.discord.command.discord.MinecraftCommand;
import me.axieum.mcmod.chatter.impl.discord.command.discord.TPSCommand;
import me.axieum.mcmod.chatter.impl.discord.command.discord.UptimeCommand;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Arrays;

import static me.axieum.mcmod.chatter.impl.discord.ChatterDiscord.CONFIG;
import static me.axieum.mcmod.chatter.impl.discord.ChatterDiscord.LOGGER;

public final class DiscordCommands
{
    /**
     * Builds a JDA command client instance.
     *
     * @return JDA command client instance
     */
    public static CommandClient build()
    {
        try {
            final CommandClientBuilder builder = new CommandClientBuilder();

            // General command configurations
            builder.setPrefix(CONFIG.commands.prefix)
                   .setHelpWord(CONFIG.commands.helpWord)
                   .useHelpBuilder(CONFIG.commands.helpWord != null)
                   .setStatus(CONFIG.bot.status.starting)
                   .setActivity(null);

            // Conditionally apply owner identifiers
            final String[] ownerIds = CONFIG.commands.admins;
            if (ownerIds.length > 0) {
                // Assume the first identifier is the primary owner
                builder.setOwnerId(ownerIds[0]);
                // ..and the rest are co-owners
                if (ownerIds.length > 1)
                    builder.setCoOwnerIds(Arrays.copyOfRange(ownerIds, 1, ownerIds.length));
            } else {
                throw new IllegalArgumentException("At least one command admin must be configured!");
            }

            // Set the command listener
            builder.setListener(new DiscordCommandListener());

            // Conditionally add built-in commands
            if (CONFIG.commands.builtin.uptime.enabled)
                builder.addCommand(new UptimeCommand());

            if (CONFIG.commands.builtin.tps.enabled)
                builder.addCommand(new TPSCommand());

            // Add any custom commands
            Arrays.stream(CONFIG.commands.custom)
                  // Filter out disabled commands
                  .filter(c -> c.enabled)
                  // Build the commands
                  .map(c -> {
                      LOGGER.debug("Registering custom command: '{}{}' -> '{}'", CONFIG.commands.prefix, c.name, c.command);
                      return new CommandBuilder()
                              .setName(c.name)
                              .setAliases(c.aliases)
                              .setArguments(c.usage)
                              .setHelp(c.help)
                              .setHidden(c.hidden)
                              .setRequiredRole(c.role)
                              .setCooldown(c.cooldown)
                              .setCooldownScope(c.cooldownScope)
                              .build(new MinecraftCommand(c.command, c.quiet));
                  })
                  // Register the commands
                  .forEach(builder::addCommand);

            // Build the client
            JDAEvents.BUILD_COMMAND_CLIENT.invoker().onBuildCommandClient(builder);
            return builder.build();
        } catch (Exception e) {
            LOGGER.warn("Unable to prepare the command client: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Replies to the invoker, notifying that the Minecraft server is unavailable.
     *
     * @param event JDA command event
     */
    public static void replyUnavailable(CommandEvent event)
    {
        event.reply(new EmbedBuilder().setColor(0xff8800).setDescription(CONFIG.commands.messages.unavailable).build());
    }
}
