/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netvirt.aclservice.utils;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.netvirt.aclservice.api.utils.IAclServiceUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Matches;

@Singleton
public class AclServiceUtilFacade implements IAclServiceUtil {

    @Inject
    public AclServiceUtilFacade() {
    }

    @Override
    public Map<String, List<MatchInfoBase>> programIpFlow(Matches matches) {
        return AclServiceOFFlowBuilder.programIpFlow(matches);
    }

}
