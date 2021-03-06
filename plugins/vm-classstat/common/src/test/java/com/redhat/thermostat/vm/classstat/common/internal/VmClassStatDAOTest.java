/*
 * Copyright 2012-2017 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.vm.classstat.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;
import com.redhat.thermostat.vm.classstat.common.model.VmClassStat;

public class VmClassStatDAOTest {

    private static final Long TIMESTAMP = 1234L;
    private static final String VM_ID = "vmId";
    private static final Long LOADED_CLASSES = 12345L;
    private static final Long LOADED_BYTES = 2345L;
    private static final Long UNLOADED_CLASSES = 3456L;
    private static final Long UNLOADED_BYTES = 4567L;
    private static final Long CLASS_LOAD_TIME = 5678L;

    @Test
    public void testStatementDescriptorsAreSane() {
        String addVmClassStat = "ADD vm-class-stats SET 'agentId' = ?s , " +
                                                "'vmId' = ?s , " +
                                                "'timeStamp' = ?l , " +
                                                "'loadedClasses' = ?l , " +
                                                "'loadedBytes' = ?l , " +
                                                "'unloadedClasses' = ?l , " +
                                                "'unloadedBytes' = ?l , " +
                                                "'classLoadTime' = ?l";

        assertEquals(addVmClassStat, VmClassStatDAOImpl.DESC_ADD_VM_CLASS_STAT);
    }
    
    @Test
    public void testCategory() {
        assertEquals("vm-class-stats", VmClassStatDAO.vmClassStatsCategory.getName());
        Collection<Key<?>> keys = VmClassStatDAO.vmClassStatsCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<Integer>("vmId")));
        assertTrue(keys.contains(new Key<Long>("timeStamp")));
        assertTrue(keys.contains(new Key<Long>("loadedClasses")));
        assertTrue(keys.contains(new Key<Long>("loadedBytes")));
        assertTrue(keys.contains(new Key<Long>("unloadedClasses")));
        assertTrue(keys.contains(new Key<Long>("unloadedBytes")));
        assertTrue(keys.contains(new Key<Long>("classLoadTime")));
        assertEquals(8, keys.size());
    }

    @Test
    public void testGetLatestClassStatsBasic() throws DescriptorParsingException, StatementExecutionException {

        VmClassStat vmClassStat = getClassStat();

        @SuppressWarnings("unchecked")
        Cursor<VmClassStat> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(vmClassStat);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<VmClassStat> stmt = (PreparedStatement<VmClassStat>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getHostRef()).thenReturn(hostRef);
        when(vmRef.getVmId()).thenReturn(VM_ID);

        VmClassStatDAO dao = new VmClassStatDAOImpl(storage);
        List<VmClassStat> vmClassStats = dao.getLatestClassStats(vmRef, Long.MIN_VALUE);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "system");
        verify(stmt).setString(1, VM_ID);
        verify(stmt).setLong(2, Long.MIN_VALUE);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(1, vmClassStats.size());
        VmClassStat stat = vmClassStats.get(0);
        assertEquals(TIMESTAMP, (Long) stat.getTimeStamp());
        assertEquals(LOADED_CLASSES, (Long) stat.getLoadedClasses());
        assertEquals(VM_ID, stat.getVmId());
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<VmClassStat> anyDescriptor() {
        return (StatementDescriptor<VmClassStat>) any(StatementDescriptor.class);
    }

    private VmClassStat getClassStat() {
        return new VmClassStat("foo-agent", VM_ID, TIMESTAMP,
                LOADED_CLASSES, LOADED_BYTES,
                UNLOADED_CLASSES, UNLOADED_BYTES,
                CLASS_LOAD_TIME);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutVmClassStat() throws DescriptorParsingException, StatementExecutionException {

        Storage storage = mock(Storage.class);
        PreparedStatement<VmClassStat> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);

        VmClassStat stat = new VmClassStat("foo-agent", VM_ID, TIMESTAMP,
                LOADED_CLASSES, LOADED_BYTES, UNLOADED_CLASSES, UNLOADED_BYTES, CLASS_LOAD_TIME);
        VmClassStatDAO dao = new VmClassStatDAOImpl(storage);
        dao.putVmClassStat(stat);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(VmClassStatDAOImpl.DESC_ADD_VM_CLASS_STAT, desc.getDescriptor());

        verify(add).setString(0, stat.getAgentId());
        verify(add).setString(1, stat.getVmId());
        verify(add).setLong(2, stat.getTimeStamp());
        verify(add).setLong(3, stat.getLoadedClasses());
        verify(add).setLong(4, stat.getLoadedBytes());
        verify(add).setLong(5, stat.getUnloadedClasses());
        verify(add).setLong(6, stat.getUnloadedBytes());
        verify(add).setLong(7, stat.getClassLoadTime());
        verify(add).execute();
        verifyNoMoreInteractions(add);
    }
}

