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

package com.sakuraryoko.unplugged_afk.impl.mixins;

import java.net.SocketAddress;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import com.sakuraryoko.unplugged_afk.impl.config.ConfigWrap;
import com.sakuraryoko.unplugged_afk.impl.player.PlayerManager;
import com.sakuraryoko.unplugged_afk.impl.player.UnpluggedEntry;
import com.sakuraryoko.unplugged_afk.impl.player.UnpluggedEntryList;
import com.sakuraryoko.unplugged_afk.impl.player.interfaces.IPlayerListInvoker;
import com.sakuraryoko.unplugged_afk.impl.player.state.UnpluggedState;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.sakuraryoko.unplugged_afk.impl.modinit.InitWrap;
import com.sakuraryoko.unplugged_afk.impl.player.unplugged.UnpluggedServerPlayer;

//#if MC >= 1.21.10
//$$ import net.minecraft.server.players.NameAndId;
//$$ import com.sakuraryoko.unplugged_afk.impl.player.state.ProfileWrap;
//#else
import com.mojang.authlib.GameProfile;
//#endif

@Mixin(ServerLoginPacketListenerImpl.class)
@ApiStatus.Internal
public abstract class MixinServerLoginPacketListenerImpl
{
//#if MC >= 1.21.10
	//$$ @WrapOperation(method = "verifyLoginAndFinishConnectionSetup",
						//$$ at = @At(value = "INVOKE",
									//$$ target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;"))
	//$$ private Component unplugged$checkForStaleShadow(PlayerList instance, SocketAddress socketAddress,
														//$$ NameAndId nameAndId,
														//$$ Operation<Component> original)
	//$$ {
		//$$ ServerPlayer player = instance.getPlayer(nameAndId.id());
//#elseif MC >= 1.20.2
	//$$ @WrapOperation(method = "verifyLoginAndFinishConnectionSetup",
						//$$ at = @At(value = "INVOKE",
									//$$ target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"))
	//$$ private Component unplugged$checkForStaleShadow(PlayerList instance, SocketAddress socketAddress,
														//$$ GameProfile gameProfile,
														//$$ Operation<Component> original)
//$$ {
	//$$ ServerPlayer player = instance.getPlayer(gameProfile.getId());
//#else
	@WrapOperation(method = "handleAcceptedLogin",
	               at = @At(value = "INVOKE",
	                        target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"))
	private Component unplugged$checkForStaleShadow(PlayerList instance, SocketAddress socketAddress,
													GameProfile gameProfile,
													Operation<Component> original)
	{
		ServerPlayer player = instance.getPlayer(gameProfile.getId());
//#endif

		if (player instanceof UnpluggedServerPlayer sp)
		{
			if (sp.isValid())
			{
				UnpluggedEntry entry = UnpluggedEntryList.getInstance().get(sp);

				if (entry != null)
				{
					UnpluggedEntryList.getInstance().remove(sp, false);
				}

				//#if MC >= 1.21.10
				//$$ PlayerManager.getInstance().setShadowState(ProfileWrap.profile(nameAndId), UnpluggedState.DEFAULT);
				//#else
				PlayerManager.getInstance().setShadowState(gameProfile, UnpluggedState.DEFAULT);
				//#endif
			}

			if (player.isInvulnerable() && player.gameMode.isSurvival())
			{
				player.setInvulnerable(false);
			}

			if (ConfigWrap.mess().hideUnpluggedJoin)
			{
				((IPlayerListInvoker) instance).unplugged$toggleBroadcastSystemMessage(true);
			}

			String str = "shadow replaced";
			sp.kill(InitWrap.text().formatText(str));
			instance.remove(player);
			((IPlayerListInvoker) instance).unplugged$toggleBroadcastSystemMessage(false);
		}

//#if MC >= 1.21.10
		//$$ return original.call(instance, socketAddress, nameAndId);
//#else
		return original.call(instance, socketAddress, gameProfile);
//#endif
	}
}
