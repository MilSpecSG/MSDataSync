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

package org.anvilpowered.datasync.common.serializer;

import com.google.inject.Inject;
import org.anvilpowered.anvil.api.registry.Registry;
import org.anvilpowered.anvil.api.util.UserService;
import org.anvilpowered.datasync.api.model.snapshot.Snapshot;
import org.anvilpowered.datasync.api.registry.DataSyncKeys;
import org.anvilpowered.datasync.api.serializer.ExperienceSerializer;
import org.anvilpowered.datasync.api.serializer.GameModeSerializer;
import org.anvilpowered.datasync.api.serializer.HealthSerializer;
import org.anvilpowered.datasync.api.serializer.HungerSerializer;
import org.anvilpowered.datasync.api.serializer.InventorySerializer;
import org.anvilpowered.datasync.api.serializer.Serializer;
import org.anvilpowered.datasync.api.serializer.SnapshotSerializer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public abstract class CommonSnapshotSerializer<
    TDataKey,
    TUser,
    TPlayer,
    TInventory,
    TItemStackSnapshot>
    extends CommonSerializer<TDataKey, TUser>
    implements SnapshotSerializer<TUser> {

    @Override
    public String getName() {
        return "datasync:snapshot";
    }

    private List<Serializer<TUser>> serializers;

    private List<Serializer<TUser>> externalSerializers;

    @Inject
    private ExperienceSerializer<TUser> experienceSerializer;

    @Inject
    private GameModeSerializer<TUser> gameModeSerializer;

    @Inject
    private HealthSerializer<TUser> healthSerializer;

    @Inject
    private HungerSerializer<TUser> hungerSerializer;

    @Inject
    private InventorySerializer<TUser, TInventory, TItemStackSnapshot> inventorySerializer;

    @Inject
    private Logger logger;

    @Inject
    private UserService<TUser, TPlayer> userService;

    protected Registry registry;

    protected CommonSnapshotSerializer(Registry registry) {
        this.registry = registry;
        registry.whenLoaded(this::registryLoaded).register();
        externalSerializers = new ArrayList<>();
    }

    @Override
    public boolean isSerializerEnabled(String name) {
        return serializers.stream().anyMatch(serializer -> serializer.getName().equals(name));
    }

    @Override
    public void registerSerializer(Serializer<TUser> serializer) {
        externalSerializers.add(serializer);
    }

    private void registryLoaded() {

        postLoadedEvent();

        verifyExternalSerializers();

        List<String> enabledSerializers = new ArrayList<>(
            registry.getOrDefault(DataSyncKeys.SERIALIZE_ENABLED_SERIALIZERS));
        serializers = new ArrayList<>();

        if (enabledSerializers.remove("datasync:experience")) {
            announceEnabled("datasync:experience");
            serializers.add(experienceSerializer);
        }

        if (enabledSerializers.remove("datasync:gameMode")) {
            announceEnabled("datasync:gameMode");
            serializers.add(gameModeSerializer);
        }

        if (enabledSerializers.remove("datasync:health")) {
            announceEnabled("datasync:health");
            serializers.add(healthSerializer);
        }

        if (enabledSerializers.remove("datasync:hunger")) {
            announceEnabled("datasync:hunger");
            serializers.add(hungerSerializer);
        }

        if (enabledSerializers.remove("datasync:inventory")) {
            announceEnabled("datasync:inventory");
            serializers.add(inventorySerializer);
        }

        externalSerializers.stream()
            .filter(serializer -> enabledSerializers.remove(serializer.getName()))
            .forEach(serializer -> serializers.add(serializer));
    }

    protected abstract void postLoadedEvent();

    private void verifyExternalSerializers() {
        List<Serializer<TUser>> externalSerializersToRemove = new ArrayList<>();
        externalSerializers.forEach(serializer -> {
            String name = serializer.getName();
            if (name.startsWith("datasync") || name.split(":").length != 2) {
                logger.warn("[DataSync] External serialization module {} is invalid. Ignoring!",
                    serializer.getName());
                externalSerializersToRemove.add(serializer);
            }
        });

        externalSerializersToRemove.forEach(externalSerializers::remove);
    }

    protected abstract void announceEnabled(String name);

    @Override
    public boolean serialize(Snapshot<?> snapshot, TUser user) {
        if (serializers.isEmpty()) {
            logger.info("[DataSync] No enabled serializers");
            return false;
        }
        boolean success = true;

        for (Serializer<TUser> serializer : serializers) {
            // will still try to keep going even if one module fails
            try {
                snapshot.getModulesUsed().add(serializer.getName());
                if (!serializer.serialize(snapshot, user)) {
                    logger.error(
                        "[DataSync] Serialization module {} failed for {}! All valid data was "
                            + "still uploaded!",
                        serializer.getName(),
                        userService.getUserName(user)
                    );
                    success = false;
                    snapshot.getModulesFailed().add(serializer.getName());
                }
            } catch (RuntimeException e) {
                success = false;
            }
        }
        return success;
    }

    @Override
    public boolean deserialize(Snapshot<?> snapshot, TUser user) {
        if (serializers.isEmpty()) {
            System.err.println("[DataSync] No enabled deserializers");
            return false;
        }
        boolean success = true;
        List<String> serializersToUse = new ArrayList<>(snapshot.getModulesUsed());
        for (Serializer<TUser> serializer : serializers) {
            // will still try to keep going even if one module fails
            try {
                if (serializersToUse.remove(serializer.getName())) {
                    // only use modules that were used to upload
                    if (!serializer.deserialize(snapshot, user)) {
                        logger.error(
                            "[DataSync] Deserialization module {} failed for snapshot {} for {}",
                            serializer.getName(),
                            snapshot.getIdAsString(),
                            userService.getUserName(user)
                        );
                        success = false;
                    }
                } else {
                    logger.warn(
                        "[DataSync] Deserialization module {} was not used in "
                            + "snapshot {} for {} but it is enabled in the config, skipping!",
                        serializer.getName(),
                        snapshot.getIdAsString(),
                        userService.getUserName(user)
                    );
                }
            } catch (Exception e) {
                success = false;
            }
        }

        serializersToUse.forEach(moduleName -> logger.warn(
            "[DataSync] Deserialization module {} was used in snapshot {} but is not enabled "
                + "in the config! Not all data from snapshot could be added to {}",
            moduleName,
            snapshot.getIdAsString(),
            userService.getUserName(user)
        ));
        return success;
    }
}
