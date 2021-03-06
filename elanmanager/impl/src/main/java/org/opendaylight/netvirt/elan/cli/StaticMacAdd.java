/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netvirt.elan.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.netvirt.elanmanager.api.IElanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "staticMac", name = "add", description = "adding Static Mac Address")
public class StaticMacAdd extends OsgiCommandSupport {

    @Argument(index = 0, name = "elanName", description = "ELAN-NAME", required = true, multiValued = false)
    private String elanName;
    @Argument(index = 1, name = "interfaceName", description = "InterfaceName", required = true, multiValued = false)
    private String interfaceName;
    @Argument(index = 2, name = "staticMacAddress", description = "StaticMacAddress", required = true,
            multiValued = false)
    private String staticMacAddress;
    private static final Logger LOG = LoggerFactory.getLogger(StaticMacAdd.class);
    private IElanService elanProvider;

    public void setElanProvider(IElanService elanServiceProvider) {
        this.elanProvider = elanServiceProvider;
    }

    @Override
    protected Object doExecute() {
        LOG.debug("Executing static mac add command for elanName:{}, interfaceName:{}, staticMacAddress:{}",
                elanName, interfaceName, staticMacAddress);
        elanProvider.addStaticMacAddress(interfaceName, staticMacAddress);
        return null;
    }
}
