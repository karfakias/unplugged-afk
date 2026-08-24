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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                        .executes(ctx -> this.setUnpluggedAfk(ctx, "", ""))
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

	private int setUnpluggedAfk(CommandContext<CommandSourceStack> context, String duration, String reason)
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

		int time = parseDurationMinutes(duration);
		int maximumTime = ConfigWrap.unplugged().maximumUnpluggedTimeout;
		if (!duration.isEmpty() && time < 0)
		{
			String msg = "§cInvalid duration. Use 1h30m30s, 90m, 30s, or 60.§r";
			//#if MC >= 1.20.1
			//$$ context.getSource().sendSuccess(() -> InitWrap.text().formatTextSafe(msg), false);
			//#else
			context.getSource().sendSuccess(InitWrap.text().formatTextSafe(msg), false);
			//#endif
			return 0;
		}
		if (time < 0)
		{
			time = duration.isEmpty() && maximumTime > 0
				? maximumTime
				: ConfigWrap.unplugged().defaultUnpluggedTimeout;
			if (time < 0) time = 129600;
		}
		if (maximumTime > 0 && time > maximumTime)
		{
			String msg = "§cYou can only AFK for " + formatDuration(maximumTime) + ".§r";
			//#if MC >= 1.20.1
			//$$ context.getSource().sendSuccess(() -> InitWrap.text().formatTextSafe(msg), false);
			//#else
			context.getSource().sendSuccess(InitWrap.text().formatTextSafe(msg), false);
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

        final int finalTime = time;
        final String finalReason = reason;

        server.execute(() -> UnpluggedServerPlayer.createFromPlayer(server, player, finalTime, finalReason));

        UnpluggedAfk.debugLog("setUnpluggedAfk: player: ['{}'/{}] // T: {}m, R: '{}'", ProfileWrap.name(profile), ProfileWrap.id(profile), time, reason);
		return 1;
	}

	private static int parseDurationMinutes(String input)
	{
		if (input == null || input.isEmpty()) return -1;
		if (input.matches("\\d+"))
		{
			try { return Math.toIntExact(Long.parseLong(input)); }
			catch (NumberFormatException | ArithmeticException error) { return -1; }
		}

		Matcher matcher = DURATION_PART.matcher(input);
		long seconds = 0L;
		int end = 0;
		boolean found = false;
		while (matcher.find())
		{
			if (matcher.start() != end) return -1;
			long amount;
			try { amount = Long.parseLong(matcher.group(1)); }
			catch (NumberFormatException error) { return -1; }
			long multiplier = switch (matcher.group(2).toLowerCase(Locale.ROOT))
			{
				case "h" -> 3600L;
				case "m" -> 60L;
				default -> 1L;
			};
			try { seconds = Math.addExact(seconds, Math.multiplyExact(amount, multiplier)); }
			catch (ArithmeticException error) { return -1; }
			end = matcher.end();
			found = true;
		}
		if (!found || end != input.length() || seconds < 1L) return -1;
		long minutes = (seconds + 59L) / 60L;
		return minutes > Integer.MAX_VALUE ? -1 : (int) minutes;
	}

	private static String formatDuration(int minutes)
	{
		if (minutes % 60 == 0) return (minutes / 60) + " hours";
		return minutes + " minutes";
	}
}
