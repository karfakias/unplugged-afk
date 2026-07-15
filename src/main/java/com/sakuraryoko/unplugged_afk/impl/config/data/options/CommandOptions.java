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

package com.sakuraryoko.unplugged_afk.impl.config.data.options;

import org.jetbrains.annotations.ApiStatus;

import com.sakuraryoko.corelib.api.config.IConfigOption;

@ApiStatus.Internal
public class CommandOptions implements IConfigOption
{
    public int unpluggedCommandPermissions;
    public int unpluggedAdminCommandPermissions;
    public int afkCommandPermissions;
    public boolean enableUnpluggedCommand;
    public boolean enableAfkCommand;

    public CommandOptions()
    {
        this.defaults();
    }

    public void defaults()
    {
        this.unpluggedCommandPermissions = 0;
        this.unpluggedAdminCommandPermissions = 4;
        this.afkCommandPermissions = 0;
        this.enableUnpluggedCommand = true;
        this.enableAfkCommand = false;
    }

    @Override
    public CommandOptions copy(IConfigOption opt)
    {
        CommandOptions opts = (CommandOptions) opt;

        this.unpluggedCommandPermissions = opts.unpluggedCommandPermissions;
        this.unpluggedAdminCommandPermissions = opts.unpluggedAdminCommandPermissions;
        this.afkCommandPermissions = opts.afkCommandPermissions;
        this.enableUnpluggedCommand = opts.enableUnpluggedCommand;
        this.enableAfkCommand = opts.enableAfkCommand;

        return this;
    }
}
