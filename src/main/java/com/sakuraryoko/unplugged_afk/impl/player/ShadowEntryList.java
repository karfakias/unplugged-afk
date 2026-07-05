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

import java.util.HashMap;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import net.minecraft.network.chat.Component;

import com.sakuraryoko.unplugged_afk.impl.UnpluggedAfk;
import com.sakuraryoko.unplugged_afk.impl.player.shadow.ShadowServerPlayer;
import com.sakuraryoko.unplugged_afk.impl.player.state.ShadowState;

@ApiStatus.Internal
public class ShadowEntryList
{
	private static final ShadowEntryList INSTANCE = new ShadowEntryList();
	public static ShadowEntryList getInstance() { return INSTANCE; }
	private final HashMap<UUID, ShadowEntry> shadowMap;

	private ShadowEntryList()
	{
		this.shadowMap = new HashMap<>();
	}

	public @Nullable ShadowEntry get(@Nonnull ShadowServerPlayer player)
	{
		UUID uuid = player.getUUID();

		if (this.shadowMap.containsKey(uuid))
		{
			return this.shadowMap.get(uuid);
		}

		return null;
	}

	public @Nullable ShadowEntry add(@Nonnull ShadowServerPlayer player, ShadowState state)
	{
		if (this.get(player) == null)
		{
			ShadowEntry entry = ShadowEntry.create(player);

			if (state.enabled())
			{
				entry.updateShadowState(state);
			}

			this.shadowMap.put(player.getUUID(), entry);
			UnpluggedAfk.debugLog("ShadowEntryList(): add({}) --> ADD", entry.name().getString());
			return entry;
		}

		return this.get(player);
	}

	public void updateShadow(@Nonnull ShadowServerPlayer player)
	{
		UUID uuid = player.getUUID();

		if (this.shadowMap.containsKey(uuid))
		{
			ShadowEntry entry = this.shadowMap.get(uuid);

			if (entry != null)
			{
				entry.setShadowPlayer(player);
			}
		}
	}

	protected void syncShadowEntry(@Nonnull ShadowServerPlayer player, ShadowEntry entry)
	{
		UUID uuid = player.getUUID();

		if (entry.matches(uuid))
		{
			this.shadowMap.remove(uuid);
			entry.setShadowPlayer(player);
			this.shadowMap.put(uuid, entry);
		}
	}

	public void remove(@Nonnull UUID uuid)
	{
		ShadowEntry entry = this.shadowMap.remove(uuid);

		if (entry != null)
		{
			this.shadowMap.remove(uuid);
			UnpluggedAfk.debugLog("ShadowEntryList(): remove({}) --> REMOVE", entry.name().getString());
			entry.handler().unregisterShadowAfk();
		}
	}

	public void remove(@Nonnull ShadowServerPlayer player)
	{
		this.remove(player.getUUID());
	}

	@VisibleForTesting
	public ImmutableMap<UUID, ShadowEntry> shadowMapCopy()
	{
		ImmutableMap.Builder<UUID, ShadowEntry> builder = ImmutableMap.builder();
		this.shadowMap.forEach(builder::put);
		return builder.build();
	}

	@VisibleForTesting
	public Component getDebugFormatted(UUID uuid)
	{
		if (this.shadowMap.containsKey(uuid))
		{
			ShadowEntry entry = this.shadowMap.get(uuid);

			if (entry != null)
			{
				return entry.getDebugFormatted();
			}
		}

		return Component.literal("§cShadow Player not found§r");
	}
}
