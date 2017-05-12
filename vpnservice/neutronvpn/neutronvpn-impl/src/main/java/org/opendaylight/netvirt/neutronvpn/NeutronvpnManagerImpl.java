/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netvirt.neutronvpn;

import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.netvirt.neutronvpn.interfaces.INeutronVpnManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev150712.subnets.attributes.subnets.Subnet;

@Singleton
public class NeutronvpnManagerImpl implements INeutronVpnManager {

    private NeutronvpnManager nvManager;

    @Inject
    public NeutronvpnManagerImpl(final NeutronvpnManager neutronvpnManager) {
        this.nvManager = neutronvpnManager;
    }

    @Override
    public List<String> showNeutronPortsCLI() {
        return nvManager.showNeutronPortsCLI();
    }

    @Override
    public Network getNeutronNetwork(Uuid networkId) {
        return nvManager.getNeutronNetwork(networkId);
    }

    @Override
    public List<String> showVpnConfigCLI(Uuid vuuid) {
        return nvManager.showVpnConfigCLI(vuuid);
    }

    @Override
    public void addSubnetToVpn(Uuid vpnId, Uuid subnet) {
        nvManager.addSubnetToVpn(vpnId, subnet);
    }

    @Override
    public void removeSubnetFromVpn(Uuid vpnId, Uuid subnet) {
        nvManager.removeSubnetFromVpn(vpnId, subnet);
    }

    @Override
    public Uuid getNetworkForSubnet(Uuid subnetId) {
        return nvManager.getNetworkForSubnet(subnetId);
    }

    @Override
    public List<Uuid> getNetworksForVpn(Uuid vpnId) {
        return nvManager.getNetworksForVpn(vpnId);
    }

    @Override
    public Port getNeutronPort(String name) {
        return nvManager.getNeutronPort(name);
    }

    @Override
    public Port getNeutronPort(Uuid portId) {
        return nvManager.getNeutronPort(portId);
    }

    @Override
    public Subnet getNeutronSubnet(Uuid subnetId) {
        return nvManager.getNeutronSubnet(subnetId);
    }

    @Override
    public IpAddress getNeutronSubnetGateway(Uuid subnetId) {
        return nvManager.getNeutronSubnetGateway(subnetId);
    }

    @Override
    public Collection<Uuid> getSubnetIdsForGatewayIp(IpAddress ipAddress) {
        return  NeutronvpnUtils.getSubnetIdsForGatewayIp(ipAddress);
    }

    @Override
    public String getOpenDaylightVniRangesConfig() {
        return nvManager.getOpenDaylightVniRangesConfig();
    }
}
