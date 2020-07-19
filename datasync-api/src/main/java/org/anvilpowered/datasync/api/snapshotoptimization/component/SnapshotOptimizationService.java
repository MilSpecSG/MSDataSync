/*
 *   DataSync - AnvilPowered
 *   Copyright (C) 2020 Cableguy20
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.anvilpowered.datasync.api.snapshotoptimization.component;

import org.anvilpowered.anvil.api.datastore.Component;

import java.util.Collection;
import java.util.UUID;

public interface SnapshotOptimizationService<
    TKey,
    TUser,
    TCommandSource,
    TDataStore>
    extends Component<TKey, TDataStore> {

    int getTotalMembers();

    int getMembersCompleted();

    int getSnapshotsDeleted();

    int getSnapshotsUploaded();

    boolean isOptimizationTaskRunning();

    boolean stopOptimizationTask();

    void addLockedPlayer(final UUID uuid);

    void removeLockedPlayer(final UUID uuid);

    boolean optimize(final Collection<? extends TUser> users, final TCommandSource source, final String name);

    boolean optimize(final TCommandSource source);
}