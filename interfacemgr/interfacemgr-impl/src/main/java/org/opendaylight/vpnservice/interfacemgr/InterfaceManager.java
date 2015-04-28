/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vpnservice.interfacemgr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.interfacemgr.rev150331.IfL3tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state._interface.StatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state._interface.Statistics;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import com.google.common.util.concurrent.FutureCallback;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.interfacemgr.rev150331.BaseIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.vpnservice.AbstractDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;

public class InterfaceManager extends AbstractDataChangeListener<Interface> implements AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceManager.class);
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private final DataBroker broker;
    private final Map<NodeConnectorId, String> mapNcToInterfaceName = new ConcurrentHashMap<>();

    private static final FutureCallback<Void> DEFAULT_CALLBACK =
                    new FutureCallback<Void>() {
                        public void onSuccess(Void result) {
                            LOG.debug("Success in Datastore write operation");
                        }

                        public void onFailure(Throwable error) {
                            LOG.error("Error in Datastore write operation", error);
                        };
                    };

    public InterfaceManager(final DataBroker db) {
        super(Interface.class);
        broker = db;
        registerListener(db);
    }

    @Override
    public void close() throws Exception {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("Error when cleaning up DataChangeListener.", e);
            }
            listenerRegistration = null;
        }
        LOG.info("Interface Manager Closed");
    }

    private void registerListener(final DataBroker db) {
        try {
            listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                    getWildCardPath(), InterfaceManager.this, DataChangeScope.SUBTREE);
        } catch (final Exception e) {
            LOG.error("InterfaceManager DataChange listener registration fail!", e);
            throw new IllegalStateException("InterfaceManager registration Listener failed.", e);
        }
    }

    @Override
    protected void add(final InstanceIdentifier<Interface> identifier,
            final Interface imgrInterface) {
        LOG.trace("key: " + identifier + ", value=" + imgrInterface );
        addInterface(identifier, imgrInterface);
    }

    private InstanceIdentifier<Interface> buildId(final InstanceIdentifier<Interface> identifier) {
        //TODO Make this generic and move to AbstractDataChangeListener or Utils.
        final InterfaceKey key = identifier.firstKeyOf(Interface.class, InterfaceKey.class);
        return buildId(key.getName());
    }

    private InstanceIdentifier<Interface> buildId(String interfaceName) {
        //TODO Make this generic and move to AbstractDataChangeListener or Utils.
        InstanceIdentifierBuilder<Interface> idBuilder =
                InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new InterfaceKey(interfaceName));
        InstanceIdentifier<Interface> id = idBuilder.build();
        return id;
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> buildStateInterfaceId(String interfaceName) {
        //TODO Make this generic and move to AbstractDataChangeListener or Utils.
        InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> idBuilder =
                InstanceIdentifier.builder(InterfacesState.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.class,
                                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey(interfaceName));
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> id = idBuilder.build();
        return id;
    }

    private void addInterface(final InstanceIdentifier<Interface> identifier,
                              final Interface imgrInterface) {
        InstanceIdentifier<Interface> id = buildId(identifier);
        Optional<Interface> port = read(LogicalDatastoreType.CONFIGURATION, id);
        if(port.isPresent()) {
            Interface interf = port.get();
            NodeConnector nodeConn = getNodeConnectorFromDataStore(interf);
            updateInterfaceState(identifier, interf, nodeConn);
            if(nodeConn == null) {
                mapNcToInterfaceName.put(this.getNodeConnectorIdFromInterface(interf) , interf.getName());
            } else {
                mapNcToInterfaceName.put(nodeConn.getId(), interf.getName());
            }
            /* TODO:
             *  1. Get interface-id from id manager
             *  2. Update interface-state with following:
             *    admin-status = set to enable value
             *    oper-status = Down [?]
             *    if-index = interface-id
             * FIXME:
             *  1. Get operational data from node-connector-id?
             *
             */
        }
    }

    private void updateInterfaceState(InstanceIdentifier<Interface> identifier,
                    Interface interf, NodeConnector nodeConn) {
        /* Update InterfaceState
         * 1. Get interfaces-state Identifier
         * 2. Add interface to interfaces-state/interface
         * 3. Get interface-id from id manager
         * 4. Update interface-state with following:
         *    admin-status = set to enable value
         *    oper-status = Down [?]
         *    if-index = interface-id
        */
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> id =
                        buildStateInterfaceId(interf.getName());
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> stateIf =
                        read(LogicalDatastoreType.OPERATIONAL, id);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface stateIface;
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder ifaceBuilder =
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder();
        if(!stateIf.isPresent()) {
            // TODO: Get interface-id from IdManager
            ifaceBuilder.setAdminStatus((interf.isEnabled()) ?  org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus.Up :
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus.Down);
            ifaceBuilder.setOperStatus(getOperStatus(nodeConn));

            ifaceBuilder.setIfIndex(200).setName(interf.getName()).setType(interf.getType());
            ifaceBuilder.setKey(getStateInterfaceKeyFromName(interf.getName()));
            //ifaceBuilder.setStatistics(createStatistics(interf.getName(), nodeConn));
            stateIface = ifaceBuilder.build();
            LOG.trace("Adding stateIface {} and id {} to OPERATIONAL DS", stateIface, id);
            asyncWrite(LogicalDatastoreType.OPERATIONAL, id, stateIface, DEFAULT_CALLBACK);
        } else {
            if(interf.isEnabled() != null) {
                ifaceBuilder.setAdminStatus((interf.isEnabled()) ?  org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus.Up :
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus.Down);
            }
            if(interf.getType() != null) {
                ifaceBuilder.setType(interf.getType());
            }
            ifaceBuilder.setOperStatus(getOperStatus(nodeConn));
            stateIface = ifaceBuilder.build();
            LOG.trace("updating OPERATIONAL data store with stateIface {} and id {}", stateIface, id);
            asyncUpdate(LogicalDatastoreType.OPERATIONAL, id, stateIface, DEFAULT_CALLBACK);
        }
    }

    /*
    private void setAugmentations(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder ifaceBuilder,
                    InstanceIdentifier<Interface> identifier, Interface interf) {
        // TODO Add code for all augmentations
        InstanceIdentifier<IfL3tunnel> ifL3TunnelPath = identifier.augmentation(IfL3tunnel.class);
        Optional<IfL3tunnel> l3Tunnel = read(LogicalDatastoreType.CONFIGURATION, ifL3TunnelPath);
        String ifName = interf.getName();
        if(l3Tunnel.isPresent()) {
            l3Tunnel.get();
        }
    }
    */

    private OperStatus getOperStatus(NodeConnector nodeConn) {
        LOG.trace("nodeConn is {}", nodeConn);
        if(nodeConn == null) {
            return OperStatus.Down;
        }else {
            return OperStatus.Up;
        }
    }

    private Statistics createStatistics(String name, NodeConnector nodeConn) {
        Counter64 init64 = new Counter64(new BigInteger("0000000000000000"));
        Counter32 init32 = new Counter32((long) 0);
        StatisticsBuilder statBuilder = new StatisticsBuilder();
        statBuilder.setDiscontinuityTime(new DateAndTime("2015-04-04T00:00:00Z"))
        .setInBroadcastPkts(init64).setInDiscards(init32).setInErrors(init32).setInMulticastPkts(init64)
        .setInOctets(init64).setInUnicastPkts(init64).setInUnknownProtos(init32).setOutBroadcastPkts(init64)
        .setOutDiscards(init32).setOutErrors(init32).setOutMulticastPkts(init64).setOutOctets(init64)
        .setOutUnicastPkts(init64);
        return statBuilder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey getStateInterfaceKeyFromName(
                    String name) {
        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey(name);
    }

    private NodeConnector getNodeConnectorFromDataStore(Interface interf) {
        NodeConnectorId ncId = interf.getAugmentation(BaseIds.class).getOfPortId();
        //TODO: Replace with MDSAL Util method
        NodeId nodeId = new NodeId(ncId.getValue().substring(0,ncId.getValue().lastIndexOf(":")));
        InstanceIdentifier<NodeConnector> ncIdentifier = InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, new NodeKey(nodeId))
                        .child(NodeConnector.class, new NodeConnectorKey(ncId)).build();

        Optional<NodeConnector> nc = read(LogicalDatastoreType.OPERATIONAL, ncIdentifier);
        if(nc.isPresent()) {
            NodeConnector nodeConn = nc.get();
            LOG.trace("nodeConnector: {}",nodeConn);
            return nodeConn;
        }
        return null;
    }

    private NodeConnectorId getNodeConnectorIdFromInterface(Interface interf) {
        return interf.getAugmentation(BaseIds.class).getOfPortId();
    }

    private void delInterface(final InstanceIdentifier<Interface> identifier,
                              final Interface delInterface) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> id =
                        buildStateInterfaceId(delInterface.getName());
        Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> stateIf =
                        read(LogicalDatastoreType.OPERATIONAL, id);
        if(stateIf.isPresent()) {
            LOG.trace("deleting interfaces:state OPERATIONAL data store with id {}", id);
            asyncRemove(LogicalDatastoreType.OPERATIONAL, id, DEFAULT_CALLBACK);
            NodeConnectorId ncId = getNodeConnectorIdFromInterface(delInterface);
            if(ncId != null) {
                mapNcToInterfaceName.remove(ncId);
            }
        }
    }

    private void updateInterface(final InstanceIdentifier<Interface> identifier,
                              final Interface original, final Interface update) {
        InstanceIdentifier<Interface> id = buildId(identifier);
        Optional<Interface> port = read(LogicalDatastoreType.CONFIGURATION, id);
        if(port.isPresent()) {
            Interface interf = port.get();
            NodeConnector nc = getNodeConnectorFromDataStore(update);
            updateInterfaceState(identifier, update, nc);
            /*
             * Alternative is to get from interf and update map irrespective if NCID changed or not.
             */
            if(nc != null) {
                // Name doesn't change. Is it present in update?
                mapNcToInterfaceName.put(nc.getId(), original.getName());
            }
            //TODO: Update operational data
        }
    }

    private <T extends DataObject> Optional<T> read(LogicalDatastoreType datastoreType,
            InstanceIdentifier<T> path) {

        ReadOnlyTransaction tx = broker.newReadOnlyTransaction();

        Optional<T> result = Optional.absent();
        try {
            result = tx.read(datastoreType, path).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> identifier, Interface del) {
        LOG.trace("remove - key: " + identifier + ", value=" + del );
        delInterface(identifier, del);
    }

    @Override
    protected void update(InstanceIdentifier<Interface> identifier, Interface original, Interface update) {
        LOG.trace("update - key: " + identifier + ", original=" + original + ", update=" + update );
        updateInterface(identifier, original, update);
    }

    private <T extends DataObject> void asyncWrite(LogicalDatastoreType datastoreType,
                    InstanceIdentifier<T> path, T data, FutureCallback<Void> callback) {
    WriteTransaction tx = broker.newWriteOnlyTransaction();
    tx.put(datastoreType, path, data, true);
    Futures.addCallback(tx.submit(), callback);
    }

    private <T extends DataObject> void asyncUpdate(LogicalDatastoreType datastoreType,
                    InstanceIdentifier<T> path, T data, FutureCallback<Void> callback) {
    WriteTransaction tx = broker.newWriteOnlyTransaction();
    tx.merge(datastoreType, path, data, true);
    Futures.addCallback(tx.submit(), callback);
    }

    private <T extends DataObject> void asyncRemove(LogicalDatastoreType datastoreType,
                    InstanceIdentifier<T> path, FutureCallback<Void> callback) {
    WriteTransaction tx = broker.newWriteOnlyTransaction();
    tx.delete(datastoreType, path);
    Futures.addCallback(tx.submit(), callback);
    }

    void processPortAdd(NodeConnector port) {
        NodeConnectorId portId = port.getId();
        FlowCapableNodeConnector ofPort = port.getAugmentation(FlowCapableNodeConnector.class);
        LOG.debug("PortAdd: PortId { "+portId.getValue()+"} PortName {"+ofPort.getName()+"}");
        String ifName = this.mapNcToInterfaceName.get(portId);
        setInterfaceOperStatus(ifName, OperStatus.Up);
    }

    void processPortUpdate(NodeConnector oldPort, NodeConnector update) {
        //TODO: Currently nothing to do here.
        //LOG.trace("map: {}", this.mapNcToInterfaceName);
    }

    void processPortDelete(NodeConnector port) {
        NodeConnectorId portId = port.getId();
        FlowCapableNodeConnector ofPort = port.getAugmentation(FlowCapableNodeConnector.class);
        LOG.debug("PortDelete: PortId { "+portId.getValue()+"} PortName {"+ofPort.getName()+"}");
        String ifName = this.mapNcToInterfaceName.get(portId);
        setInterfaceOperStatus(ifName, OperStatus.Down);
    }

    private void setInterfaceOperStatus(String ifName, OperStatus opStatus) {
        if (ifName != null) {
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> id =
                            buildStateInterfaceId(ifName);
            Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> stateIf =
                            read(LogicalDatastoreType.OPERATIONAL, id);
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface stateIface;
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder ifaceBuilder =
                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder();
            if (stateIf.isPresent()) {
                stateIface = ifaceBuilder.setOperStatus(opStatus).build();
                LOG.trace("Setting OperStatus for {} to {} in OPERATIONAL DS", ifName, opStatus);
                asyncUpdate(LogicalDatastoreType.OPERATIONAL, id, stateIface, DEFAULT_CALLBACK);
            }
        }
    }

}
