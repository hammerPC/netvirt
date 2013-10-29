
package org.opendaylight.ovsdb.plugin;
import java.net.InetAddress;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Property;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class IPAddressProperty extends Property implements Cloneable {
    private static final long serialVersionUID = 1L;
    @XmlElement(name="value")
    private final InetAddress address;
    public static final String name = "IPAddress";

    /*
     * Private constructor used for JAXB mapping
     */
    private IPAddressProperty() {
        super(name);
        this.address = null;
    }

    public IPAddressProperty(InetAddress address) {
        super(name);
        this.address = address;
    }

    @Override
    public String getStringValue() {
        if (address == null) return null;
        return this.address.getHostAddress();
    }

    @Override
    public Property clone() {
        return new IPAddressProperty(this.address);
    }

    public InetAddress getAddress() {
        return address;
    }
}
