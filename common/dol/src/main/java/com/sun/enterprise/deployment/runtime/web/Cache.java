/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.deployment.runtime.web;

import com.sun.enterprise.deployment.runtime.RuntimeDescriptor;

/**
* this class contains runtime information for the web bundle 
* it was kept to be backward compatible with the schema2beans descriptors
* generated by iAS 7.0 engineering team.
*
* @author Jerome Dochez
*/
public class Cache extends WebPropertyContainer
{
    
    static public final String CACHE_HELPER = "CacheHelper";	// NOI18N
    static public final String DEFAULT_HELPER = "DefaultHelper";	// NOI18N
    static public final String PROPERTY = "WebProperty";	// NOI18N
    static public final String CACHE_MAPPING = "CacheMapping";	// NOI18N
    static public final String MAX_ENTRIES = "MaxEntries";
    static public final String TIMEOUT_IN_SECONDS = "TimeoutInSeconds";
    static public final String ENABLED = "Enabled";
    
    public Cache() {
	
    	// set default values
	setAttributeValue("MaxEntries", "4096");
	setAttributeValue("TimeoutInSeconds", "30");
    }
    
    // This attribute is an array, possibly empty
    public void setCacheHelper(int index, CacheHelper value)
    {
	setValue(CACHE_HELPER, index, value);
    }
    
    //
    public CacheHelper getCacheHelper(int index)
    {
	return (CacheHelper)getValue(CACHE_HELPER, index);
    }
    
    // This attribute is an array, possibly empty
    public void setCacheHelper(CacheHelper[] value)
    {
	setValue(CACHE_HELPER, value);
    }
    
    //
    public CacheHelper[] getCacheHelper()
    {
	return (CacheHelper[])getValues(CACHE_HELPER);
    }
    
    // Return the number of properties
    public int sizeCacheHelper()
    {
	return size(CACHE_HELPER);
    }
    
    // Add a new element returning its index in the list
    public int addCacheHelper(CacheHelper value)
    {
	return addValue(CACHE_HELPER, value);
    }
    
    // Add a new element returning its index in the list
    public void addNewCacheHelper(CacheHelper value)
    {
	addCacheHelper(value);
    }    
    
    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeCacheHelper(CacheHelper value)
    {
	return removeValue(CACHE_HELPER, value);
    }
    
    // This attribute is optional
    public void setDefaultHelper(DefaultHelper value)
    {
	setValue(DEFAULT_HELPER, value);
    }
    
    //
    public DefaultHelper getDefaultHelper()
    {
	return (DefaultHelper)getValue(DEFAULT_HELPER);
    }
    
    // This attribute is an array, possibly empty
    public void setCacheMapping(int index, CacheMapping value)
    {
	setValue(CACHE_MAPPING, index, value);
    }
    
    //
    public CacheMapping getCacheMapping(int index)
    {
	return (CacheMapping)getValue(CACHE_MAPPING, index);
    }
    
    // This attribute is an array, possibly empty
    public void setCacheMapping(CacheMapping[] value)
    {
	setValue(CACHE_MAPPING, value);
    }
    
    //
    public CacheMapping[] getCacheMapping()
    {
	return (CacheMapping[])getValues(CACHE_MAPPING);
    }
    
    // Return the number of properties
    public int sizeCacheMapping()
    {
	return size(CACHE_MAPPING);
    }
    
    // Add a new element returning its index in the list
    public int addCacheMapping(CacheMapping value)
    {
	return addValue(CACHE_MAPPING, value);
    }
    
    // Add a new element returning its index in the list
    public void addNewCacheMapping(CacheMapping value)
    {
	addCacheMapping(value);
    }
    
    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeCacheMapping(CacheMapping value)
    {
	return removeValue(CACHE_MAPPING, value);
    }
    
    // This method verifies that the mandatory properties are set
    public boolean verify()
    {
	return true;
    }
    
}
