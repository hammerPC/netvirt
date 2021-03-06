/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netvirt.natservice.internal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.BucketInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.GroupEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NWUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionGroup;
import org.opendaylight.genius.mdsalutil.actions.ActionNxLoadMetadata;
import org.opendaylight.genius.mdsalutil.actions.ActionNxResubmit;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.instructions.InstructionApplyActions;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.mdsalutil.matches.MatchEthernetType;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv4Destination;
import org.opendaylight.genius.mdsalutil.matches.MatchMetadata;
import org.opendaylight.genius.mdsalutil.matches.MatchTunnelId;
import org.opendaylight.netvirt.fibmanager.api.IFibManager;
import org.opendaylight.netvirt.fibmanager.api.RouteOrigin;
import org.opendaylight.netvirt.natservice.api.SnatServiceListener;
import org.opendaylight.netvirt.natservice.ha.NatDataUtil;
import org.opendaylight.netvirt.vpnmanager.api.IVpnFootprintService;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.VpnInterfaces;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.VpnInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterface;
import org.opendaylight.yang.gen.v1.urn.huawei.params.xml.ns.yang.l3vpn.rev140815.vpn.interfaces.VpnInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.fibmanager.rev150330.vrfentries.VrfEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.AdjacenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.LearntVpnVipToPortData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.LearntVpnVipToPortDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.adjacency.list.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.learnt.vpn.vip.to.port.data.LearntVpnVipToPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.l3vpn.rev130911.prefix.to._interface.vpn.ids.Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.natservice.rev160111.ext.routers.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.natservice.rev160111.ext.routers.routers.ExternalIps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSnatService implements SnatServiceListener {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSnatService.class);

    static final int LOAD_START = mostSignificantBit(MetaDataUtil.METADATA_MASK_SH_FLAG.intValue());
    static final int LOAD_END = mostSignificantBit(MetaDataUtil.METADATA_MASK_VRFID.intValue() | MetaDataUtil
            .METADATA_MASK_SH_FLAG.intValue());

    protected final DataBroker dataBroker;
    protected final IMdsalApiManager mdsalManager;
    protected final IdManagerService idManager;
    protected final NAPTSwitchSelector naptSwitchSelector;
    protected final ItmRpcService itmManager;
    protected final OdlInterfaceRpcService odlInterfaceRpcService;
    protected final IInterfaceManager interfaceManager;
    protected final IVpnFootprintService vpnFootprintService;
    protected final IFibManager fibManager;
    protected final NatDataUtil natDataUtil;

    protected AbstractSnatService(final DataBroker dataBroker, final IMdsalApiManager mdsalManager,
                                  final ItmRpcService itmManager, final OdlInterfaceRpcService odlInterfaceRpcService,
                                  final IdManagerService idManager, final NAPTSwitchSelector naptSwitchSelector,
                                  final IInterfaceManager interfaceManager,
                                  final IVpnFootprintService vpnFootprintService,
                                  final IFibManager fibManager, final NatDataUtil natDataUtil) {
        this.dataBroker = dataBroker;
        this.mdsalManager = mdsalManager;
        this.itmManager = itmManager;
        this.interfaceManager = interfaceManager;
        this.idManager = idManager;
        this.naptSwitchSelector = naptSwitchSelector;
        this.odlInterfaceRpcService = odlInterfaceRpcService;
        this.vpnFootprintService = vpnFootprintService;
        this.fibManager = fibManager;
        this.natDataUtil = natDataUtil;
    }

    protected DataBroker getDataBroker() {
        return dataBroker;
    }

    public void init() {
        LOG.info("{} init", getClass().getSimpleName());
    }

    public void close() {
        LOG.debug("AbstractSnatService Closed");
    }

    @Override
    public boolean handleSnatAllSwitch(Routers routers, BigInteger primarySwitchId,  int addOrRemove) {
        LOG.info("handleSnatAllSwitch : Handle Snat in all switches for router {}", routers.getRouterName());
        String routerName = routers.getRouterName();
        List<BigInteger> switches = naptSwitchSelector.getDpnsForVpn(routerName);
        /*
         * Primary switch handled separately since the pseudo port created may
         * not be present in the switch list on delete.
         */
        boolean isLastRouterDelete = false;
        if (addOrRemove == NwConstants.DEL_FLOW) {
            isLastRouterDelete = NatUtil.isLastExternalRouter(routers.getNetworkId()
                .getValue(), routers.getRouterName(), natDataUtil);
            LOG.info("handleSnatAllSwitch : action is delete for router {} and isLastRouterDelete is {}",
                    routers.getRouterName(), isLastRouterDelete);
        }
        handleSnat(routers, primarySwitchId, primarySwitchId, addOrRemove);
        for (BigInteger dpnId : switches) {
            if (primarySwitchId != dpnId) {
                handleSnat(routers, primarySwitchId, dpnId, addOrRemove);
            }
        }
        if (isLastRouterDelete) {
            removeLearntIpPorts(routers);
            removeMipAdjacencies(routers);
        }
        return true;
    }

    @Override
    public boolean handleSnat(Routers routers, BigInteger primarySwitchId, BigInteger dpnId,  int addOrRemove) {

        // Handle non NAPT switches and NAPT switches separately
        if (!dpnId.equals(primarySwitchId)) {
            LOG.info("handleSnat : Handle non NAPT switch {} for router {}", dpnId, routers.getRouterName());
            installSnatCommonEntriesForNonNaptSwitch(routers, primarySwitchId, dpnId, addOrRemove);
            installSnatSpecificEntriesForNonNaptSwitch(routers, dpnId, addOrRemove);
        } else {
            LOG.info("handleSnat : Handle NAPT switch {} for router {}", dpnId, routers.getRouterName());
            installSnatCommonEntriesForNaptSwitch(routers, dpnId, addOrRemove);
            installSnatSpecificEntriesForNaptSwitch(routers, dpnId, addOrRemove);

        }
        return true;
    }

    protected void installSnatCommonEntriesForNaptSwitch(Routers routers, BigInteger dpnId,  int addOrRemove) {
        String routerName = routers.getRouterName();
        Long routerId = NatUtil.getVpnId(dataBroker, routerName);
        installDefaultFibRouteForSNAT(dpnId, routerId, addOrRemove);
        String externalGwMac = routers.getExtGwMacAddress();
        for (ExternalIps externalIp : routers.getExternalIps()) {
            if (!NWUtil.isIpv4Address(externalIp.getIpAddress())) {
                // In this class we handle only IPv4 use-cases.
                continue;
            }
            //The logic now handle only one external IP per router, others if present will be ignored.
            long extSubnetId = NatConstants.INVALID_ID;
            if (addOrRemove == NwConstants.ADD_FLOW) {
                extSubnetId = NatUtil.getExternalSubnetVpnId(dataBroker, externalIp.getSubnetId());
            }
            installInboundFibEntry(dpnId, externalIp.getIpAddress(), routerId, extSubnetId,
                routers.getNetworkId().getValue(), externalIp.getSubnetId().getValue(), externalGwMac, addOrRemove);
            installInboundTerminatingServiceTblEntry(dpnId, routerId, extSubnetId, addOrRemove);
            break;
        }
    }

    protected void installSnatCommonEntriesForNonNaptSwitch(Routers routers, BigInteger primarySwitchId,
            BigInteger dpnId, int addOrRemove) {
        String routerName = routers.getRouterName();
        Long routerId = NatUtil.getVpnId(dataBroker, routerName);
        installDefaultFibRouteForSNAT(dpnId, routerId, addOrRemove);
        installSnatMissEntry(dpnId, routerId, routerName, primarySwitchId, addOrRemove);
    }

    protected abstract void installSnatSpecificEntriesForNaptSwitch(Routers routers, BigInteger dpnId,
            int addOrRemove);

    protected abstract void installSnatSpecificEntriesForNonNaptSwitch(Routers routers, BigInteger dpnId,
            int addOrRemove);

    protected void installInboundFibEntry(BigInteger dpnId, String externalIp, Long routerId,
            long extSubnetId, String externalNetId, String subNetId, String routerMac, int addOrRemove) {
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(MatchEthernetType.IPV4);
        if (addOrRemove == NwConstants.ADD_FLOW) {
            if (extSubnetId == NatConstants.INVALID_ID) {
                LOG.error("ConntrackBasedSnatService : installInboundFibEntry : external subnet id is invalid.");
                return;
            }
            matches.add(new MatchMetadata(MetaDataUtil.getVpnIdMetadata(extSubnetId),
                    MetaDataUtil.METADATA_MASK_VRFID));
        }
        matches.add(new MatchIpv4Destination(externalIp, "32"));

        ArrayList<ActionInfo> listActionInfo = new ArrayList<>();
        ArrayList<InstructionInfo> instructionInfo = new ArrayList<>();
        listActionInfo.add(new ActionNxResubmit(NwConstants.INBOUND_NAPT_TABLE));
        instructionInfo.add(new InstructionApplyActions(listActionInfo));

        String flowRef = getFlowRef(dpnId, NwConstants.L3_FIB_TABLE, routerId);
        flowRef = flowRef + "inbound" + externalIp;
        syncFlow(dpnId, NwConstants.L3_FIB_TABLE, flowRef, NatConstants.SNAT_FIB_FLOW_PRIORITY, flowRef,
                NwConstants.COOKIE_SNAT_TABLE, matches, instructionInfo, addOrRemove);
        String rd = NatUtil.getVpnRd(dataBroker, subNetId);
        String nextHopIp = NatUtil.getEndpointIpAddressForDPN(dataBroker, dpnId);
        String ipPrefix = externalIp + "/32";
        if (addOrRemove == NwConstants.ADD_FLOW) {
            NatUtil.addPrefixToInterface(dataBroker, NatUtil.getVpnId(dataBroker, subNetId),
                    null, ipPrefix, dpnId, new Uuid(subNetId), Prefixes.PrefixCue.Nat);

            fibManager.addOrUpdateFibEntry(rd, routerMac, ipPrefix,
                    Collections.singletonList(nextHopIp), VrfEntry.EncapType.Mplsgre, extSubnetId,
                    0, null, externalNetId, RouteOrigin.STATIC, null);
        } else {
            fibManager.removeFibEntry(rd, ipPrefix, null);
            NatUtil.deletePrefixToInterface(dataBroker, NatUtil.getVpnId(dataBroker, subNetId), ipPrefix);
        }
    }

    protected void installSnatMissEntry(BigInteger dpnId, Long routerId, String routerName, BigInteger primarySwitchId,
            int addOrRemove) {
        LOG.debug("installSnatMissEntry : Installing SNAT miss entry in switch {}", dpnId);
        List<ActionInfo> listActionInfoPrimary = new ArrayList<>();
        String ifNamePrimary = getTunnelInterfaceName(dpnId, primarySwitchId);
        List<BucketInfo> listBucketInfo = new ArrayList<>();
        if (ifNamePrimary != null) {
            LOG.debug("installSnatMissEntry : On Non- Napt switch , Primary Tunnel interface is {}", ifNamePrimary);
            listActionInfoPrimary = NatUtil.getEgressActionsForInterface(odlInterfaceRpcService, itmManager,
                    interfaceManager, ifNamePrimary, routerId, true);
        }
        BucketInfo bucketPrimary = new BucketInfo(listActionInfoPrimary);
        listBucketInfo.add(0, bucketPrimary);
        LOG.debug("installSnatMissEntry : installSnatMissEntry called for dpnId {} with primaryBucket {} ", dpnId,
                listBucketInfo.get(0));
        // Install the select group
        long groupId = createGroupId(getGroupIdKey(routerName));
        GroupEntity groupEntity = MDSALUtil.buildGroupEntity(dpnId, groupId, routerName, GroupTypes.GroupAll,
                listBucketInfo);

        if (addOrRemove == NwConstants.ADD_FLOW) {
            LOG.debug("installing the PSNAT to NAPTSwitch GroupEntity:{} with GroupId: {}", groupEntity, groupId);
            mdsalManager.installGroup(groupEntity);
        } else {
            LOG.debug("removing the PSNAT to NAPTSwitch GroupEntity:{} with GroupId: {}", groupEntity, groupId);
            mdsalManager.syncRemoveGroup(groupEntity);
        }

        // Install miss entry pointing to group
        LOG.debug("installSnatMissEntry : buildSnatFlowEntity is called for dpId {}, routerName {} and groupId {}",
                dpnId, routerName, groupId);
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(new MatchEthernetType(0x0800L));
        matches.add(new MatchMetadata(MetaDataUtil.getVpnIdMetadata(routerId), MetaDataUtil.METADATA_MASK_VRFID));


        List<ActionInfo> actionsInfo = new ArrayList<>();
        actionsInfo.add(new ActionSetFieldTunnelId(BigInteger.valueOf(routerId)));
        LOG.debug("installSnatMissEntry : Setting the tunnel to the list of action infos {}", actionsInfo);
        actionsInfo.add(new ActionGroup(groupId));
        List<InstructionInfo> instructions = new ArrayList<>();
        instructions.add(new InstructionApplyActions(actionsInfo));
        String flowRef = getFlowRef(dpnId, NwConstants.PSNAT_TABLE, routerId);
        syncFlow(dpnId, NwConstants.PSNAT_TABLE, flowRef,  NatConstants.DEFAULT_PSNAT_FLOW_PRIORITY, flowRef,
                NwConstants.COOKIE_SNAT_TABLE, matches, instructions, addOrRemove);
    }

    protected void installInboundTerminatingServiceTblEntry(BigInteger dpnId, Long routerId,
            long extSubnetId, int addOrRemove) {
        //Install the tunnel table entry in NAPT switch for inbound traffic to SNAP IP from a non a NAPT switch.
        LOG.info("installInboundTerminatingServiceTblEntry : creating entry for Terminating Service Table "
                + "for switch {}, routerId {}", dpnId, routerId);
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(MatchEthernetType.IPV4);
        List<ActionInfo> actionsInfos = new ArrayList<>();
        if (addOrRemove == NwConstants.ADD_FLOW) {
            if (extSubnetId == NatConstants.INVALID_ID) {
                LOG.error("installInboundTerminatingServiceTblEntry : external subnet id is invalid.");
                return;
            }
            matches.add(new MatchTunnelId(BigInteger.valueOf(extSubnetId)));
            ActionNxLoadMetadata actionLoadMeta = new ActionNxLoadMetadata(MetaDataUtil
                    .getVpnIdMetadata(extSubnetId), LOAD_START, LOAD_END);
            actionsInfos.add(actionLoadMeta);
        }
        actionsInfos.add(new ActionNxResubmit(NwConstants.INBOUND_NAPT_TABLE));
        List<InstructionInfo> instructions = new ArrayList<>();
        instructions.add(new InstructionApplyActions(actionsInfos));
        String flowRef = getFlowRef(dpnId, NwConstants.INTERNAL_TUNNEL_TABLE, routerId.longValue());
        flowRef = flowRef + "INBOUND";
        syncFlow(dpnId,  NwConstants.INTERNAL_TUNNEL_TABLE, flowRef, NatConstants.SNAT_FIB_FLOW_PRIORITY, flowRef,
                 NwConstants.COOKIE_SNAT_TABLE, matches, instructions, addOrRemove);

    }

    protected void installDefaultFibRouteForSNAT(BigInteger dpnId, Long extNetId, int addOrRemove) {

        List<MatchInfo> matches = new ArrayList<>();
        matches.add(MatchEthernetType.IPV4);
        matches.add(new MatchMetadata(MetaDataUtil.getVpnIdMetadata(extNetId),
                MetaDataUtil.METADATA_MASK_VRFID));

        List<InstructionInfo> instructions = new ArrayList<>();
        instructions.add(new InstructionGotoTable(NwConstants.PSNAT_TABLE));

        String flowRef = "DefaultFibRouteForSNAT" + getFlowRef(dpnId, NwConstants.L3_FIB_TABLE, extNetId);
        syncFlow(dpnId, NwConstants.L3_FIB_TABLE, flowRef, NatConstants.DEFAULT_DNAT_FLOW_PRIORITY, flowRef,
                NwConstants.COOKIE_SNAT_TABLE, matches, instructions, addOrRemove);
    }

    protected String getFlowRef(BigInteger dpnId, short tableId, long routerID) {
        return new StringBuilder().append(NatConstants.NAPT_FLOWID_PREFIX).append(dpnId).append(NatConstants
                .FLOWID_SEPARATOR).append(tableId).append(NatConstants.FLOWID_SEPARATOR).append(routerID).toString();
    }

    protected void syncFlow(BigInteger dpId, short tableId, String flowId, int priority, String flowName,
            BigInteger cookie, List<? extends MatchInfoBase> matches,
            List<InstructionInfo> instructions, int addOrRemove) {
        if (addOrRemove == NwConstants.DEL_FLOW) {
            FlowEntity flowEntity = MDSALUtil.buildFlowEntity(dpId, tableId, flowId, priority, flowName,
                    NatConstants.DEFAULT_IDLE_TIMEOUT, NatConstants.DEFAULT_IDLE_TIMEOUT, cookie, matches, null);
            LOG.trace("syncFlow : Removing Acl Flow DpnId {}, flowId {}", dpId, flowId);
            mdsalManager.removeFlow(flowEntity);
        } else {
            FlowEntity flowEntity = MDSALUtil.buildFlowEntity(dpId, tableId, flowId, priority, flowName,
                    NatConstants.DEFAULT_IDLE_TIMEOUT, NatConstants.DEFAULT_IDLE_TIMEOUT, cookie, matches,
                    instructions);
            LOG.trace("syncFlow : Installing DpnId {}, flowId {}", dpId, flowId);
            mdsalManager.installFlow(flowEntity);
        }
    }

    protected long createGroupId(String groupIdKey) {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder()
            .setPoolName(NatConstants.SNAT_IDPOOL_NAME).setIdKey(groupIdKey)
            .build();
        try {
            Future<RpcResult<AllocateIdOutput>> result = idManager.allocateId(getIdInput);
            RpcResult<AllocateIdOutput> rpcResult = result.get();
            return rpcResult.getResult().getIdValue();
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOG.error("createGroupId: Exception while creating group with key : {}",groupIdKey, e);
        }
        return 0;
    }

    protected String getGroupIdKey(String routerName) {
        return "snatmiss." + routerName;
    }

    protected String getTunnelInterfaceName(BigInteger srcDpId, BigInteger dstDpId) {
        Class<? extends TunnelTypeBase> tunType = TunnelTypeVxlan.class;
        RpcResult<GetTunnelInterfaceNameOutput> rpcResult;
        try {
            Future<RpcResult<GetTunnelInterfaceNameOutput>> result = itmManager
                    .getTunnelInterfaceName(new GetTunnelInterfaceNameInputBuilder().setSourceDpid(srcDpId)
                            .setDestinationDpid(dstDpId).setTunnelType(tunType).build());
            rpcResult = result.get();
            if (!rpcResult.isSuccessful()) {
                tunType = TunnelTypeGre.class ;
                result = itmManager.getTunnelInterfaceName(new GetTunnelInterfaceNameInputBuilder()
                        .setSourceDpid(srcDpId)
                        .setDestinationDpid(dstDpId)
                        .setTunnelType(tunType)
                        .build());
                rpcResult = result.get();
                if (!rpcResult.isSuccessful()) {
                    LOG.warn("getTunnelInterfaceName : RPC Call to getTunnelInterfaceId returned with Errors {}",
                            rpcResult.getErrors());
                } else {
                    return rpcResult.getResult().getInterfaceName();
                }
                LOG.warn("getTunnelInterfaceName : RPC Call to getTunnelInterfaceId returned with Errors {}",
                        rpcResult.getErrors());
            } else {
                return rpcResult.getResult().getInterfaceName();
            }
        } catch (InterruptedException | ExecutionException | NullPointerException e) {
            LOG.error("getTunnelInterfaceName : Exception when getting tunnel interface Id for tunnel "
                    + "between {} and {}", srcDpId, dstDpId);
        }
        return null;
    }

    protected void removeMipAdjacencies(Routers routers) {
        LOG.info("removeMipAdjacencies for router {}", routers.getRouterName());
        String externalSubNetId  = null;
        for (ExternalIps externalIp : routers.getExternalIps()) {
            if (!NWUtil.isIpv4Address(externalIp.getIpAddress())) {
                // In this class we handle only IPv4 use-cases.
                continue;
            }
            externalSubNetId = externalIp.getSubnetId().getValue();
            break;
        }
        if (externalSubNetId == null) {
            LOG.info("removeMipAdjacencies no external Ipv4 address present on router {}",
                    routers.getRouterName());
            return;
        }
        InstanceIdentifier<VpnInterfaces> vpnInterfacesId =
                InstanceIdentifier.builder(VpnInterfaces.class).build();
        try {
            VpnInterfaces vpnInterfaces = SingleTransactionDataBroker.syncRead(dataBroker,
                    LogicalDatastoreType.CONFIGURATION, vpnInterfacesId);
            List<VpnInterface> updatedVpnInterface = new ArrayList<>();
            for (VpnInterface vpnInterface : vpnInterfaces.getVpnInterface()) {
                List<Adjacency> updatedAdjacencies = new ArrayList<>();
                Adjacencies adjacencies = vpnInterface.augmentation(Adjacencies.class);
                if (null != adjacencies) {
                    for (Adjacency adjacency : adjacencies.getAdjacency()) {
                        if (!adjacency.getSubnetId().getValue().equals(externalSubNetId)) {
                            updatedAdjacencies.add(adjacency);
                        }
                    }
                }
                AdjacenciesBuilder adjacenciesBuilder = new AdjacenciesBuilder();
                adjacenciesBuilder.setAdjacency(updatedAdjacencies);
                VpnInterfaceBuilder vpnInterfaceBuilder = new VpnInterfaceBuilder(vpnInterface);
                vpnInterfaceBuilder.addAugmentation(Adjacencies.class, adjacenciesBuilder.build());
                updatedVpnInterface.add(vpnInterfaceBuilder.build());
            }
            VpnInterfacesBuilder vpnInterfacesBuilder = new VpnInterfacesBuilder();
            vpnInterfacesBuilder.setVpnInterface(updatedVpnInterface);

            SingleTransactionDataBroker.syncWrite(dataBroker, LogicalDatastoreType.CONFIGURATION,
                    vpnInterfacesId, vpnInterfacesBuilder.build());
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read removeMipAdjacencies with error {}", e.getMessage());
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to remove removeMipAdjacencies with error {}", e.getMessage());
        }
    }

    private void removeLearntIpPorts(Routers routers) {
        LOG.info("removeLearntIpPorts for router {} and network {}", routers.getRouterName(), routers.getNetworkId());
        String networkId = routers.getNetworkId().getValue();
        LearntVpnVipToPortData learntVpnVipToPortData = NatUtil.getLearntVpnVipToPortData(dataBroker);
        LearntVpnVipToPortDataBuilder learntVpnVipToPortDataBuilder = new LearntVpnVipToPortDataBuilder();
        List<LearntVpnVipToPort> learntVpnVipToPortList = new ArrayList<>();
        for (LearntVpnVipToPort learntVpnVipToPort : learntVpnVipToPortData.getLearntVpnVipToPort()) {
            if (!learntVpnVipToPort.getVpnName().equals(networkId)) {
                LOG.info("The learned port belongs to Vpn {} hence not removing", learntVpnVipToPort.getVpnName());
                learntVpnVipToPortList.add(learntVpnVipToPort);
            } else {
                String externalSubNetId = null;
                for (ExternalIps externalIp : routers.getExternalIps()) {
                    if (!NWUtil.isIpv4Address(externalIp.getIpAddress())) {
                        // In this class we handle only IPv4 use-cases.
                        continue;
                    }
                    externalSubNetId = externalIp.getSubnetId().getValue();
                    break;
                }
                if (externalSubNetId == null) {
                    LOG.info("removeLearntIpPorts no external Ipv4 address present on router {}",
                            routers.getRouterName());
                    return;
                }
                String prefix = learntVpnVipToPort.getPortFixedip() + "/32";
                NatUtil.deletePrefixToInterface(dataBroker, NatUtil.getVpnId(dataBroker,
                        externalSubNetId), prefix);
            }
        }

        try {
            learntVpnVipToPortDataBuilder.setLearntVpnVipToPort(learntVpnVipToPortList);
            InstanceIdentifier<LearntVpnVipToPortData> learntVpnVipToPortDataId = NatUtil
                    .getLearntVpnVipToPortDataId();
            SingleTransactionDataBroker.syncWrite(dataBroker, LogicalDatastoreType.OPERATIONAL,
                    learntVpnVipToPortDataId, learntVpnVipToPortDataBuilder.build());

        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to remove removeLearntIpPorts with error {}", e.getMessage());
        }
    }

    static int mostSignificantBit(int value) {
        return 31 - Integer.numberOfLeadingZeros(value);
    }
}
