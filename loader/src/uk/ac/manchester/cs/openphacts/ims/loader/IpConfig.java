// BridgeDb,
// An abstraction layer for identifier mapping services, both local and online.
//
// Copyright 2006-2009 BridgeDb developers
// Copyright 2012-2013  Christian Y. A. Brenninkmeijer
// Copyright 2012-2013  OpenPhacts
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package uk.ac.manchester.cs.openphacts.ims.loader;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;
import org.bridgedb.utils.BridgeDBException;
import org.bridgedb.utils.ConfigReader;

/**
 * Currently not used as remote loading not supported.
 * 
 * @author Christian
 */
public class IpConfig {

    static final Logger logger = Logger.getLogger(IpConfig.class);

    private static Properties properties;

    public static boolean isAdminIPAddress(String ipAddress) throws BridgeDBException {
        String owner = getProperties().getProperty(ipAddress);
        if (owner == null){
            properties = ConfigReader.getProperties();
            owner = properties.getProperty(ipAddress);
        }
        return owner != null;
    }

    public static String checkIPAddress(String ipAddress) throws BridgeDBException{
        String owner = getProperties().getProperty(ipAddress);
        if (owner == null){
            properties = ConfigReader.getProperties();
            owner = properties.getProperty(ipAddress);
            if (owner == null){
                logger.warn("Attempt to check IP address " + ipAddress);
            }
        }
        return owner;
    }
    
    private static Properties getProperties() throws BridgeDBException{
        if (properties == null){
            properties = ConfigReader.getProperties();
        }
        return properties;
    }
    
    public static void main(String[] args) throws BridgeDBException  {
        Set<Object> keys = getProperties().keySet();
        System.out.println(keys);
    }
}
