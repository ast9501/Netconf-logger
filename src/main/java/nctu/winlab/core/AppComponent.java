/*
 * Copyright 2022-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nctu.winlab.core;

import com.google.common.collect.ImmutableSet;
import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;

import static org.onlab.util.Tools.get;


// Import
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;

import org.onosproject.net.device.DeviceEvent;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_REMOVED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED; // get device connection status change

import org.onosproject.net.device.DeviceListener;
//import org.onosproject.event.ListenerService;
import org.onosproject.net.device.DeviceService;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Device.Type;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true,
           service = {SomeInterface.class},
           property = {
               "someProperty=Some Default String Value",
           })
public class AppComponent implements SomeInterface {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Some configurable property. */
    private String someProperty;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    // NOTICE: DeviceListener c.f.  DeviceAgentListener?
    private final NetconfDeviceListener nfgListener = new NetconfDeviceListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService dvcService;

    private ApplicationId appId;

    @Activate
    protected void activate() {
        cfgService.registerProperties(getClass());
	appId = coreService.registerApplication("nctu.winlab.netconfLogger");	
	dvcService.addListener(nfgListener);

        log.info("Started", appId.id());
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
	dvcService.removeListener(nfgListener);

        log.info("Stopped", appId.id());
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
        log.info("Reconfigured");
    }

    @Override
    public void someMethod() {
        log.info("Invoked");
    }

    private class NetconfDeviceListener implements DeviceListener {
    	@Override
	public void event(DeviceEvent event){
		if (event.type() == DEVICE_ADDED){
			//DeviceId deviceId = event.Device.id();
			//log.info("[Log] New device added: {}", deviceId.toString());
			//log.info("[Log] Device type: {}", deviceId.type());
			log.info("[Log] Device Added!");
			log.info("[Log] {}", event.subject());
			log.info("[Log] Current time: {}", event.time());
		} else if(event.type() == DEVICE_REMOVED){
			//DeviceId deviceId = event.Device.id();
                        //log.info("[Log] Device removed: {}", deviceId.toString());
                        log.info("[Log] Device Remove!");
			//log.info("[Log] Device type: {}", deviceId.type());
                        log.info("[Log] {}", event.subject());
			log.info("[Log] Current time: {}", event.time());
		} else if(event.type() == DEVICE_AVAILABILITY_CHANGED){
			//DeviceId deviceId = event.Device.id();
                        //log.info("[Log] Device status change: {}", deviceId.toString());
                        //log.info("[Log] Device type: {}", deviceId.type());
                        log.info("[Log] Device status change!");
			log.info("[Log] {}", event.subject());
			log.info("[Log] Current time: {}", event.time());
		} else {
			log.info("[Log] New device event detected! ");
			log.info("[Log] Event type: {}", event.type());
			log.info("[Log] {}", event.subject());
			log.info("[Log] Current time: {}", event.time());
		}
	}
    }
}
