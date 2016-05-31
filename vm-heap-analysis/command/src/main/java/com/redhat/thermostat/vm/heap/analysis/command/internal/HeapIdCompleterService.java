/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import com.redhat.thermostat.common.cli.AbstractCompleterService;
import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.TabCompleter;
import com.redhat.thermostat.common.cli.CompletionFinderTabCompleter;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HeapIdCompleterService extends AbstractCompleterService {

    @Override
    public Set<String> getCommands() {
        return new HashSet<>(Arrays.asList(
                FindObjectsCommand.COMMAND_NAME,
                FindRootCommand.COMMAND_NAME,
                ObjectInfoCommand.COMMAND_NAME,
                SaveHeapDumpToFileCommand.COMMAND_NAME,
                ShowHeapHistogramCommand.COMMAND_NAME
        ));
    }

    @Override
    public Map<CliCommandOption, TabCompleter> getOptionCompleters() {
        CliCommandOption heapOption = new CliCommandOption("h", "heapId", true, "Heap ID", false);
        TabCompleter heapCompleter = new CompletionFinderTabCompleter(new HeapIdsFinder(dependencyServices));

        return Collections.singletonMap(heapOption, heapCompleter);
    }

    void setHeapDAO(HeapDAO heapDao) {
        setService(HeapDAO.class, heapDao);
    }

    void setVmInfoDAO(VmInfoDAO vmInfoDao) {
        setService(VmInfoDAO.class, vmInfoDao);
    }

}