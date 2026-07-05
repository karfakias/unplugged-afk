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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import com.sakuraryoko.corelib.impl.config.ConfigManager;
import com.sakuraryoko.unplugged_afk.impl.UnpluggedAfk;
import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.impl.config.UnpluggedConfigHandler;
import com.sakuraryoko.unplugged_afk.impl.config.data.options.PlayerOptions;
import com.sakuraryoko.unplugged_afk.impl.player.shadow.ShadowServerPlayer;
import com.sakuraryoko.unplugged_afk.impl.player.state.*;

@ApiStatus.Internal
public class PlayerManager
{
	private static final PlayerManager INSTANCE = new PlayerManager();
	public static PlayerManager getInstance() { return INSTANCE; }

	private final HashMap<UUID, PlayerEntry> players;
	private long lastTick;

	@ApiStatus.Internal
	private PlayerManager()
	{
		this.players = new HashMap<>();
		this.lastTick = System.currentTimeMillis();
	}

	@ApiStatus.Internal
	public void syncProfile(GameProfile profile)
	{
		if (profile == null) { return; }
		List<PlayerOptions> config = ConfigWrap.players();
		UUID uuid = ProfileWrap.id(profile);

		for (PlayerOptions opt : config)
		{
			if (opt.uuid.equals(uuid))
			{
				this.addOrUpdateProfile(profile, opt.state);
				return;
			}
		}

		// Doesn't exist in config --> Add
		this.addConfig(profile);
		this.addOrUpdateProfile(profile, ShadowState.DEFAULT);
	}

	@ApiStatus.Internal
	public void syncFromConfig(@Nonnull PlayerOptions opt)
	{
		this.addOrUpdateProfile(ProfileWrap.profile(opt.uuid, opt.name), opt.state);
	}

