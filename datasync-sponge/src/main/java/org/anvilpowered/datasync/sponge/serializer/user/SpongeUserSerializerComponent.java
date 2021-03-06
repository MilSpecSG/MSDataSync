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

package org.anvilpowered.datasync.sponge.serializer.user;

import com.google.inject.Inject;
import org.anvilpowered.datasync.api.model.snapshot.Snapshot;
import org.anvilpowered.datasync.api.registry.DataSyncKeys;
import org.anvilpowered.datasync.api.snapshotoptimization.SnapshotOptimizationManager;
import org.anvilpowered.datasync.common.serializer.user.CommonUserSerializerComponent;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SpongeUserSerializerComponent<
    TKey,
    TDataStore>
    extends CommonUserSerializerComponent<TKey, User, Player, Key<?>, TDataStore> {

    @Inject
    private PluginContainer pluginContainer;

    @Inject
    private SnapshotOptimizationManager<User, Text, CommandSource> snapshotOptimizationManager;

    @Override
    public boolean deserialize(Snapshot<?> snapshot, User user) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        Task.builder().execute(() ->
            result.complete(snapshotSerializer.deserialize(snapshot, user))
        ).submit(pluginContainer);
        return result.join();
    }

    @Override
    public CompletableFuture<Optional<Snapshot<TKey>>> deserialize(User user, CompletableFuture<Boolean> waitFuture) {
        snapshotOptimizationManager.getPrimaryComponent().addLockedPlayer(user.getUniqueId());
        // save current user data
        Snapshot<TKey> previousState = snapshotRepository.generateEmpty();
        serialize(previousState, user);
        if (registry.getOrDefault(DataSyncKeys.SERIALIZE_ENABLED_SERIALIZERS)
            .contains("datasync:inventory")) {
            user.getInventory().clear();
        }
        return waitFuture.thenApplyAsync(shouldDeserialize -> {
            if (!shouldDeserialize) {
                return Optional.<Snapshot<TKey>>empty();
            }
            return memberRepository.getLatestSnapshotForUser(user.getUniqueId())
                .exceptionally(e -> {
                    e.printStackTrace();
                    return Optional.empty();
                })
                .thenApplyAsync(optionalSnapshot -> {
                    // make sure user is still online
                    if (!user.isOnline()) {
                        logger.warn("{} has logged off. Skipping deserialization!", user.getName());
                        return Optional.<Snapshot<TKey>>empty();
                    }
                    if (!optionalSnapshot.isPresent()) {
                        logger.warn("Could not find snapshot for " + user.getName() + "! " +
                            "Check your DB configuration! Rolling back user.");
                        deserialize(previousState, user);
                        return Optional.<Snapshot<TKey>>empty();
                    }
                    if (deserialize(optionalSnapshot.get(), user)) {
                        return optionalSnapshot;
                    }
                    return Optional.<Snapshot<TKey>>empty();
                }).join();
        }).thenApplyAsync(s -> {
            snapshotOptimizationManager.getPrimaryComponent()
                .removeLockedPlayer(user.getUniqueId());
            return s;
        });
    }
}
