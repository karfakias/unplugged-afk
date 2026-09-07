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

/*
 * This file is part of the Unplugged-AFK project, licensed under the
 * GNU Lesser General Public License v3.0
 */

package com.sakuraryoko.unplugged_afk.impl.mixins;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.sakuraryoko.unplugged_afk.impl.player.unplugged.UnpluggedServerPlayer;

@Mixin(ServerGamePacketListenerImpl.class)
@ApiStatus.Internal
public abstract class MixinServerGamePacketListenerImpl
{
	@Shadow public ServerPlayer player;

	@Inject(method = "removePlayerFromWorld", at = @At("HEAD"), cancellable = true)
	private void unplugged$skipAlreadyRemovedPlayer(CallbackInfo ci)
	{
		if (this.player instanceof UnpluggedServerPlayer
			|| !this.unplugged$isStillRegistered())
		{
			ci.cancel();
		}
	}

	private boolean unplugged$isStillRegistered()
	{
		MinecraftServer server;
		//#if MC >= 1.21.8
		//$$ server = this.player.level().getServer();
		//#elseif MC >= 1.20.1
		//$$ server = this.player.serverLevel().getServer();
		//#else
		server = this.player.getLevel().getServer();
		//#endif
		return server.getPlayerList().getPlayer(this.player.getUUID()) == this.player;
	}
}
