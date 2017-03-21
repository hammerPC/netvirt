/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netvirt.vpnmanager;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.netvirt.vpnmanager.utilities.InterfaceUtils;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceStateChangeListener
    extends AsyncDataTreeChangeListenerBase<Interface, InterfaceStateChangeListener> implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceStateChangeListener.class);
    private final DataBroker dataBroker;
    private final VpnInterfaceManager vpnInterfaceManager;

    public InterfaceStateChangeListener(final DataBroker dataBroker, final VpnInterfaceManager vpnInterfaceManager) {
        super(Interface.class, InterfaceStateChangeListener.class);
        this.dataBroker = dataBroker;
        this.vpnInterfaceManager = vpnInterfaceManager;
    }

    public void start() {
        LOG.info("{} start", getClass().getSimpleName());
        registerListener(LogicalDatastoreType.OPERATIONAL, dataBroker);
    }


    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    protected InterfaceStateChangeListener getDataTreeChangeListener() {
        return InterfaceStateChangeListener.this;
    }


    @Override
    // TODO Clean up the exception handling
    @SuppressWarnings("checkstyle:IllegalCatch")
    protected void add(InstanceIdentifier<Interface> identifier, Interface intrf) {
        try {
            if (intrf != null && (intrf.getType() != null) && !intrf.getType().equals(Tunnel.class)) {
                DataStoreJobCoordinator dataStoreCoordinator = DataStoreJobCoordinator.getInstance();
                dataStoreCoordinator.enqueueJob("VPNINTERFACE-" + intrf.getName(),
                    () -> {
                        WriteTransaction writeConfigTxn = dataBroker.newWriteOnlyTransaction();
                        WriteTransaction writeOperTxn = dataBroker.newWriteOnlyTransaction();
                        WriteTransaction writeInvTxn = dataBroker.newWriteOnlyTransaction();
                        List<ListenableFuture<Void>> futures = new ArrayList<>();

                        final String interfaceName = intrf.getName();
                        LOG.info("Detected interface add event for interface {}", interfaceName);
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                                .interfaces.Interface configInterface =
                                InterfaceUtils.getInterface(dataBroker, interfaceName);
                        if (configInterface != null && (configInterface.getType() != null)
                                && !configInterface.getType().equals(Tunnel.class)) {
                            // We service only VM interfaces and Router interfaces here.
                            // We donot service Tunnel Interfaces here.
                            // Tunnel events are directly serviced
                            // by TunnelInterfacesStateListener present as part of VpnInterfaceManager
                            LOG.debug("Config Interface Name {}", configInterface.getName());
                            final VpnInterface vpnInterface =
                                    VpnUtil.getConfiguredVpnInterface(dataBroker, interfaceName);
                            if (vpnInterface != null) {
                                LOG.debug("VPN Interface Name {}", vpnInterface);
                                BigInteger intfDpnId = BigInteger.ZERO;
                                try {
                                    intfDpnId = InterfaceUtils.getDpIdFromInterface(intrf);
                                } catch (Exception e) {
                                    LOG.error("Unable to retrieve dpnId for interface {}. "
                                            + "Process vpn interface add failed",intrf.getName(), e);
                                    return futures;
                                }
                                final BigInteger dpnId = intfDpnId;
                                final int ifIndex = intrf.getIfIndex();
                                vpnInterfaceManager.processVpnInterfaceUp(dpnId, vpnInterface, ifIndex, false,
                                        writeConfigTxn, writeOperTxn, writeInvTxn, intrf);
                                ListenableFuture<Void> operFuture = writeOperTxn.submit();
                                try {
                                    operFuture.get();
                                } catch (ExecutionException e) {
                                    LOG.error("InterfaceStateChange - Exception encountered while submitting"
                                            + " operational future for addVpnInterface {} : {}",
                                            vpnInterface.getName(), e);
                                    return null;
                                }
                                futures.add(writeConfigTxn.submit());
                                futures.add(writeInvTxn.submit());
                            }
                        } else {
                            LOG.debug("Unable to process add for interface {} ,"
                                    + "since Interface ConfigDS entry absent for the same", interfaceName);
                        }

                        return futures;
                    });
            }
        } catch (Exception e) {
            LOG.error("Exception caught in Interface {} Operational State Up event", intrf.getName(), e);
        }
    }

    @Override
    // TODO Clean up the exception handling
    @SuppressWarnings("checkstyle:IllegalCatch")
    protected void remove(InstanceIdentifier<Interface> identifier, Interface intrf) {
        final String interfaceName = intrf.getName();
        LOG.info("Detected interface remove event for interface {}", interfaceName);
        try {
            if (intrf != null && (intrf.getType() != null) && !intrf.getType().equals(Tunnel.class)) {
                DataStoreJobCoordinator dataStoreCoordinator = DataStoreJobCoordinator.getInstance();
                dataStoreCoordinator.enqueueJob("VPNINTERFACE-" + interfaceName,
                    () -> {
                        WriteTransaction writeOperTxn = dataBroker.newWriteOnlyTransaction();
                        WriteTransaction writeConfigTxn = dataBroker.newWriteOnlyTransaction();
                        WriteTransaction writeInvTxn = dataBroker.newWriteOnlyTransaction();
                        List<ListenableFuture<Void>> futures = new ArrayList<>();

                        BigInteger dpId;
                        InstanceIdentifier<VpnInterface> id = VpnUtil.getVpnInterfaceIdentifier(interfaceName);
                        Optional<VpnInterface> optVpnInterface =
                                VpnUtil.read(dataBroker, LogicalDatastoreType.OPERATIONAL, id);
                        if (optVpnInterface.isPresent()) {
                            final VpnInterface vpnInterface = optVpnInterface.get();
                            try {
                                dpId = InterfaceUtils.getDpIdFromInterface(intrf);
                            } catch (Exception e) {
                                LOG.error("Unable to retrieve dpnId from interface operational data store for interface"
                                        + " {}. Fetching from vpn interface op data store. ", interfaceName, e);
                                dpId = vpnInterface.getDpnId();
                            }
                            final BigInteger dpnId = dpId;
                            final int ifIndex = intrf.getIfIndex();
                            vpnInterfaceManager.processVpnInterfaceDown(dpnId, interfaceName, ifIndex, false, false,
                                    writeConfigTxn, writeOperTxn, writeInvTxn, intrf);
                            ListenableFuture<Void> operFuture = writeOperTxn.submit();
                            try {
                                operFuture.get();
                            } catch (ExecutionException e) {
                                LOG.error("InterfaceStateChange - Exception encountered while submitting operational"
                                        + " future for removeVpnInterface {} : {}", vpnInterface.getName(), e);
                                return null;
                            }
                            futures.add(writeConfigTxn.submit());
                            futures.add(writeInvTxn.submit());
                        } else {
                            LOG.debug("Interface {} is not a vpninterface, ignoring.", interfaceName);
                        }

                        return futures;
                    });
            }
        } catch (Exception e) {
            LOG.error("Exception observed in handling deletion of VPN Interface {}. ", interfaceName, e);
        }
    }

    // TODO Clean up the exception handling
    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    protected void update(InstanceIdentifier<Interface> identifier,
        Interface original, Interface update) {
        final String interfaceName = update.getName();
        LOG.info("Detected interface update event for interface {}", interfaceName);
        LOG.trace("Detected interface update event for interface {} - Old: {}, New: {}",
                interfaceName, original, update);
        try {
            OperStatus originalOperStatus = original.getOperStatus();
            OperStatus updateOperStatus = update.getOperStatus();
            if (originalOperStatus.equals(Interface.OperStatus.Unknown)
                  || updateOperStatus.equals(Interface.OperStatus.Unknown)) {
                LOG.debug("Interface {} state change is from/to null/UNKNOWN. Ignoring the update event.",
                        interfaceName);
                return;
            }

            if (update.getIfIndex() == null) {
                return;
            }
            if (update != null && (update.getType() != null) && !update.getType().equals(Tunnel.class)) {
                DataStoreJobCoordinator dataStoreCoordinator = DataStoreJobCoordinator.getInstance();
                dataStoreCoordinator.enqueueJob("VPNINTERFACE-" + interfaceName,
                    () -> {
                        WriteTransaction writeConfigTxn = dataBroker.newWriteOnlyTransaction();
                        WriteTransaction writeOperTxn = dataBroker.newWriteOnlyTransaction();
                        WriteTransaction writeInvTxn = dataBroker.newWriteOnlyTransaction();
                        List<ListenableFuture<Void>> futures = new ArrayList<>();

                        final VpnInterface vpnInterface =
                                VpnUtil.getConfiguredVpnInterface(dataBroker, interfaceName);
                        if (vpnInterface != null) {
                            final int ifIndex = update.getIfIndex();
                            final BigInteger dpnId = InterfaceUtils.getDpIdFromInterface(update);
                            if (update.getOperStatus().equals(Interface.OperStatus.Up)) {
                                vpnInterfaceManager.processVpnInterfaceUp(dpnId, vpnInterface, ifIndex,
                                        true, writeConfigTxn, writeOperTxn, writeInvTxn, update);
                            } else if (update.getOperStatus().equals(Interface.OperStatus.Down)) {
                                vpnInterfaceManager.processVpnInterfaceDown(dpnId, interfaceName, ifIndex, true,
                                        false, writeConfigTxn, writeOperTxn, writeInvTxn, update);
                            }
                            ListenableFuture<Void> operFuture = writeOperTxn.submit();
                            try {
                                operFuture.get();
                            } catch (ExecutionException e) {
                                LOG.error("InterfaceStateChange - Exception encountered while submitting operational"
                                        + " future for updateVpnInterface {} : {}", vpnInterface.getName(), e);
                                return null;
                            }
                            futures.add(writeConfigTxn.submit());
                            futures.add(writeInvTxn.submit());
                        } else {
                            LOG.debug("Interface {} is not a vpninterface, ignoring.", interfaceName);
                        }

                        return futures;
                    });
            }
        } catch (Exception e) {
            LOG.error("Exception observed in handling updation of VPN Interface {}. ", update.getName(), e);
        }
    }
}
