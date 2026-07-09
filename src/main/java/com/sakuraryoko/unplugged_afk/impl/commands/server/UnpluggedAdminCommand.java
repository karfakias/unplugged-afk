/*
 * This file is part of the Unplugged-AFK project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Sakura-Ryoko and contributors
 *
 * Unplugged-AFK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unplugged-AFK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Unplugged-AFK.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sakuraryoko.unplugged_afk.impl.commands.server;

import java.util.List;
import java.util.UUID;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//#if MC >= 26.2
//$$ import net.minecraft.world.entity.EntityTypes;
//#endif
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.EntityType;

import com.sakuraryoko.corelib.api.commands.IServerCommand;
import com.sakuraryoko.corelib.api.modinit.ModInitData;
import com.sakuraryoko.corelib.impl.config.ConfigManager;
import com.sakuraryoko.unplugged_afk.impl.Reference;
import com.sakuraryoko.unplugged_afk.impl.UnpluggedAfk;
import com.sakuraryoko.unplugged_afk.impl.commands.PermsWrap;
import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.impl.config.UnpluggedConfigHandler;
import com.sakuraryoko.unplugged_afk.impl.config.data.options.PlayerOptions;
import com.sakuraryoko.unplugged_afk.impl.modinit.InitWrap;
import com.sakuraryoko.unplugged_afk.impl.modinit.UnpluggedInit;
import com.sakuraryoko.unplugged_afk.impl.player.*;
import com.sakuraryoko.unplugged_afk.impl.player.interfaces.IPlayerListInvoker;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.UnpluggedServerPlayer;
import com.sakuraryoko.unplugged_afk.impl.player.state.ProfileWrap;
import com.sakuraryoko.unplugged_afk.impl.player.state.UnpluggedState;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@ApiStatus.Internal
public class UnpluggedAdminCommand implements IServerCommand
{
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment)
    {
        dispatcher.register(
                literal(this.getName())
                        .requires(PermsWrap.check(this.getNode(), ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                        .executes(this::about)
                        .then(literal("save")
                                      .requires(PermsWrap.check(this.getNode()+".save", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                      .executes(this::save)
                        )
                        .then(literal("reload")
                                      .requires(PermsWrap.check(this.getNode()+".reload", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                      .executes(this::reload)
                        )
                        .then(literal("list")
                                      .requires(PermsWrap.check(this.getNode()+".list", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                      .then(literal("players")
                                                    .requires(PermsWrap.check(this.getNode()+".list.players", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                                    .executes(this::listPlayerMap)
                                      )
                                      .then(literal("shadows")
                                                    .requires(PermsWrap.check(this.getNode()+".list.shadows", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                                    .executes(this::listShadowMap)
                                      )
                                      .then(literal("all")
                                                    .requires(PermsWrap.check(this.getNode()+".list.all", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                                    .executes(this::listAll)
                                      )
                        )
                        .then(literal("info")
                                      .requires(PermsWrap.check(this.getNode()+".info", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                      .executes(this::infoPlayer)
                                      .then(argument("player", EntityArgument.player())
                                                    .executes(ctx ->
                                                                      this.infoPlayer(ctx, EntityArgument.getPlayer(ctx, "player"))
                                                    )
                                      )
                        )
                        .then(literal("purge")
                                      .requires(PermsWrap.check(this.getNode()+".purge", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                      .executes(this::purgePlayers)
                        )
                        .then(literal("spawn")
                                      .requires(PermsWrap.check(this.getNode()+".spawn", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                      .then(argument("shadow", StringArgumentType.string())
                                                    .suggests(
                                                            (ctx, builder) ->
                                                                    SharedSuggestionProvider.suggest(
                                                                            PlayerManager.getInstance().getSpawnCommandSuggestions(ctx),
                                                                            builder,
                                                                            ProfileWrap::name,
                                                                            this::formatTooltip
                                                                    )
                                                    )
                                                    .requires(PermsWrap.check(this.getNode()+".spawn", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                                    .executes(ctx ->
                                                              {
                                                                  String result = StringArgumentType.getString(ctx, "shadow");
                                                                  return this.createShadow(ctx, result, -1, "");
                                                              }
                                                    )
                                                    .then(argument("time", IntegerArgumentType.integer(1))
                                                                  .requires(PermsWrap.check(this.getNode()+".spawn", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                                                  .executes(ctx ->
                                                                            {
                                                                                String result = StringArgumentType.getString(ctx, "shadow");
                                                                                return this.createShadow(ctx, result, IntegerArgumentType.getInteger(ctx, "time"), "");
                                                                            }
                                                                  )
                                                                  .then(argument("reason", StringArgumentType.greedyString())
                                                                                .requires(PermsWrap.check(this.getNode()+".spawn", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                                                                .executes(ctx ->
                                                                                          {
                                                                                              String result = StringArgumentType.getString(ctx, "shadow");
                                                                                              return this.createShadow(ctx, result, IntegerArgumentType.getInteger(ctx, "time"), StringArgumentType.getString(ctx, "reason"));
                                                                                          }
                                                                                )
                                                                  )
                                                    )
                                      )
                        )
                        .then(literal("kill")
                                      .requires(PermsWrap.check(this.getNode()+".kill", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                      .then(argument("target", StringArgumentType.string())
                                                    .suggests(
                                                            (ctx, builder) ->
                                                                    SharedSuggestionProvider.suggest(
                                                                            PlayerManager.getInstance().getKillCommandSuggestions(ctx),
                                                                            builder,
                                                                            ProfileWrap::name,
                                                                            this::formatTooltip
                                                                    )
                                                    )
                                                    .requires(PermsWrap.check(this.getNode()+".kill", ConfigWrap.unplugged().unpluggedAdminCommandPermissions))
                                                    .executes(ctx ->
                                                              {
                                                                  String result = StringArgumentType.getString(ctx, "target");
                                                                  return this.killShadow(ctx, result);
                                                              }
                                                    )
                                      )
                        )
        );
    }

    @Override
    public String getName()
    {
        return "unplugged-admin";
    }

    @Override
    public String getModId()
    {
        return Reference.MOD_ID;
    }

    private int about(CommandContext<CommandSourceStack> ctx)
    {
        List<Component> info = UnpluggedInit.getInstance().getVanillaFormatted(ModInitData.ALL_INFO);
        MutableComponent text = Component.literal("");

        for (Component entry : info)
        {
            text.append(entry).append("\n");
        }

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> text, false);
        //#else
        ctx.getSource().sendSuccess(text, false);
        //#endif

        return 1;
    }

    private int save(CommandContext<CommandSourceStack> ctx)
    {
        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText("Saving config!"), false);
        //#else
        ctx.getSource().sendSuccess(InitWrap.text().formatText("Saving config!"), false);
        //#endif

        ConfigManager.getInstance().saveEach(UnpluggedConfigHandler.getInstance());
        String user = ctx.getSource().getTextName();
        UnpluggedAfk.LOGGER.info("{} has saved the configuration.", user);

        return 1;
    }

    private int reload(CommandContext<CommandSourceStack> ctx)
    {
        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText("Reloaded config!"), false);
        //#else
        ctx.getSource().sendSuccess(InitWrap.text().formatText("Reloaded config!"), false);
        //#endif

        ConfigManager.getInstance().reloadEach(UnpluggedConfigHandler.getInstance());
        String user = ctx.getSource().getTextName();
        UnpluggedAfk.LOGGER.info("{} has reloaded the configuration.", user);

        return 1;
    }

    private int listAll(CommandContext<CommandSourceStack> ctx)
    {
        this.listPlayerMap(ctx);
        this.listShadowMap(ctx);

        return 1;
    }

    private int listPlayerMap(CommandContext<CommandSourceStack> ctx)
    {
        ImmutableMap<UUID, PlayerEntry> playerMap = PlayerManager.getInstance().playerMapCopy();
        MutableComponent text = Component.literal("");
        int count = 0;

        text.append(
                InitWrap.text().formatText("§dPlayer Map:")
        );

        for (UUID key : playerMap.keySet())
        {
            PlayerEntry entry = playerMap.get(key);

            if (entry != null)
            {
                text.append(
                        InitWrap.text().formatText(
                                String.format("\n§9[Entry: %02d]", count)
                        )
                ).append(
                        entry.getDebugFormatted()
                );
            }

            count++;
        }

        text.append(
                String.format("\n§6(%d total)§r", count)
        );

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> text, false);
        //#else
        ctx.getSource().sendSuccess(text, false);
        //#endif

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile profile = ctx.getSource().getPlayer().getGameProfile();
            UnpluggedAfk.debugLog("listPlayerMap: by: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }
        else
        {
            UnpluggedAfk.debugLog("listPlayerMap: by: [console/unknown]");
        }

        return 1;
    }

    private int listShadowMap(CommandContext<CommandSourceStack> ctx)
    {
        ImmutableMap<UUID, UnpluggedEntry> map = UnpluggedEntryList.getInstance().shadowMapCopy();
        MutableComponent text = Component.literal("");
        int count = 0;

        text.append(
                InitWrap.text().formatText("\n§dShadow Map:")
        );

        for (UnpluggedEntry entry : map.values())
        {
            text.append(
                    InitWrap.text().formatText(
                            String.format("\n§9[Entry: %02d]", count)
                    )
            ).append(
                    entry.getDebugFormatted()
            );

            count++;
        }

        text.append(
                String.format("\n§6(%d total)§r", count)
        );

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> text, false);
        //#else
        ctx.getSource().sendSuccess(text, false);
        //#endif

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile profile = ctx.getSource().getPlayer().getGameProfile();
            UnpluggedAfk.debugLog("listShadowMap: by: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }
        else
        {
            UnpluggedAfk.debugLog("listShadowMap: by: [console/unknown]");
        }

        return 1;
    }

    private int infoPlayer(CommandContext<CommandSourceStack> ctx)
    {
        try
        {
            return this.infoPlayer(ctx, ctx.getSource().getPlayerOrException());
        }
        catch (CommandSyntaxException err)
        {
            UnpluggedAfk.LOGGER.warn("CMD:infoPlayer: Syntax Error; {}", err.getLocalizedMessage());
            return 0;
        }
    }

    private int infoPlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player)
    {
        MutableComponent text = Component.literal("");

        text.append(
                InitWrap.text().formatText("§9Player Info: ")
        ).append(
                PlayerManager.getInstance().getDebugFormatted(player.getUUID())
        ).append(
                InitWrap.text().formatText("\n§9Shadow Info: ")
        ).append(
                UnpluggedEntryList.getInstance().getDebugFormatted(player.getUUID())
        );

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> text, false);
        //#else
        ctx.getSource().sendSuccess(text, false);
        //#endif

        GameProfile profile = player.getGameProfile();

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile ctxProfile = ctx.getSource().getPlayer().getGameProfile();
            UnpluggedAfk.debugLog("infoPlayer: by: ['{}'/{}] for player: ['{}'/{}]",
                                  ProfileWrap.name(ctxProfile), ProfileWrap.id(ctxProfile),
                                  ProfileWrap.name(profile), ProfileWrap.id(profile)
            );
        }
        else
        {
            UnpluggedAfk.debugLog("infoPlayer: by: [console/unknown] for player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }

        return 1;
    }

    private int purgePlayers(CommandContext<CommandSourceStack> ctx)
    {
        ServerPlayer player = ctx.getSource().getPlayer();
        ImmutableMap<UUID, PlayerEntry> playerMap = PlayerManager.getInstance().playerMapCopy();
        ImmutableMap<UUID, UnpluggedEntry> shadowMap = UnpluggedEntryList.getInstance().shadowMapCopy();
        int count = 0;

        PlayerManager.getInstance().flushToConfig();

        for (UUID uuid : playerMap.keySet())
        {
            if (player != null)
            {
                if (!uuid.equals(player.getUUID()))
                {
                    PlayerManager.getInstance().remove(uuid, true);
                    count++;
                }
            }
            else
            {
                // Via console command, probably.
                PlayerManager.getInstance().remove(uuid, true);
                count++;
            }
        }

        // Resync
        PlayerManager.getInstance().onServerResync(ctx.getSource().getServer(), playerMap, shadowMap);
        playerMap = PlayerManager.getInstance().playerMapCopy();
        shadowMap = UnpluggedEntryList.getInstance().shadowMapCopy();
        String result = String.format("§ePurged: §c%d §eplayers, and then resynced §a%d §ecurrent players, with §6%d shadows§r", count, playerMap.size(), shadowMap.size());

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(result), false);
        //#else
        ctx.getSource().sendSuccess(InitWrap.text().formatText(result), false);
        //#endif

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile profile = ctx.getSource().getPlayer().getGameProfile();
            UnpluggedAfk.debugLog("purgePlayers: by: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }
        else
        {
            UnpluggedAfk.debugLog("purgePlayers: by: [console/unknown]");
        }

        return 1;
    }

    @ApiStatus.Internal
    private Component formatTooltip(GameProfile profile)
    {
        MutableComponent result = Component.literal(ProfileWrap.name(profile));
        //#if MC >= 1.21.5
        //$$ HoverEvent hoverEvent = new HoverEvent.ShowEntity(
        //#else
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_ENTITY,
        //#endif
                                               new HoverEvent.EntityTooltipInfo(this.getEntityTypeWrap(),
                                                                                ProfileWrap.id(profile),
                                                                                Component.literal(ProfileWrap.name(profile))
                                               )
        );
        result.withStyle(style -> style.withHoverEvent(hoverEvent));
        return result;
    }

    @ApiStatus.Internal
    private EntityType<?> getEntityTypeWrap()
    {
        //#if MC >= 26.2
        //$$ return EntityTypes.PLAYER;
        //#else
        return EntityType.PLAYER;
        //#endif
    }

    @ApiStatus.Internal
    private int createShadow(CommandContext<CommandSourceStack> ctx, String result, int time, String reason)
    {
        ImmutableList<GameProfile> list = PlayerManager.getInstance().getSpawnCommandSuggestions(ctx);
        boolean found = false;
        String reply = "";

        if (time < 0)
        {
            time = ConfigWrap.unplugged().defaultUnpluggedTimeout;

            if (time < 0)
            {
                time = 129600;
            }
        }
        if (reason == null || reason.isEmpty())
        {
            reason = ConfigWrap.mess().defaultUnpluggedReason;

            if (reason == null || reason.isEmpty())
            {
                reason = "§rnone";
            }
        }

        for (GameProfile entry : list)
        {
            if (ProfileWrap.name(entry).equals(result))
            {
                try
                {
                    PlayerOptions opts = ConfigWrap.players().stream()
                            .filter(opt -> opt.uuid.equals(ProfileWrap.id(entry)))
                                                   .findFirst()
                                                   .orElseThrow();

                    UnpluggedAfk.debugLog("createShadow: Scheduling Shadow player: ['{}'/{}]", opts.name, opts.uuid.toString());
                    reply = "§eScheduling shadow spawn for: §7"+ result + "§r";
                    opts.state = new UnpluggedState(true, time, (time * 60L) * 1000L, reason);
                    PlayerManager.getInstance().setShadowState(entry, opts.state);
                    PlayerManager.getInstance().flushToConfig();
                    PendingUnpluggedSpawns.INSTANCE.unlock();
                    PendingUnpluggedSpawns.INSTANCE.scheduleSpawn(opts);
                }
                catch (Exception e)
                {
                    reply = "§cException: "+ e.getLocalizedMessage() + "§r";
                }

                found = true;
                break;
            }
        }

        if (!found)
        {
            reply = "§cNo matching player found§r";
        }

        final String finalReply = reply;

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
        //#else
        ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
        //#endif

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile profile = ctx.getSource().getPlayer().getGameProfile();
            UnpluggedAfk.debugLog("createShadow: by: ['{}'/{}] // result: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), finalReply);
        }
        else
        {
            UnpluggedAfk.debugLog("createShadow: by: [console/unknown] // result: {}", finalReply);
        }

        return 1;
    }

    @ApiStatus.Internal
    private int killShadow(CommandContext<CommandSourceStack> ctx, String result)
    {
        ImmutableList<GameProfile> list = PlayerManager.getInstance().getKillCommandSuggestions(ctx);
        boolean found = false;
        String reply = "";

        for (GameProfile entry : list)
        {
            if (ProfileWrap.name(entry).equals(result))
            {
                try
                {
                    MinecraftServer server = ctx.getSource().getServer();
                    PlayerList playerList = server.getPlayerList();
                    List<ServerPlayer> players = playerList.getPlayers();

                    for (ServerPlayer player : players)
                    {
                        if (player.getUUID().equals(ProfileWrap.id(entry)) && player instanceof UnpluggedServerPlayer sp)
                        {
                            UnpluggedAfk.debugLog("killShadow: Killing Shadow player: ['{}'/{}]", ProfileWrap.name(entry), ProfileWrap.id(entry).toString());
                            reply = "§eKilling shadow: §7"+ ProfileWrap.name(entry) + "§r";

                            if (ConfigWrap.mess().hideUnpluggedJoin)
                            {
                                ((IPlayerListInvoker) playerList).unplugged$toggleBroadcastSystemMessage(true);
                            }

                            Component message = Component.literal("Killed");
                            sp.kill(message);
                            playerList.remove(player);
                            ((IPlayerListInvoker) playerList).unplugged$toggleBroadcastSystemMessage(false);

                            break;
                        }
                    }
                }
                catch (Exception e)
                {
                    reply = "§cException: "+ e.getLocalizedMessage() + "§r";
                }

                found = true;
                break;
            }
        }

        if (!found)
        {
            reply = "§cNo matching shadow player found§r";
        }

        final String finalReply = reply;

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
        //#else
        ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
        //#endif

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile profile = ctx.getSource().getPlayer().getGameProfile();
            UnpluggedAfk.debugLog("killShadow: by: ['{}'/{}] // result: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), finalReply);
        }
        else
        {
            UnpluggedAfk.debugLog("killShadow: by: [console/unknown] // result: {}", finalReply);
        }

        return 1;
    }
}
