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

package com.sakuraryoko.unplugged_afk.impl.player.unplugged;

import java.util.List;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
//#if MC >= 1.19.3
//$$ import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
//#endif
//#if MC >= 1.21.11
//$$ import net.minecraft.server.permissions.Permissions;
//#endif
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
//#if MC >= 1.21.6
//$$ import net.minecraft.server.waypoints.ServerWaypointManager;
//$$ import com.sakuraryoko.unplugged_afk.impl.player.interfaces.IWaypointManagerInvoker;
//#endif

import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.impl.player.interfaces.IPlayerListInvoker;

@ApiStatus.Internal
public class UnpluggedPlayerUtils
{
	@ApiStatus.Internal
	public static ImmutableList<UnpluggedServerPlayer> getShadows(@Nonnull MinecraftServer server)
	{
		ImmutableList.Builder<UnpluggedServerPlayer> builder = ImmutableList.builder();
		PlayerList pl = server.getPlayerList();
		List<ServerPlayer> players = pl.getPlayers();
		((IPlayerListInvoker) pl).unplugged$toggleBroadcastSystemMessage(false);

		for (ServerPlayer player : players)
		{
			if (player instanceof UnpluggedServerPlayer sp)
			{
				builder.add(sp);
			}
		}

		return builder.build();
	}

	@ApiStatus.Internal
	public static void hideAllUnpluggedFromPlayer(@Nonnull MinecraftServer server, @Nonnull ServerPlayer player)
	{
		if (ConfigWrap.unplugged().unpluggedHidePlayer)
		{
			ImmutableList<UnpluggedServerPlayer> shadows = getShadows(server);
			boolean result = false;

			if (ConfigWrap.unplugged().unpluggedHideFromOps && isOpWrap(player))
			{
				result = true;
			}
			else if (!isOpWrap(player))
			{
				result = true;
			}

			if (result)
			{
				for (UnpluggedServerPlayer shadow : shadows)
				{
					sendRemovePacketToPlayerWrap(shadow, player);
				}
			}
		}
	}

	@ApiStatus.Internal
	public static void unhideAllUnpluggedFromPlayer(@Nonnull MinecraftServer server, @Nonnull ServerPlayer player)
	{
		if (!ConfigWrap.unplugged().unpluggedHidePlayer ||
			(!ConfigWrap.unplugged().unpluggedHideFromOps) && isOpWrap(player))
		{
			ImmutableList<UnpluggedServerPlayer> shadows = getShadows(server);

			for (UnpluggedServerPlayer shadow : shadows)
			{
				sendAddPacketToPlayerWrap(shadow, player);

				// Note, that the difference between hiding from
				// Ops vs all players; is indistinguishable for Waypoints

				//#if MC >= 1.21.6
				//$$ if (!ConfigWrap.unplugged().unpluggedHidePlayer)
				//$$ {
					//$$ player.level().getWaypointManager().addPlayer(shadow);
				//$$ }
				//#endif
			}
		}
	}

	@ApiStatus.Internal
	protected static void sendHidePlayerPacket(@Nonnull MinecraftServer server, @Nonnull UnpluggedServerPlayer sp)
	{
		if (ConfigWrap.unplugged().unpluggedHidePlayer)
		{
			PlayerList pl = server.getPlayerList();
			List<ServerPlayer> players = pl.getPlayers();
			((IPlayerListInvoker) pl).unplugged$toggleBroadcastSystemMessage(false);

			for (ServerPlayer player : players)
			{
				boolean result = false;

				if (ConfigWrap.unplugged().unpluggedHideFromOps && isOpWrap(player))
				{
					result = true;
				}
				else if (!isOpWrap(player))
				{
					result = true;
				}

				if (result)
				{
					sendRemovePacketToPlayerWrap(sp, player);
				}
			}
		}
	}

	@ApiStatus.Internal
	protected static boolean isOpWrap(@Nonnull ServerPlayer player)
	{
		//#if MC >= 1.21.11
		//$$ return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
		//#else
		return player.hasPermissions(2);
		//#endif
	}

	@ApiStatus.Internal
	protected static void sendAddPacketToPlayerWrap(@Nonnull UnpluggedServerPlayer sp, @Nonnull ServerPlayer player)
	{
		player.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, sp));
		//#if MC >= 1.19.3
		//$$ player.connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, sp));
		//#endif
	}

	@ApiStatus.Internal
	protected static void sendRemovePacketToPlayerWrap(@Nonnull UnpluggedServerPlayer sp, @Nonnull ServerPlayer player)
	{
		//#if MC >= 1.19.3
		//$$ player.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(sp.getUUID())));
		//#else
		player.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, List.of(sp)));
		//#endif
	}

	//#if MC >= 1.21.6
	//$$ @ApiStatus.Internal
	//$$ public static void onAddOrUpdateWaypoint(ServerWaypointManager manager, @Nonnull ServerPlayer player)
	//$$ {
		//$$ if (ConfigWrap.unplugged().unpluggedHidePlayer && player instanceof UnpluggedServerPlayer sp)
		//$$ {
			//$$ if (sp.isValid())
			//$$ {
				//$$ boolean result = false;

				//$$ if (ConfigWrap.unplugged().unpluggedHideFromOps && isOpWrap(player))
				//$$ {
					//$$ result = true;
				//$$ }
				//$$ else if (!isOpWrap(player))
				//$$ {
					//$$ result = true;
				//$$ }

				//$$ if (result)
				//$$ {
					//$$ ((IWaypointManagerInvoker) manager).unplugged$removePlayer(player);
				//$$ }
			//$$ }
		//$$ }
	//$$ }

	//$$ @ApiStatus.Internal
	//$$ public static void onUnhideWaypoint(ServerWaypointManager manager, @Nonnull ServerPlayer player)
	//$$ {
		//$$ if (!ConfigWrap.unplugged().unpluggedHidePlayer && player instanceof UnpluggedServerPlayer sp)
		//$$ {
			//$$ if (sp.isValid())
			//$$ {
				//$$ ((IWaypointManagerInvoker) manager).unplugged$addPlayer(player);
			//$$ }
		//$$ }
	//$$ }
	//#endif
}
