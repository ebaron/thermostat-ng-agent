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

package com.redhat.thermostat.vm.heap.analysis.agent.internal;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;

class JMXHeapDumper {

    private MXBeanConnectionPool pool;
    
    JMXHeapDumper(MXBeanConnectionPool pool) {
        this.pool = pool;
    }

    void dumpHeap(int pid, String filename) throws HeapDumpException {
        try {
            MXBeanConnection connection = pool.acquire(pid);
            try {
                doHeapDump(connection, filename);
            } finally {
                pool.release(pid, connection);
            }
        } catch (Exception ex) {
            throw new HeapDumpException(ex);
        }
    }

    private void doHeapDump(MXBeanConnection connection, String filename) throws Exception {
        MBeanServerConnection mbsc = connection.get();
        mbsc.invoke(new ObjectName("com.sun.management:type=HotSpotDiagnostic"),
                "dumpHeap",
                new Object[] { filename, Boolean.TRUE },
                new String[] { String.class.getName(), boolean.class.getName() });
    }

}

