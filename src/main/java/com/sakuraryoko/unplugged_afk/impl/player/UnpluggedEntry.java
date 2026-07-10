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

package com.sakuraryoko.unplugged_afk.impl.player;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import com.mojang.authlib.GameProfile;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.impl.modinit.InitWrap;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.UnpluggedServerPlayer;
import com.sakuraryoko.unplugged_afk.impl.player.state.ProfileWrap;
import com.sakuraryoko.unplugged_afk.impl.player.state.UnpluggedState;
import com.sakuraryoko.corelib.api.time.DurationFormat;
import com.sakuraryoko.corelib.api.time.TimeFormat;

@ApiStatus.Internal
public class UnpluggedEntry
{
	private @Nullable UnpluggedServerPlayer player;
	private final UnpluggedEntryHandler handler;
	private boolean enabled;
	private int time;
	private long startTimeMs;
	private long startTimeEpoch;
	private long timeout;
	private String reason;

	@ApiStatus.Internal
	private UnpluggedEntry()
	{
		this.player = null;
		this.enabled = false;
		this.time = 0;
		this.startTimeMs = -1L;
		this.startTimeEpoch = -1L;
		this.timeout = -1L;
		this.reason = "";
		this.handler = new UnpluggedEntryHandler(this);
	}

	@ApiStatus.Internal
	public static UnpluggedEntry create(@Nonnull UnpluggedServerPlayer player)
	{
		UnpluggedEntry newEntry = new UnpluggedEntry();
		newEntry.setPlayer(player);
		return newEntry;
	}

	@ApiStatus.Internal
	public UnpluggedEntryHandler handler()
	{
		return this.handler;
	}

	@Nullable
	public UnpluggedServerPlayer player()
	{
		return this.player;
	}

	public Component name()
	{
		return this.player != null
		       ? this.player.getName()
		       : Component.empty();
	}

	public @Nullable GameProfile profile()
	{
		return this.player != null
		       ? this.player.getGameProfile()
		       : null;
	}

	public boolean enabled()
	{
		return this.enabled;
	}

	public int timer()
	{
		return this.time;
	}

	public long startTimeEpoch()
	{
		return this.startTimeEpoch;
	}

	public long startTimeMs()
	{
		return this.startTimeMs;
	}

	public long timeout()
	{
		return this.timeout;
	}

	public String reason()
	{
		return this.reason;
	}

	public DurationFormat durationType()
	{
		DurationFormat format = DurationFormat.fromStringStatic(ConfigWrap.mess().duration.option.getName());

		if (format != null)
		{
			return format;
		}

		ConfigWrap.mess().duration.option = DurationFormat.PRETTY;
		return DurationFormat.REGULAR;
	}

	public TimeFormat timeDateType()
	{
		TimeFormat format = TimeFormat.fromStringStatic(ConfigWrap.mess().timeDate.option.getName());

		if (format != null)
		{
			return format;
		}

		ConfigWrap.mess().timeDate.option = TimeFormat.REGULAR;
		return TimeFormat.REGULAR;
	}

	public String startTimeEpochString()
	{
		return this.timeDateType().formatTo(this.startTimeEpoch(), ConfigWrap.mess().duration.customFormat);
	}

	public String durationString()
	{
		return this.durationType().format((Util.getMillis() - this.startTimeMs()), ConfigWrap.mess().duration.customFormat);
	}

    public String timeoutString()
    {
        return this.durationType().format((this.timeout), ConfigWrap.mess().duration.customFormat);
    }

	public String startTimeEpochFormatted()
	{
		if (this.startTimeEpoch > 1L)
		{
			String result = this.startTimeEpochString();
			return "§a" + result + "§r";
		}

		return "§eN/A§r";
	}

	public String durationFormatted()
	{
		if (this.startTimeMs() > 1L)
		{
			String result = this.durationString();
			return "§a" + result + "§r";
		}

		return "§eN/A§r";
	}

	public String timeoutFormatted()
	{
		if (this.timeout > 1L)
		{
			String result = this.timeoutString();
			return "§6" + result + "§r";
		}

		return "§eN/A§r";
	}

