/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.cpu.agent.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendID;
import com.redhat.thermostat.backend.BackendsProperties;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.utils.ProcDataSource;
import com.redhat.thermostat.utils.SysConf;
import com.redhat.thermostat.vm.cpu.common.VmCpuStatDAO;
import com.redhat.thermostat.vm.cpu.common.model.VmCpuStat;

public class VmCpuBackend extends Backend implements VmStatusListener {

    private static final Logger LOGGER = LoggingUtils.getLogger(VmCpuBackend.class);
    static final long PROC_CHECK_INTERVAL = 1000; // TODO make this configurable.

    private VmCpuStatBuilder vmCpuStatBuilder;
    private VmCpuStatDAO vmCpuStats;
    private ScheduledExecutorService executor;
    private VmStatusListenerRegistrar registrar;
    private boolean started;

    private final List<Integer> pidsToMonitor = new CopyOnWriteArrayList<>();

    public VmCpuBackend(ScheduledExecutorService executor, VmCpuStatDAO vmCpuStatDao, Version version,
            VmStatusListenerRegistrar registrar) {
        super(new BackendID("VM CPU Backend", VmCpuBackend.class.getName()));
        this.executor = executor;
        this.vmCpuStats = vmCpuStatDao;
        this.registrar = registrar;
        
        setConfigurationValue(BackendsProperties.VENDOR.name(), "Red Hat, Inc.");
        setConfigurationValue(BackendsProperties.DESCRIPTION.name(), "Gathers CPU statistics about a JVM");
        setConfigurationValue(BackendsProperties.VERSION.name(), version.getVersionNumber());
        
        Clock clock = new SystemClock();
        long ticksPerSecond = SysConf.getClockTicksPerSecond();
        ProcDataSource source = new ProcDataSource();
        ProcessStatusInfoBuilder builder = new ProcessStatusInfoBuilder(new ProcDataSource());
        int numCpus = getCpuCount(source);
        vmCpuStatBuilder = new VmCpuStatBuilder(clock, numCpus, ticksPerSecond, builder);
    }

    @Override
    public boolean activate() {
        if (!started) {
            registrar.register(this);

            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    for (Integer pid : pidsToMonitor) {
                        if (vmCpuStatBuilder.knowsAbout(pid)) {
                            VmCpuStat dataBuilt = vmCpuStatBuilder.build(pid);
                            if (dataBuilt != null) {
                                vmCpuStats.putVmCpuStat(dataBuilt);
                            }
                        } else {
                            vmCpuStatBuilder.learnAbout(pid);
                        }
                    }
                }
            }, 0, PROC_CHECK_INTERVAL, TimeUnit.MILLISECONDS);

            started = true;
        }
        return started;
    }

    @Override
    public boolean deactivate() {
        if (started) {
            executor.shutdown();
            registrar.unregister(this);

            started = false;
        }
        return !started;
    }
    
    @Override
    public boolean isActive() {
        return started;
    }

    @Override
    public String getConfigurationValue(String key) {
        return null;
    }

    @Override
    public boolean attachToNewProcessByDefault() {
        return true;
    }

    @Override
    public int getOrderValue() {
        return ORDER_CPU_GROUP + 50;
    }

    private int getCpuCount(ProcDataSource dataSource) {
        final String KEY_PROCESSOR_ID = "processor";
        int cpuCount = 0;
        try (BufferedReader bufferedReader = new BufferedReader(dataSource.getCpuInfoReader())) {
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(KEY_PROCESSOR_ID)) {
                    cpuCount++;
                }
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Unable to read cpu info");
        }
        
        return cpuCount;
    }

    /*
     * Methods implementing VmStatusListener
     */
    @Override
    public void vmStatusChanged(Status newStatus, int pid) {
        switch (newStatus) {
        case VM_STARTED:
            /* fall-through */
        case VM_ACTIVE:
            pidsToMonitor.add(pid);
            break;
        case VM_STOPPED:
            // the cast is important because it changes the call from remove(index) to remove(Object)
            pidsToMonitor.remove((Integer) pid);
            vmCpuStatBuilder.forgetAbout(pid);
            break;
        }

    }
    
    /*
     * For testing purposes only.
     */
    void setVmCpuStatBuilder(VmCpuStatBuilder vmCpuStatBuilder) {
        this.vmCpuStatBuilder = vmCpuStatBuilder;
    }

}
