/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.neo4j.memory.MemoryPools.NO_TRACKING;

class LocalMemoryTrackerTest
{
    @Test
    void trackDirectMemoryAllocations()
    {
        LocalMemoryTracker memoryTracker = new LocalMemoryTracker();
        memoryTracker.allocateNative( 10 );
        memoryTracker.allocateNative( 20 );
        memoryTracker.allocateNative( 40 );
        assertEquals( 70, memoryTracker.usedNativeMemory());
    }

    @Test
    void trackDirectMemoryDeallocations()
    {
        LocalMemoryTracker memoryTracker = new LocalMemoryTracker();
        memoryTracker.allocateNative( 100 );
        assertEquals( 100, memoryTracker.usedNativeMemory() );

        memoryTracker.releaseNative( 20 );
        assertEquals( 80, memoryTracker.usedNativeMemory() );

        memoryTracker.releaseNative( 40 );
        assertEquals( 40, memoryTracker.usedNativeMemory() );
    }

    @Test
    void trackHeapMemoryAllocations()
    {
        LocalMemoryTracker memoryTracker = new LocalMemoryTracker();
        memoryTracker.allocateHeap( 10 );
        memoryTracker.allocateHeap( 20 );
        memoryTracker.allocateHeap( 40 );
        assertEquals( 70, memoryTracker.estimatedHeapMemory() );
    }

    @Test
    void trackHeapMemoryDeallocations()
    {
        LocalMemoryTracker memoryTracker = new LocalMemoryTracker();
        memoryTracker.allocateHeap( 100 );
        assertEquals( 100, memoryTracker.estimatedHeapMemory() );

        memoryTracker.releaseHeap( 20 );
        assertEquals( 80, memoryTracker.estimatedHeapMemory() );

        memoryTracker.releaseHeap( 40 );
        assertEquals( 40, memoryTracker.estimatedHeapMemory() );
    }

    @Test
    void throwsOnLimit()
    {
        LocalMemoryTracker memoryTracker = new LocalMemoryTracker( NO_TRACKING, 10, 0, "settingName" );
        MemoryLimitExceededException memoryLimitExceededException = assertThrows( MemoryLimitExceededException.class, () -> memoryTracker.allocateHeap( 100 ) );
        assertThat( memoryLimitExceededException.getMessage() ).contains( "settingName" );
    }
}
