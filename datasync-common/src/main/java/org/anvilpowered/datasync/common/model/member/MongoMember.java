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

package org.anvilpowered.datasync.common.model.member;

import com.google.common.base.Preconditions;
import dev.morphia.annotations.Entity;
import org.anvilpowered.anvil.base.model.MongoDbo;
import org.anvilpowered.datasync.api.model.member.Member;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity("members")
public class MongoMember extends MongoDbo implements Member<ObjectId> {

    private UUID userUUID;
    private boolean skipDeserialization;
    private List<ObjectId> snapshotIds;

    @Override
    public UUID getUserUUID() {
        return userUUID;
    }

    @Override
    public void setUserUUID(UUID userUUID) {
        this.userUUID = userUUID;
    }

    @Override
    public List<ObjectId> getSnapshotIds() {
        if (snapshotIds == null) {
            snapshotIds = new ArrayList<>();
        }
        return snapshotIds;
    }

    @Override
    public void setSnapshotIds(List<ObjectId> snapshotIds) {
        this.snapshotIds = Preconditions.checkNotNull(snapshotIds, "snapshotIds");
    }

    @Override
    public boolean isSkipDeserialization() {
        return skipDeserialization;
    }

    @Override
    public void setSkipDeserialization(boolean skipDeserialization) {
        this.skipDeserialization = skipDeserialization;
    }
}
