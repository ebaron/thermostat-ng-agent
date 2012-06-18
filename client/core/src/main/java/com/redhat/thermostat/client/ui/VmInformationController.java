/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.Component;
import java.util.Collection;

import com.redhat.thermostat.client.internal.UiFacadeFactory;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.osgi.service.VmInformationService;
import com.redhat.thermostat.client.osgi.service.VmInformationServiceController;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.VmRef;

public class VmInformationController {

    private final VmInformationView view;

    private final VmOverviewController overviewController;
    private final VmCpuController cpuController;
    private final VmGcController gcController;

    public VmInformationController(UiFacadeFactory uiFacadeFactory, VmRef vmRef) {
        overviewController = new VmOverviewController(vmRef);
        cpuController = new VmCpuController(vmRef);
        gcController = new VmGcController(vmRef);

        view = ApplicationContext.getInstance().getViewFactory().getView(VmInformationView.class);

        view.addChildView(localize(LocaleResources.VM_INFO_TAB_OVERVIEW), overviewController.getComponent());
        view.addChildView(localize(LocaleResources.VM_INFO_TAB_CPU), cpuController.getComponent());
        view.addChildView(localize(LocaleResources.VM_INFO_TAB_GC), gcController.getComponent());

        Collection<VmInformationService> vmInfoServices = uiFacadeFactory.getVmInformationServices();
        for (VmInformationService vmInfoService : vmInfoServices) {
            VmInformationServiceController ctrl = vmInfoService.getInformationServiceController(vmRef);
            String name = ctrl.getLocalizedName();
            Component viewComp = ctrl.getComponent();
            view.addChildView(name, viewComp);
        }
    }

    public Component getComponent() {
        return view.getUiComponent();
    }

    public int getSelectedChildID() {
        return view.getSelectedChildID();
    }

    public void selectChildID(int id) {
        view.selectChildID(id);
    }

    public int getNumChildren() {
        return view.getNumChildren();
    }
}