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

package com.redhat.thermostat.numa.agent.internal;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.OS;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendService;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.storage.core.WriterID;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Activator implements BundleActivator {

    private static final Logger logger = LoggingUtils.getLogger(Activator.class);

    private MultipleServiceTracker tracker;
    private NumaBackend backend;
    private ServiceRegistration<Backend> reg;
    
    @Override
    public void start(final BundleContext context) throws Exception {

        Class<?>[] deps = new Class<?>[] {
                BackendService.class,
                NumaDAO.class,
                ApplicationService.class,
                WriterID.class, // numa backend uses it
        };
        tracker = new MultipleServiceTracker(context, deps, new Action() {
            
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                ApplicationService appService = services.get(ApplicationService.class);
                NumaDAO numaDAO = services.get(NumaDAO.class);
                Version version = new Version(context.getBundle());
                WriterID writerId = services.get(WriterID.class);
                NumaCollector collector = OS.IS_LINUX ? new NumaLinuxCollectorImpl() : new NumaCollectorImpl();
                backend = new NumaBackend(appService, numaDAO, collector, version, writerId);
                reg = context.registerService(Backend.class, backend, null);
                if (!OS.IS_LINUX) {
                    logger.log(Level.WARNING, "NUMA backend is not yet ported to Windows");
                }
            }

            @Override
            public void dependenciesUnavailable() {
                if (backend.isActive()) {
                    backend.deactivate();
                }
                reg.unregister();
            }
        });
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }
    
    /*
     * For testing purposes only.
     */
    NumaBackend getBackend() {
        return backend;
    }
}