	public String reasonFormatted()
	{
		if (this.reason().isEmpty())
		{
			return "§e<empty>";
		}

		return "§e"+this.reason()+"§r";
	}

	@ApiStatus.Internal
	public void setPlayer(@Nonnull UnpluggedServerPlayer player)
	{
		this.player = player;
	}

	@ApiStatus.Internal
	public void updateState(UnpluggedState state)
	{
		this.enabled = state.enabled();
		this.setTimer(state.time());
		this.setTimeout(state.timeout());
		this.setReason(state.reason());

		if (this.startTimeMs() <= 1L)
		{
			this.setStartTimeMs(Util.getMillis());
		}
		if (this.startTimeEpoch() <= 1L)
		{
			this.setStartTimeEpoch(ZonedDateTime.now().toInstant().toEpochMilli());
		}
	}

	@ApiStatus.Internal
	public void clearPlayer()
	{
		this.player = null;
	}

	@ApiStatus.Internal
	public void setTimer(int time)
	{
		this.time = time;
	}

	@ApiStatus.Internal
	public void setStartTimeMs(long time)
	{
		this.startTimeMs = time;
	}

	@ApiStatus.Internal
	public void setStartTimeEpoch(long time)
	{
		this.startTimeEpoch = time;
	}

	@ApiStatus.Internal
	public void setTimeout(long timeout)
    {
        this.timeout = Math.min(Math.max(timeout, 0L), Long.MAX_VALUE);
    }

	@ApiStatus.Internal
	public void setReason(String reason)
	{
		this.reason = reason;
	}

	@ApiStatus.Internal
    public boolean tickShadowTimeout(final long tickDelta)
    {
        this.timeout -= tickDelta;
        return this.timeout > 0L;
    }

	public boolean matches(@Nonnull UnpluggedServerPlayer player)
	{
		if (this.player == null) { return false; }
		return  this.player.getUUID().equals(player.getUUID()) ||
				this.player.equals(player);
	}

	public boolean matches(UUID uuid)
	{
		if (this.player == null) { return false; }
		return  this.player.getUUID().equals(uuid);
	}

	@VisibleForTesting
	public Component debugFormatted()
	{
		MutableComponent text = Component.literal("");

		if (this.player != null)
		{
			text.append(
					InitWrap.text().formatText("§r\n - §7Name: ")
			).append(
					PlayerUtils.formatSuggestKillCommand(this.name())
			);
		}
		else
		{
			text.append(
					InitWrap.text().formatText("§r\n - §cNO PLAYER")
			);
		}

		if (!ConfigWrap.mainOpt().reducedListDebugInfo)
		{
			text.append(
					InitWrap.text().formatText("§r\n - §7UUID: ")
			).append(
					InitWrap.text().formatText(
							this.profile() != null
							? ProfileWrap.id(Objects.requireNonNull(this.profile())).toString()
							: "§c<NULL>"
					)
			);
		}

		text.append(
				InitWrap.text().formatText("§r\n - §7Status: ")
		).append(
				InitWrap.text().formatText(this.enabled() ? "§6Enabled" : "§aDisabled")
		);

		if (this.enabled())
		{
			text.append(
					InitWrap.text().formatText("§r\n - §7Timeout: ")
			).append(
					InitWrap.text().formatText(this.timeoutFormatted())
			);

			if (!ConfigWrap.mainOpt().reducedListDebugInfo)
			{
				text.append(
						InitWrap.text().formatText("§r\n - §7Duration: ")
				).append(
						InitWrap.text().formatText(this.durationFormatted())
				).append(
						InitWrap.text().formatText("§r\n - §7Since: ")
				).append(
						InitWrap.text().formatText(this.startTimeEpochFormatted())
				);
			}
			text.append(
					InitWrap.text().formatText("§r\n - §7Reason: ")
			).append(
					InitWrap.text().formatText(this.reasonFormatted())
			);
		}

		return text;
	}

	@ApiStatus.Internal
	public void reset()
	{
		this.enabled = false;
		this.time = 0;
		this.startTimeMs = -1L;
		this.startTimeEpoch = -1L;
		this.timeout = -1L;
		this.reason = "";
	}
}
