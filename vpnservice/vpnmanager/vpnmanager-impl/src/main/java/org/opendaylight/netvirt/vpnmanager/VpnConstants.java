/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netvirt.vpnmanager;

import java.math.BigInteger;

public class VpnConstants {
    public static final String VPN_IDPOOL_NAME = "vpnservices";
    public static final long VPN_IDPOOL_START = 70000L;
    public static final String VPN_IDPOOL_SIZE = "100000";
    public static final short DEFAULT_FLOW_PRIORITY = 10;
    public static final int DEFAULT_LPORT_DISPATCHER_FLOW_PRIORITY = 1;
    public static final long INVALID_ID = -1;
    public static final String SEPARATOR = ".";
    public static final BigInteger COOKIE_L3_BASE = new BigInteger("8000000", 16);
    public static final String FLOWID_PREFIX = "L3.";
    public static final long MIN_WAIT_TIME_IN_MILLISECONDS = 5000;
    public static final long MAX_WAIT_TIME_IN_MILLISECONDS = 90000;
    public static final long PER_INTERFACE_MAX_WAIT_TIME_IN_MILLISECONDS = 10000;
    public static final int ELAN_GID_MIN = 200000;

    // An IdPool for Pseudo LPort tags, that is, lportTags that are no related to an interface.
    // These lportTags must be higher than 65535 to avoid collision with interface LportTags
    // TODO: This pool details and creation... should it be located in InterfaceManager?
    public static final String PSEUDO_LPORT_TAG_ID_POOL_NAME = System.getProperty("lporttag.idpool.name", "lporttag");
    public static final long LOWER_PSEUDO_LPORT_TAG = Long.getLong("lporttag.range.lower", 170001);
    // The max value for LPortTag is 1F FF FF => 2097151
    public static final long UPPER_PSEUDO_LPORT_TAG = Long.getLong("lporttag.range.upper", 2097151);

    public static byte[] EthernetDestination_Broadcast = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
    public static byte[] MAC_Broadcast = new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
    public enum ITMTunnelLocType {
        Invalid(0), Internal(1), External(2), Hwvtep(3);

        private final int type;
        ITMTunnelLocType(int id) { this.type = id; }
        public int getValue() { return type; }
    }
    public enum DCGWPresentStatus {
        Invalid(0), Present(1), Absent(2);

        private final int status;
        DCGWPresentStatus(int id) { this.status = id; }
        public int getValue() { return status; }
    }
}
