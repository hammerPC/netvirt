/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */

package org.opendaylight.netvirt.bgpmanager.thrift.gen;

public enum af_afi implements org.apache.thrift.TEnum {
  AFI_IP(1);

  private final int value;

  private af_afi(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static af_afi findByValue(int value) { 
    switch (value) {
      case 1:
        return AFI_IP;
      default:
        return null;
    }
  }
}
