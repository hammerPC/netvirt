/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netvirt.vpnmanager.intervpnlink;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.netvirt.vpnmanager.api.intervpnlink.InterVpnLinkCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.inter.vpn.link.rev160311.InterVpnLinks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.inter.vpn.link.rev160311.inter.vpn.links.InterVpnLink;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clustered listener whose only purpose is to keep global (well, per cluster)
 * caches updated.
 */
@Singleton
public class InterVpnLinkCacheFeeder
    extends AsyncClusteredDataTreeChangeListenerBase<InterVpnLink, InterVpnLinkCacheFeeder> {

    private static final Logger LOG = LoggerFactory.getLogger(InterVpnLinkCacheFeeder.class);

    private final InterVpnLinkCache interVpnLinkCache;
    private final DataBroker dataBroker;

    @Inject
    public InterVpnLinkCacheFeeder(final DataBroker dataBroker, final InterVpnLinkCache interVpnLinkCache) {
        this.dataBroker = dataBroker;
        this.interVpnLinkCache = interVpnLinkCache;
    }

    @PostConstruct
    public void init() {
        registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
    }

    @Override
    protected void remove(InstanceIdentifier<InterVpnLink> identifier, InterVpnLink del) {
        interVpnLinkCache.removeInterVpnLinkFromCache(del);
    }

    @Override
    protected void update(InstanceIdentifier<InterVpnLink> identifier, InterVpnLink original, InterVpnLink update) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void add(InstanceIdentifier<InterVpnLink> identifier, InterVpnLink add) {
        LOG.debug("Added interVpnLink {}  with vpn1={} and vpn2={}", add.getName(),
            add.getFirstEndpoint().getVpnUuid(), add.getSecondEndpoint().getVpnUuid());
        interVpnLinkCache.addInterVpnLinkToCaches(add);
    }

    @Override
    protected InstanceIdentifier<InterVpnLink> getWildCardPath() {
        return InstanceIdentifier.create(InterVpnLinks.class).child(InterVpnLink.class);
    }

    @Override
    protected InterVpnLinkCacheFeeder getDataTreeChangeListener() {
        return this;
    }

}
