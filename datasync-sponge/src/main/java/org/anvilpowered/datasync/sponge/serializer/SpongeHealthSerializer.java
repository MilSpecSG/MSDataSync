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

package org.anvilpowered.datasync.sponge.serializer;

import org.anvilpowered.datasync.api.model.snapshot.Snapshot;
import org.anvilpowered.datasync.common.serializer.CommonHealthSerializer;
import org.anvilpowered.datasync.sponge.util.Utils;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;

import java.util.Optional;

public class SpongeHealthSerializer extends CommonHealthSerializer<Key<?>, User> {

    @Override
    public boolean serialize(Snapshot<?> snapshot, User user) {
        Optional<Double> health = user.get(Keys.HEALTH).flatMap(h -> {
            if (h <= 0) {
                return user.get(Keys.MAX_HEALTH);
            } else {
                return Optional.of(h);
            }
        });
        return snapshotManager.getPrimaryComponent()
            .setSnapshotValue(snapshot, Keys.HEALTH, health);
    }

    @Override
    public boolean deserialize(Snapshot<?> snapshot, User user) {
        return Utils.deserialize(snapshotManager, snapshot, user, Keys.HEALTH);
    }
}
