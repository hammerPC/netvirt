/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netvirt.neutronvpn.shell;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker.syncReadOptional;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.LearntVpnVipToPortData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.learnt.vpn.vip.to.port.data.LearntVpnVipToPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.learnt.vpn.vip.to.port.data.LearntVpnVipToPortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.NeutronVpnPortipPortData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.neutron.vpn.portip.port.data.VpnPortipToPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.neutronvpn.rev150602.neutron.vpn.portip.port.data.VpnPortipToPortKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "vpnservice", name = "vpn-ipaddress-to-port-show",
    description = "Displays the IPAddresses and Ports on which they are configured (or) discovered for "
        + "VPN Instance(s)")
public class ShowVpnIpToPort extends OsgiCommandSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ShowVpnIpToPort.class);

    @Argument(index = 0, name = "--vpn-name", description = "Name of the Vpn Instance", required = false,
        multiValued = false)
    private String vpnName;
    @Argument(index = 1, name = "--ip-address", description = "Ip address to search for in the Vpn Instance",
        required = false, multiValued = false)
    private String portFixedIp;

    private DataBroker dataBroker;
    private List<VpnPortipToPort> vpnPortipToPortList = new ArrayList<>();
    private List<LearntVpnVipToPort> vpnVipToPortList = new ArrayList<>();

    public void setDataBroker(DataBroker broker) {
        this.dataBroker = broker;
    }

    @Override
    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    protected Object doExecute() throws Exception {
        if (vpnName == null && portFixedIp == null) {
            getNeutronVpnPort();
            getLearntVpnVipPort();
            System.out.println(vpnPortipToPortList.size() + " Entries are present: ");
            System.out.println("-----------------------------------------------------------------------");
            System.out.println(String.format("             %s   %24s   %20s   %32s", "VpnName", "IPAddress",
                "MacAddress", "Port"));
            System.out.println("-----------------------------------------------------------------------");
            for (VpnPortipToPort vpnPortipToPort : vpnPortipToPortList) {
                System.out.println(String.format("  %-32s  %-16s  %-16s  %-32s", vpnPortipToPort.getVpnName(),
                        vpnPortipToPort.getPortFixedip(),
                        vpnPortipToPort.getMacAddress(),
                        vpnPortipToPort.getPortName()));
            }
            for (LearntVpnVipToPort learntVpnVipToPort : vpnVipToPortList) {
                System.out.println(String.format("* %-32s  %-16s  %-16s  %-32s", learntVpnVipToPort.getVpnName(),
                        learntVpnVipToPort.getPortFixedip(),
                        learntVpnVipToPort.getMacAddress(),
                        learntVpnVipToPort.getPortName()));
            }
            System.out.println("\n * prefixed entries are Learned.");
            System.out.println("\n" + getshowVpnCLIHelp());
        } else if (portFixedIp == null || vpnName == null) {
            System.out.println("Insufficient arguments"
                + "\nCorrect Usage : neutronvpn-port-show [<vpnName> <portFixedIp>]");
        } else {
            InstanceIdentifier<VpnPortipToPort> id =
                InstanceIdentifier.builder(NeutronVpnPortipPortData.class)
                    .child(VpnPortipToPort.class, new VpnPortipToPortKey(portFixedIp, vpnName)).build();
            Optional<VpnPortipToPort> vpnPortipToPortData = syncReadOptional(dataBroker, CONFIGURATION, id);
            if (vpnPortipToPortData.isPresent()) {
                VpnPortipToPort data = vpnPortipToPortData.get();
                System.out.println("\n----------"
                    + "---------------------------------------------------------------------------------");
                System.out.println("VpnName:   " + data.getVpnName() + "\nIPAddress: " + data.getPortFixedip()
                    + "\nMacAddress: " + data.getMacAddress() + "\nPort: " + data.getPortName());
                System.out.println("\n----------"
                    + "---------------------------------------------------------------------------------");
            } else {
                InstanceIdentifier<LearntVpnVipToPort> learntId =
                    InstanceIdentifier.builder(LearntVpnVipToPortData.class)
                        .child(LearntVpnVipToPort.class, new LearntVpnVipToPortKey(portFixedIp, vpnName)).build();
                Optional<LearntVpnVipToPort> learntVpnVipToPortData =
                        syncReadOptional(dataBroker, OPERATIONAL, learntId);
                if (!learntVpnVipToPortData.isPresent()) {
                    System.out.println("Data not available");
                    return null;
                }
                LearntVpnVipToPort data = learntVpnVipToPortData.get();
                System.out.println("\n----------"
                    + "---------------------------------------------------------------------------------");
                System.out.println("VpnName: * " + data.getVpnName() + "\nIPAddress: " + data.getPortFixedip()
                    + "\nMacAddress: " + data.getMacAddress() + "\nPort: " + data.getPortName());
                System.out.println("\n----------"
                    + "---------------------------------------------------------------------------------");
            }
            System.out.println("\n" + getshowVpnCLIHelp());
        }
        return null;
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    private void getNeutronVpnPort() throws ReadFailedException {
        InstanceIdentifier<NeutronVpnPortipPortData> neutronVpnPortipPortDataIdentifier = InstanceIdentifier
                .builder(NeutronVpnPortipPortData.class).build();
        Optional<NeutronVpnPortipPortData> optionalNeutronVpnPort = syncReadOptional(dataBroker, CONFIGURATION,
                neutronVpnPortipPortDataIdentifier);
        if (!optionalNeutronVpnPort.isPresent()) {
            System.out.println("No NeutronVpnPortIpToPortData configured.");
        } else {
            vpnPortipToPortList = optionalNeutronVpnPort.get().getVpnPortipToPort();
        }
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    private void getLearntVpnVipPort() throws ReadFailedException {
        InstanceIdentifier<LearntVpnVipToPortData> learntVpnVipPortDataIdentifier = InstanceIdentifier
                .builder(LearntVpnVipToPortData.class).build();
        Optional<LearntVpnVipToPortData> optionalLearntVpnPort = syncReadOptional(dataBroker, OPERATIONAL,
                learntVpnVipPortDataIdentifier);
        if (!optionalLearntVpnPort.isPresent()) {
            System.out.println("No LearntVpnVipToPortData discovered.");
        } else {
            vpnVipToPortList = optionalLearntVpnPort.get().getLearntVpnVipToPort();
        }
    }

    private String getshowVpnCLIHelp() {
        StringBuilder help = new StringBuilder("Usage:");
        help.append("To display ports and their associated vpn instances "
            + "neutronvpn-port-show [<vpnName> <portFixedIp>].\n");
        return help.toString();
    }
}
