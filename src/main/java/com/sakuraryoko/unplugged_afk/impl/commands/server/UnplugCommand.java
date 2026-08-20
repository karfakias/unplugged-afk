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

import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//#if MC >= 1.21.10
//$$ import net.minecraft.server.players.NameAndId;
//#endif

import com.sakuraryoko.unplugged_afk.impl.Reference;
import com.sakuraryoko.unplugged_afk.impl.UnpluggedAfk;
import com.sakuraryoko.unplugged_afk.impl.commands.PermsWrap;
import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.impl.modinit.InitWrap;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.UnpluggedServerPlayer;
import com.sakuraryoko.corelib.api.commands.IServerCommand;
import com.sakuraryoko.unplugged_afk.impl.player.wrap.ProfileWrap;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApiStatus.Internal
public class UnplugCommand implements IServerCommand
{
	private static final Pattern DURATION_PART = Pattern.compile("(\\d+)([hms])", Pattern.CASE_INSENSITIVE);
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment)
    {
        dispatcher.register(
                literal(this.getName())
                        .requires(PermsWrap.check(this.getNode(), ConfigWrap.cmdOpt().unplugCommandPermissions))
                        .executes(ctx -> this.setUnpluggedAfk(ctx, null, ""))
                        .then(argument("duration", StringArgumentType.word())
                                      .requires(PermsWrap.check(this.getNode(), ConfigWrap.cmdOpt().unplugCommandPermissions))
                                      .executes(ctx -> this.setUnpluggedAfk(ctx, StringArgumentType.getString(ctx, "duration"), ""))
                                      .then(argument("reason", StringArgumentType.greedyString())
                                                    .requires(PermsWrap.check(this.getNode(), ConfigWrap.cmdOpt().unplugCommandPermissions))
                                                    .executes(ctx -> this.setUnpluggedAfk(ctx, StringArgumentType.getString(ctx, "duration"), StringArgumentType.getString(ctx, "reason")))
                                      )
                        )
        );
    }

    @Override
    public String getName()
    {
        return "unplug";
    }

    @Override
    public String getModId()
    {
        return Reference.MOD_ID;
    }

    private int setUnpluggedAfk(CommandContext<CommandSourceStack> context, String durationString, String reason)
    {
        CommandSourceStack src = context.getSource();
        if (src.getPlayer() == null) { return 0; }

        if (!ConfigWrap.mainOpt().unpluggedAfkEnabled)
        {
            String msg = "§c/"+this.getName()+" Command is not enabled§r";
            //#if MC >= 1.20.1
            //$$ context.getSource().sendSuccess(() -> InitWrap.text().formatTextSafe(msg), false);
            //#else
            context.getSource().sendSuccess(InitWrap.text().formatTextSafe(msg), false);
            //#endif
            return 1;
        }

        MinecraftServer server = src.getServer();
        ServerPlayer player = src.getPlayer();
        GameProfile profile = player.getGameProfile();

        //#if MC >= 1.21.10
        //$$ if (server.isSingleplayerOwner(new NameAndId(profile)))
        //#else
        if (server.isSingleplayerOwner(profile))
        //#endif
        {
            String msg = "§cCan't use unplugged as the single player server owner§r";
            //#if MC >= 1.20.1
            //$$ context.getSource().sendSuccess(() -> InitWrap.text().formatTextSafe(msg), false);
            //#else
            context.getSource().sendSuccess(InitWrap.text().formatTextSafe(msg), false);
            //#endif
            return 1;
        }

        long timeout;
        int time;
        if (durationString == null)
        {
            time = ConfigWrap.unplugged().defaultUnpluggedTimeout;
            if (time <= 0) { time = 360; }
            try { timeout = Math.multiplyExact(Math.multiplyExact((long) time, 60L), 1000L); }
            catch (ArithmeticException ex) { return invalidDuration(context); }
        }
        else
        {
            try { timeout = parseDurationMillis(durationString); }
            catch (IllegalArgumentException ex) { return invalidDuration(context); }
            time = (int) Math.max(1L, timeout / 60_000L + (timeout % 60_000L == 0L ? 0L : 1L));
        }

        long maximumTimeout;
        try { maximumTimeout = Math.multiplyExact(Math.multiplyExact((long) ConfigWrap.unplugged().maximumUnpluggedTimeout, 60L), 1000L); }
        catch (ArithmeticException ex) { maximumTimeout = 0L; }
        if (maximumTimeout <= 0L || timeout > maximumTimeout)
        {
			String message = "\u00a7cYou cannot use /unplug for more than "
					+ formatDuration(ConfigWrap.unplugged().maximumUnpluggedTimeout)
					+ ".\u00a7r";
            //#if MC >= 1.20.1
            //$$ context.getSource().sendSuccess(() -> InitWrap.text().formatTextSafe(message), false);
            //#else
            context.getSource().sendSuccess(InitWrap.text().formatTextSafe(message), false);
            //#endif
            return 0;
        }
        if (reason == null || reason.isEmpty())
        {
            reason = ConfigWrap.mess().defaultUnpluggedReason;

            if (reason == null || reason.isEmpty())
            {
                reason = "";
            }
        }

        if (UnpluggedServerPlayer.createFromPlayer(server, player, time, timeout, reason) == null)
        {
            UnpluggedAfk.LOGGER.error("Error creating Unplugged Player from: {}", player.getName().getString());
            return 0;
        }

        UnpluggedAfk.debugLog("setUnpluggedAfk: player: ['{}'/{}] // T: {}m, R: '{}'", ProfileWrap.name(profile), ProfileWrap.id(profile), time, reason);
        return 1;
    }

    private int invalidDuration(CommandContext<CommandSourceStack> context)
    {
        final String message = "\u00a7cInvalid duration. Use values such as 10s, 60m, 1h30m, or 60.\u00a7r";
        //#if MC >= 1.20.1
        //$$ context.getSource().sendSuccess(() -> InitWrap.text().formatTextSafe(message), false);
        //#else
        context.getSource().sendSuccess(InitWrap.text().formatTextSafe(message), false);
        //#endif
        return 0;
    }

    private static String formatDuration(int minutes)
    {
        if (minutes % 60 == 0)
        {
            int hours = minutes / 60;
            return hours + (hours == 1 ? " hour" : " hours");
        }

        if (minutes > 60)
        {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            return hours + (hours == 1 ? " hour " : " hours ")
                    + remainingMinutes + (remainingMinutes == 1 ? " minute" : " minutes");
        }

        return minutes + (minutes == 1 ? " minute" : " minutes");
    }

    private static long parseDurationMillis(String input)
    {
        String value = input.toLowerCase(Locale.ROOT);
        if (value.matches("\\d+"))
        {
            try { return Math.multiplyExact(Math.multiplyExact(Long.parseLong(value), 60L), 1000L); }
            catch (ArithmeticException | NumberFormatException ex) { throw new IllegalArgumentException(); }
        }

        Matcher matcher = DURATION_PART.matcher(value);
        long seconds = 0L;
        int end = 0;
        boolean found = false;
        while (matcher.find())
        {
            if (matcher.start() != end) { throw new IllegalArgumentException(); }
            long amount;
            try { amount = Long.parseLong(matcher.group(1)); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException(); }
            long unit = switch (matcher.group(2).charAt(0)) { case 'h' -> 3600L; case 'm' -> 60L; default -> 1L; };
            try { seconds = Math.addExact(seconds, Math.multiplyExact(amount, unit)); }
            catch (ArithmeticException ex) { throw new IllegalArgumentException(); }
            end = matcher.end();
            found = true;
        }
        if (!found || end != value.length() || seconds <= 0L) { throw new IllegalArgumentException(); }
        try { return Math.multiplyExact(seconds, 1000L); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException(); }
    }
}
