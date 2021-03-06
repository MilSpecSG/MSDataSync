/*
 *   DataSync - AnvilPowered
 *   Copyright (C) 2020 Cableguy20
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.anvilpowered.datasync.sponge.command.optimize;

import com.google.inject.Inject;
import org.anvilpowered.anvil.api.plugin.PluginInfo;
import org.anvilpowered.datasync.api.registry.DataSyncKeys;
import org.anvilpowered.datasync.api.snapshotoptimization.SnapshotOptimizationManager;
import org.anvilpowered.datasync.sponge.command.SpongeSyncLockCommand;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Collection;
import java.util.Optional;

public class SpongeOptimizeStartCommand implements CommandExecutor {

    @Inject
    private PluginInfo<Text> pluginInfo;

    @Inject
    private SnapshotOptimizationManager<User, Text, CommandSource> snapshotOptimizationManager;

    @Override
    public CommandResult execute(CommandSource source, CommandContext context) throws CommandException {

        SpongeSyncLockCommand.assertUnlocked(source);

        Optional<String> optionalMode = context.getOne(Text.of("mode"));
        Collection<User> users = context.getAll(Text.of("user"));

        if (!optionalMode.isPresent()) {
            throw new CommandException(Text.of(pluginInfo.getPrefix(), "Mode is required"));
        }

        if (optionalMode.get().equals("all")) {
            if (!source.hasPermission(DataSyncKeys.MANUAL_OPTIMIZATION_ALL_PERMISSION.getFallbackValue())) {
                throw new CommandException(Text.of(pluginInfo.getPrefix(), "You do not have permission to start optimization task: all"));
            } else if (snapshotOptimizationManager.getPrimaryComponent().optimize(source)) {
                source.sendMessage(Text.of(pluginInfo.getPrefix(), TextColors.YELLOW, "Successfully started optimization task: all"));
            } else {
                throw new CommandException(Text.of(pluginInfo.getPrefix(), "Optimizer already running! Use /sync optimize info"));
            }
            snapshotOptimizationManager.getPrimaryComponent().optimize(source);
        } else {
            if (users.isEmpty()) {
                throw new CommandException(Text.of(pluginInfo.getPrefix(), "No users were selected by your query"));
            } else if (snapshotOptimizationManager.getPrimaryComponent().optimize(users, source, "Manual")) {
                source.sendMessage(Text.of(pluginInfo.getPrefix(), TextColors.YELLOW, "Successfully started optimization task: user"));
            } else {
                throw new CommandException(Text.of(pluginInfo.getPrefix(), "Optimizer already running! Use /sync optimize info"));
            }
        }

        return CommandResult.success();
    }
}
