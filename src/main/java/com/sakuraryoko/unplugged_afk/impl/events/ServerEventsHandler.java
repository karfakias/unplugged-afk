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

package com.sakuraryoko.unplugged_afk.impl.events;

import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;

import com.sakuraryoko.unplugged_afk.impl.UnpluggedAfk;
import com.sakuraryoko.unplugged_afk.impl.player.PlayerManager;
import com.sakuraryoko.unplugged_afk.impl.player.UnpluggedPendingSpawns;
import com.sakuraryoko.corelib.api.events.IServerEventsDispatch;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.UnpluggedPlayerUtils;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.UnpluggedServerPlayer;

@ApiStatus.Internal
public class ServerEventsHandler implements IServerEventsDispatch
{
	private static final ServerEventsHandler INSTANCE = new ServerEventsHandler();
	public static ServerEventsHandler getInstance() { return INSTANCE; }
	private boolean hideAllPlayers = false;
	private boolean unhideAllPlayers = false;
	private boolean tickingLock = true;
	private final long startupTime = System.currentTimeMillis();

	@Override
	public void onStarting(MinecraftServer server)
	{
		this.tickingLock = true;
		UnpluggedPendingSpawns.INSTANCE.unlock();
	}

	@Override
	public void onStarted(MinecraftServer server)
	{
		this.tickingLock = true;
		PlayerManager.getInstance().onServerStarted(server);
	}

	@Override
	public void onReloadComplete(MinecraftServer server, Collection<String> resources)
	{
		// TODO
	}

	@Override
	public void onDedicatedStarted(DedicatedServer server)
	{
		// TODO
	}

	@Override
	public void onIntegratedStarted(IntegratedServer server)
	{
		// TODO
	}

	@Override
	public void onOpenToLan(IntegratedServer server, GameType mode)
	{
		// TODO
	}

	public void onTick(MinecraftServer server)
	{
		UnpluggedPendingSpawns.INSTANCE.tick(server);

		// Hold additional tick tasks until server has been running for at least 30 seconds.
		if (this.tickingLock)
		{
			final long now = System.currentTimeMillis();

			if ((now - this.startupTime) > 30000L)
			{
				this.tickingLock = false;
			}
		}

		if (!this.tickingLock)
		{
			PlayerManager.getInstance().onTick(server);

			if (this.hideAllPlayers || this.unhideAllPlayers)
			{
				this.processAllHideOrUnhide(server);
			}
		}
	}

	@ApiStatus.Internal
	private void processAllHideOrUnhide(MinecraftServer server)
	{
		PlayerList pl = server.getPlayerList();
		List<ServerPlayer> players = pl.getPlayers();

		UnpluggedAfk.debugLog("processAllHideOrUnhide()");

		// Fom changing the config options
		for (ServerPlayer player : players)
		{
			if (this.hideAllPlayers && !(player instanceof UnpluggedServerPlayer))
			{
				UnpluggedPlayerUtils.hideAllUnpluggedFromPlayer(server, player);
			}
			else if (this.unhideAllPlayers && !(player instanceof UnpluggedServerPlayer))
			{
				UnpluggedPlayerUtils.unhideAllUnpluggedFromPlayer(server, player);
			}
		}

		this.hideAllPlayers = false;
		this.unhideAllPlayers = false;
	}

	@Override
	public void onDedicatedStopping(DedicatedServer server)
	{
		// TODO
	}

	@Override
	public void onStopping(MinecraftServer server)
	{
		PlayerManager.getInstance().onServerStop(server);
	}

	@Override
	public void onStopped(MinecraftServer server)
	{
		// TODO
	}

	@ApiStatus.Internal
	public void toggleHideAllPlayers(boolean toggle)
	{
		this.hideAllPlayers = toggle;
	}

	@ApiStatus.Internal
	public void toggleUnhideAllPlayers(boolean toggle)
	{
		this.unhideAllPlayers = toggle;
	}
}