	@ApiStatus.Internal
	private void addOrUpdateProfile(@Nonnull GameProfile profile, ShadowState state)
	{
		UUID uuid = ProfileWrap.id(profile);
		String name = ProfileWrap.name(profile);
		PosState pos = PosWrap.defaultPos();
		GameState game = GameWrap.defMode();
		PlayerEntry newEntry = new PlayerEntry(uuid, name, state, pos, game);

		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null && !entry.state().equals(state))
			{
				entry = this.players.remove(uuid);
				newEntry = entry.updateState(state);
				this.players.put(uuid, newEntry);
			}
		}
		else
		{
			this.players.put(uuid, newEntry);
		}

		UnpluggedAfk.debugLog("addOrUpdateProfile: player: ['{}'/{}] state: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), state.toString());
	}

	@ApiStatus.Internal
	private void addConfig(@Nonnull GameProfile profile)
	{
		List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());
		UUID uuid = ProfileWrap.id(profile);
		boolean exists = false;

		for (PlayerOptions entry : config)
		{
			if (entry.uuid.equals(uuid))
			{
				exists = true;
				break;
			}
		}

		if (!exists)
		{
			PlayerOptions opt = PlayerOptions.fromProfile(profile, ShadowState.DEFAULT);

			if (this.players.containsKey(uuid))
			{
				PlayerEntry entry = this.players.get(uuid);

				if (entry != null)
				{
					opt.pos = entry.pos();
					opt.game = entry.game();
				}
			}

			ConfigWrap.players().add(opt);
		}

		UnpluggedAfk.debugLog("addConfig: player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
	}

	@ApiStatus.Internal
	private void setConfig(@Nonnull GameProfile profile, ShadowState state)
	{
		List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());
		UUID uuid = ProfileWrap.id(profile);
		boolean dirty = false;

		for (PlayerOptions entry : config)
		{
			if (entry.uuid.equals(uuid))
			{
				entry.state = state;
				entry.name = ProfileWrap.name(profile);

				if (this.players.containsKey(uuid))
				{
					PlayerEntry playerEntry = this.players.get(uuid);

					if (playerEntry != null)
					{
						entry.pos = playerEntry.pos();
						entry.game = playerEntry.game();
					}

					break;
				}

				dirty = true;
			}
		}

		if (dirty)
		{
			ConfigWrap.players().clear();

			for (PlayerOptions entry : config)
			{
				PlayerOptions opt = new PlayerOptions(entry);

				if (this.players.containsKey(uuid))
				{
					PlayerEntry playerEntry = this.players.get(uuid);

					if (playerEntry != null)
					{
						entry.pos = playerEntry.pos();
						entry.game = playerEntry.game();
					}

					break;
				}

				ConfigWrap.players().add(opt);
			}
		}

		UnpluggedAfk.debugLog("setConfig: player: ['{}'/{}] state: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), state.toString());
	}

	public ShadowState getShadowState(@Nonnull GameProfile profile)
	{
		UUID uuid = ProfileWrap.id(profile);

		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null)
			{
				return entry.state();
			}
		}

		UnpluggedAfk.debugLog("getShadowState: player: ['{}'/{}] failure; adding new entry", ProfileWrap.name(profile), ProfileWrap.id(profile));
		this.addOrUpdateProfile(profile, ShadowState.DEFAULT);
		this.addConfig(profile);
		return ShadowState.DEFAULT;
	}

	@ApiStatus.Internal
	public ShadowState getShadowState(@Nonnull UUID uuid)
	{
		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null)
			{
				return entry.state();
			}
		}

		return ShadowState.DEFAULT;
	}

	@ApiStatus.Internal
	public void setShadowState(@Nonnull GameProfile profile, ShadowState state)
	{
		this.addOrUpdateProfile(profile, state);
		this.setConfig(profile, state);
		UnpluggedAfk.debugLog("setShadowState: player: ['{}'/{}] state: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), state.toString());
	}

	public void resetShadowState(@Nonnull ServerPlayer player)
	{
		this.setShadowState(player.getGameProfile(), ShadowState.DEFAULT);
	}

	public void remove(@Nonnull UUID uuid)
	{
		ShadowEntryList.getInstance().remove(uuid);
		this.players.remove(uuid);
		ConfigWrap.players().removeIf(opt -> opt.uuid.equals(uuid));
	}

	public PosState getPosState(@Nonnull UUID uuid)
	{
		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null)
			{
				return entry.pos();
			}
		}

		List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());

		for (PlayerOptions entry : config)
		{
			if (entry.uuid.equals(uuid))
			{
				return entry.pos;
			}
		}

		return PosWrap.defaultPos();
	}

	public GameState getGameMode(@Nonnull UUID uuid)
	{
		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null)
			{
				return entry.game();
			}
		}

		List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());

		for (PlayerOptions entry : config)
		{
			if (entry.uuid.equals(uuid))
			{
				return entry.game;
			}
		}

		return GameWrap.defMode();
	}

	@ApiStatus.Internal
	public void updatePlayerData(@Nonnull ServerPlayer player)
	{
		PosState pos = PosWrap.of(player);
		GameState game = GameWrap.of(player);
		UUID uuid = player.getUUID();

		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.remove(uuid);

			if (entry != null)
			{
				entry = entry.updatePlayerData(player.getName().getString(), pos, game);
				this.players.put(uuid, entry);
			}
		}
	}

	@VisibleForTesting
	public ImmutableMap<UUID, PlayerEntry> playerMapCopy()
	{
		ImmutableMap.Builder<UUID, PlayerEntry> map = ImmutableMap.builder();
		this.players.forEach(map::put);
		return map.build();
	}

	@VisibleForTesting
	public Component getDebugFormatted(UUID uuid)
	{
		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null)
			{
				return entry.getDebugFormatted();
			}
		}

		return Component.literal("§cPlayer not found§r");
	}

	@ApiStatus.Internal
	private boolean syncConfig(@Nonnull MinecraftServer server, boolean stop)
	{
		PlayerList playerList = server.getPlayerList();
		List<ServerPlayer> players = playerList.getPlayers();
		boolean dirty = false;

		for (ServerPlayer player : players)
		{
			if (this.syncConfigEach(player))
			{
				dirty = true;
			}
		}

		if (stop) { return dirty; }

		// Spawn shadow configured players
		List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());

		for (PlayerOptions entry : config)
		{
			boolean found = false;

			for (ServerPlayer player : players)
			{
				if (entry.uuid.equals(player.getUUID()))
				{
					found = true;
					break;
				}
			}

			if (!found && entry.state.enabled())
			{
				UnpluggedAfk.debugLog("syncConfig: Scheduling Shadow player: ['{}'/{}]", entry.name, entry.uuid.toString());
				PendingShadowSpawns.INSTANCE.scheduleSpawn(entry);
			}
		}

		return dirty;
	}

	@ApiStatus.Internal
	private boolean syncConfigEach(ServerPlayer player)
	{
		List<PlayerOptions> oldConfig = new ArrayList<>(ConfigWrap.players());
		List<PlayerOptions> newConfig = new ArrayList<>();
		String name = player.getName().getString();
		UUID uuid = player.getUUID();
		PosState pos = PosWrap.of(player);
		GameState game = GameWrap.of(player);
		ShadowState state = this.getShadowState(uuid).ensureValid();

		if (player instanceof ShadowServerPlayer shadow)
		{
			ShadowEntry entry = ShadowEntryList.getInstance().get(shadow);

			if (entry == null)
			{
				if (state.isEmpty())
				{
					state = new ShadowState(shadow.isValid(), shadow.getTimer(), shadow.getTimeout(), shadow.getReason());
				}

				entry = ShadowEntryList.getInstance().add(shadow, state);
			}

			if (entry != null)
			{
				state = new ShadowState(entry.shadowEnabled(), entry.shadowTimer(), shadow.getTimeout(), entry.reason());
			}
			else
			{
				state = new ShadowState(shadow.isValid(), shadow.getTimer(), shadow.getTimeout(), shadow.getReason());
			}
		}

		boolean found = false;
		boolean dirty = false;

		UnpluggedAfk.debugLog("syncConfigEach(): Player ['{}'/{}]; state: [{}], pos: [{}], game: [{}]", name, uuid.toString(), state.toString(), pos.toString(), game.toString());

		// FIXME -- Something is still broken here.
		for (PlayerOptions entry : oldConfig)
		{
			if (!found && entry.uuid.equals(uuid))
			{
				if (entry.state.isEmpty() || !entry.state.equals(state))
				{
					entry.state = state;
					dirty = true;
				}
				if (entry.pos.isEmpty() || !entry.pos.equals(pos))
				{
					entry.pos = pos;
					dirty = true;
				}
				if (entry.game.isEmpty() || !entry.game.equals(game))
				{
					entry.game = game;
					dirty = true;
				}
				if (entry.name.isEmpty() || entry.name.equals(uuid.toString()) || !entry.name.equals(name))
				{
					entry.name = name;
					dirty = true;
				}

				found = true;
			}

			newConfig.add(entry);
		}

		if (!found)
		{
			PlayerOptions opt = PlayerOptions.fromProfile(player.getGameProfile(), state);
			opt.state = state;
			opt.pos = pos;
			opt.game = game;
			newConfig.add(opt);
			dirty = true;
		}

		if (dirty)
		{
			ConfigWrap.players().clear();

			for (PlayerOptions entry : newConfig)
			{
				PlayerOptions opt = new PlayerOptions(entry);

				// Double Verify
				if (!opt.state.equals(state))
				{
					opt.state = state;
				}
				if (!opt.pos.equals(pos))
				{
					opt.pos = pos;
				}
				if (!opt.game.equals(game))
				{
					opt.game = game;
				}

				ConfigWrap.players().add(opt);
			}

			return true;
		}

		return false;
	}

	@ApiStatus.Internal
	public void onServerStop(@Nonnull MinecraftServer server)
	{
		UnpluggedAfk.debugLog("onServerStop --> syncConfig()");

		this.syncConfig(server, true);
		UnpluggedAfk.debugLog("onServerStop(): flushing config ...");
		ConfigManager.getInstance().saveEach(UnpluggedConfigHandler.getInstance());
	}

	@ApiStatus.Internal
	public void onServerStarted(@Nonnull MinecraftServer server)
	{
		UnpluggedAfk.debugLog("onServerStarted --> syncConfig()");

		if (this.syncConfig(server, false))
		{
			UnpluggedAfk.debugLog("onServerStarted(): flushing changes ...");
			ConfigManager.getInstance().saveEach(UnpluggedConfigHandler.getInstance());
		}
	}

	@ApiStatus.Internal
	public void onServerResync(@Nonnull MinecraftServer server, ImmutableMap<UUID, PlayerEntry> playerMap, ImmutableMap<UUID, ShadowEntry> shadowMap)
	{
		boolean dirty = false;
		// Same as stop, really; just with a different message
		UnpluggedAfk.debugLog("onServerResync --> syncConfig()");

		if (this.syncConfig(server, true))
		{
			dirty = true;
		}

		if (this.syncShadowEntries(server, playerMap, shadowMap))
		{
			dirty = true;
		}

		if (dirty)
		{
			UnpluggedAfk.debugLog("onServerResync(): flushing changes ...");
			ConfigManager.getInstance().saveEach(UnpluggedConfigHandler.getInstance());
		}
	}

	private boolean syncShadowEntries(@Nonnull MinecraftServer server, ImmutableMap<UUID, PlayerEntry> playerMap, ImmutableMap<UUID, ShadowEntry> shadowMap)
	{
		PlayerList playerList = server.getPlayerList();
		List<ServerPlayer> players = playerList.getPlayers();
		boolean dirty = false;

		UnpluggedAfk.debugLog("syncShadowEntries --> count: {}", shadowMap.size());

		for (ServerPlayer player : players)
		{
			UUID uuid = player.getUUID();

			if (player instanceof ShadowServerPlayer sp)
			{
				if (shadowMap.containsKey(uuid))
				{
					ShadowEntry entry = shadowMap.get(uuid);

					if (entry != null)
					{
						ShadowState newState = null;

						if (playerMap.containsKey(uuid))
						{
							PlayerEntry playerEntry = playerMap.get(uuid);

							if (playerEntry != null)
							{
								newState = playerEntry.state();
							}
						}

						if (newState == null)
						{
							newState = new ShadowState(sp.isValid(), sp.getTimer(), sp.getTimeout(), sp.getReason());
						}

						UnpluggedAfk.debugLog("syncShadowEntries --> sync: ['{}'/{}], state: [{}]", sp.getName().getString(), uuid.toString(), newState.toString());
						this.setShadowState(sp.getGameProfile(), newState);
						entry.updateShadowState(newState);
						ShadowEntryList.getInstance().syncShadowEntry(sp, entry);
						dirty = true;
					}
				}
			}
		}

		return dirty;
	}

	@ApiStatus.Internal
	public void onTick(@Nonnull MinecraftServer server)
	{
		final long now = System.currentTimeMillis();

		if ((now - this.lastTick) > this.onTickTimeout())
		{
			this.onTickCycle(server);
			this.lastTick = now;
		}
	}

	private long onTickTimeout()
	{
		return (long) (3.75f * 1000L);
	}

	private void onTickCycle(@Nonnull MinecraftServer server)
	{
		PlayerList playerList = server.getPlayerList();
		List<ServerPlayer> players = playerList.getPlayers();
		boolean dirty = false;

		for (ServerPlayer player : players)
		{
			if (this.onTickEach(player))
			{
				dirty = true;
			}
		}

		if (dirty)
		{
			UnpluggedAfk.debugLog("onServerResync(): flushing changes ...");
			ConfigManager.getInstance().saveEach(UnpluggedConfigHandler.getInstance());
		}
	}

	private boolean onTickEach(ServerPlayer player)
	{
		UUID uuid = player.getUUID();

		if (!this.players.containsKey(uuid))
		{
			this.addOrUpdateProfile(player.getGameProfile(), ShadowState.DEFAULT);
			this.updatePlayerData(player);
			UnpluggedAfk.debugLog("onTickEach() sync: ['{}'/{}] --> added missing player", player.getName().getString(), uuid.toString());
			return true;
		}

		return false;
	}
}
