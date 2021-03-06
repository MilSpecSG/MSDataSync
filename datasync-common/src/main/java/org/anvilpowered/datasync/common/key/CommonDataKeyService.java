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

package org.anvilpowered.datasync.common.key;

import org.anvilpowered.datasync.api.key.DataKeyService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class CommonDataKeyService<K> implements DataKeyService<K> {

    private Map<K, String> nameMap;

    public CommonDataKeyService() {
        nameMap = new HashMap<>();
    }

    @Override
    public void addMapping(K key, String name) {
        if (key != null && name != null) nameMap.put(key, name);
    }

    @Override
    public void removeMapping(K key) {
        nameMap.remove(key);
    }

    @Override
    public Optional<String> getName(K key) {
        return Optional.ofNullable(nameMap.get(key));
    }
}
