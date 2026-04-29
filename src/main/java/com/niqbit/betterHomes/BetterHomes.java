package com.niqbit.betterHomes;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

import com.niqbit.betterHomes.databases.PlayerHomes;


@SuppressWarnings("UnstableApiUsage")
public class BetterHomes extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PlayerHomes playerHomes = new PlayerHomes(this);
        playerHomes.connect();

        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                    Commands.literal("home")
                            .requires(source -> source.getSender().hasPermission("betterhomes.user.home"))
                            .executes(ctx -> handleHome(ctx, "home"))
                            .then(
                                    Commands.argument("homename", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                if (ctx.getSource().getSender() instanceof Player player) {
                                                    List<String> homes = PlayerHomes.getHomes(player.getUniqueId());
                                                    for (String homeName : homes) {
                                                        builder.suggest(homeName);
                                                    }
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                String homeName = StringArgumentType.getString(ctx, "homename");
                                                return handleHome(ctx, homeName);
                                            })
                            )
                            .build()
            );
            commands.register(
                    Commands.literal("sethome")
                            .requires(source -> source.getSender().hasPermission("betterhomes.user.sethome"))
                            .executes(ctx -> { return handleHomeCreation(ctx, "home"); })
                            .then(
                                    Commands.argument("homename", StringArgumentType.word())
                                            .executes(ctx ->  {
                                                String homeName = StringArgumentType.getString(ctx, "homename");
                                                return handleHomeCreation(ctx, homeName);
                                            })
                            )
                            .build()
            );
            commands.register(
                    Commands.literal("delhome")
                            .requires(source -> source.getSender().hasPermission("betterhomes.user.delhome"))
                            .then(
                                    Commands.argument("homename", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                if (ctx.getSource().getSender() instanceof Player player) {
                                                    List<String> homes = PlayerHomes.getHomes(player.getUniqueId());
                                                    for (String homeName : homes) {
                                                        builder.suggest(homeName);
                                                    }
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                String homeName = StringArgumentType.getString(ctx, "homename");
                                                return deleteHome(ctx, homeName);
                                            })
                            )
                            .build()
            );
        });
    }

    private int handleHomeCreation(CommandContext<CommandSourceStack> ctx, String homeName) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("Only players can use this.");
            return 0;
        }

        int homeCount = PlayerHomes.countHomes(player.getUniqueId());
        int maxHomes = getConfig().getInt("max-homes");
        if (homeCount >= maxHomes) {
            player.sendMessage(
                    Component.text("You reached your Homes limit of " + maxHomes + "! ", NamedTextColor.RED)
                            .append(Component.text("Try deleting a home using ", NamedTextColor.GRAY))
                            .append(Component.text("/delhome <homename>", NamedTextColor.WHITE))
                                .clickEvent(ClickEvent.suggestCommand("/delhome "))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to auto-fill command")))
            );
            return 0;
        }

        if (PlayerHomes.homeExists(player.getUniqueId(), homeName)) {
            int delStatus = PlayerHomes.deleteHome(player.getUniqueId(), homeName);
            if (delStatus != 0 && delStatus != 1) {
                player.sendMessage(Component.text("Error overwriting home. Please report this", NamedTextColor.RED));
                return 0;
            }
        }

        int status = PlayerHomes.createHome(player.getUniqueId(), homeName, player.getWorld(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());

        if (status < 0) {
            player.sendMessage(Component.text("Something went wrong during the creating of your home. Please report this", NamedTextColor.RED));
            return 0;
        }

        player.sendMessage(
                Component.text("Sucessfully created " + homeName, NamedTextColor.GREEN)
        );

        return 1;
    }

    private int handleHome(CommandContext<CommandSourceStack> ctx, String homeName) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("Only players can use this.");
            return 0;
        }

        Map<String, Object> home = PlayerHomes.getHome(player.getUniqueId(), homeName);

        if (home != null) {
            double x = (Double) home.get("x");
            double y = (Double) home.get("y");
            double z = (Double) home.get("z");
            float yaw = (Float) home.get("yaw");
            float pitch = (Float) home.get("pitch");
            String world = (String) home.get("world");

            boolean isCrossAllowed = getConfig().getBoolean("allow-cross-world-teleportation");

            if(!isCrossAllowed) {
                if (!player.getWorld().getName().equals(world)) {
                        player.sendMessage(Component.text("You need to be in the same world to teleport to your home.", NamedTextColor.RED));
                        return 0;
                }
            }

            Location location = new Location(
                    Bukkit.getWorld(world),
                    x, y, z, yaw, pitch
            );
            player.teleport(location);

            player.sendMessage(Component.text("Teleported to " + homeName, NamedTextColor.GREEN));
            return 1;
        } else {
            player.sendMessage(
                    Component.text(homeName + " does not exist.", NamedTextColor.RED)
                            .append(Component.text(" Please create one first using ", NamedTextColor.GRAY))
                            .append(Component.text("/sethome " + homeName, NamedTextColor.WHITE)
                                    .clickEvent(ClickEvent.suggestCommand("/sethome " + homeName))
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to auto-fill command"))))
            );
            return 0;
        }
    }

    public static int deleteHome(CommandContext<CommandSourceStack> ctx, String homeName) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("Only players can use this.");
            return 0;
        }

        if (!PlayerHomes.homeExists(player.getUniqueId(), homeName)) {
            player.sendMessage(
                    Component.text(homeName + " does not exist.", NamedTextColor.RED)
                            .append(Component.text(" Please create one first using ", NamedTextColor.GRAY))
                            .append(Component.text("/sethome " + homeName, NamedTextColor.WHITE)
                                    .clickEvent(ClickEvent.suggestCommand("/sethome " + homeName))
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to auto-fill command"))))
            );
            return 0;
        }

        int delResult = PlayerHomes.deleteHome(player.getUniqueId(), homeName);
        if (delResult == 1) {
            player.sendMessage(Component.text("Home deleted!", NamedTextColor.GREEN));
            return 1;
        } else if (delResult == -1) {
            player.sendMessage(Component.text("An error occurred while trying to delete Home. Please report this", NamedTextColor.RED));
            return 0;
        }

        return 0;

    }
}