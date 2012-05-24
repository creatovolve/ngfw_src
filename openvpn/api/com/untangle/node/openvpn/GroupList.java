/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;

@SuppressWarnings("serial")
public class GroupList implements Serializable, Validatable
{

    List<VpnGroup> groupList;

    public GroupList()
    {
        this( new LinkedList<VpnGroup>());
    }

    public GroupList( List<VpnGroup> groupList )
    {
        this.groupList = groupList;
    }

    public List<VpnGroup> getGroupList()
    {
        return this.groupList;
    }
    
    public void setGroupList( List<VpnGroup> groupList )
    {
        this.groupList = groupList;
    }

    List<AddressRange> buildAddressRange()
    {
        List<AddressRange> checkList = new LinkedList<AddressRange>();

        for ( VpnGroup group : this.groupList ) {
            checkList.add( AddressRange.makeNetwork( group.getAddress().getAddr(), group.getNetmask().getAddr()));
        }
        
        return checkList;
    }
   
    /** 
     * Validate the object, throw an exception if it is not valid */
    public void validate() throws ValidateException
    {
        Set<String> nameSet = new HashSet<String>();
        
        for ( VpnGroup group : this.groupList ) {
            String name = group.trans_getInternalName();
            if ( !nameSet.add( name )) {
                throw new ValidateException( "Group names must be unique: '" + name + "'" );
            }
        }

        /* Determine if all of the addresses are unique */
        AddressValidator.getInstance().validateOverlap( buildAddressRange());
    }
}
