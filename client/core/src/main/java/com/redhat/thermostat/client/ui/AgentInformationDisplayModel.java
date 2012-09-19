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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.BackendInfoDAO;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.model.AgentInformation;
import com.redhat.thermostat.common.model.BackendInformation;

/**
 * This model sits between the current view and the remote model, and maintains
 * a state of the agents that it knows about.
 */
public class AgentInformationDisplayModel {

    private final AgentInfoDAO agentInfoDao;
    private final BackendInfoDAO backendInfoDao;

    private final List<AgentInformation> agents;
    private final Map<String, List<BackendInformation>> backends;

    public AgentInformationDisplayModel() {
        ApplicationContext appContext = ApplicationContext.getInstance();
        DAOFactory daoFactory = appContext.getDAOFactory();
        agentInfoDao = daoFactory.getAgentInfoDAO();
        backendInfoDao = daoFactory.getBackendInfoDAO();

        agents = new ArrayList<>();
        backends = new HashMap<>();

        refresh();
    }

    public Collection<AgentInformation> getAgents() {
        return agents;
    }

    public AgentInformation getAgentInfo(String agentId) {
        for (AgentInformation agent : agents) {
            if (agent.getAgentId().equals(agentId)) {
                return agent;
            }
        }
        return null;
    }

    public Collection<BackendInformation> getBackends(String agentId) {
        return backends.get(agentId);
    }

    public void refresh() {
        agents.clear();
        agents.addAll(agentInfoDao.getAllAgentInformation());
        backends.clear();
        for (AgentInformation agent : agents) {
            String agentId = agent.getAgentId();
            HostRef agentRef = new HostRef(agentId, agentId);
            backends.put(agentId, backendInfoDao.getBackendInformation(agentRef));
        }
    }

}