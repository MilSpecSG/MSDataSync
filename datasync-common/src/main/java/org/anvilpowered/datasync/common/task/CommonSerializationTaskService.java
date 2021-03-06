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

package org.anvilpowered.datasync.common.task;

import com.google.inject.Inject;
import org.anvilpowered.anvil.api.registry.Registry;
import org.anvilpowered.datasync.api.registry.DataSyncKeys;
import org.anvilpowered.datasync.api.snapshotoptimization.SnapshotOptimizationManager;
import org.anvilpowered.datasync.api.task.SerializationTaskService;

public abstract class CommonSerializationTaskService<
    TUser,
    TString,
    TCommandSource>
    implements SerializationTaskService {

    @Inject
    protected SnapshotOptimizationManager<TUser, TString, TCommandSource>
        snapshotOptimizationManager;

    protected Registry registry;

    protected int baseInterval;

    protected CommonSerializationTaskService(Registry registry) {
        this.registry = registry;
        registry.whenLoaded(this::registryLoaded).register();
    }

    private void registryLoaded() {
        baseInterval = registry.getOrDefault(DataSyncKeys.SNAPSHOT_UPLOAD_INTERVAL_MINUTES);
        stopSerializationTask();
        startSerializationTask();
    }
}