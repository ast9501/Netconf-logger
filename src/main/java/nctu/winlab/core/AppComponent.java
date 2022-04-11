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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
//import org.codehaus.jackson.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onosproject.net.device.DeviceService;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Device.Type;

import java.util.Map;
import java.util.HashMap;

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

    // Extract fields: deviceId, eventType, timeStamp from device event
    private Map<String, String> eventExtract (String str) {
        String substr = str.substring(str.indexOf("{"), str.length());
        String timeStamp = substr.substring(substr.indexOf("=") + 1, substr.indexOf(","));

        String substr2 = substr.substring(substr.indexOf(",") + 1, substr.length());
        String eventType = substr2.substring(substr2.indexOf("=") + 1, substr2.indexOf(","));

        String substr3 = substr2.substring(substr2.indexOf("{"), substr2.length());
        String id = substr3.substring(substr3.indexOf("=") + 1, substr3.indexOf(","));

        Map<String, String> vars = new HashMap<>();
        vars.put("Id", id);
        vars.put("eventType", eventType);
        vars.put("timeStamp", timeStamp);

        return vars;
    }

    private class NetconfDeviceListener implements DeviceListener {
    	@Override
	    public void event(DeviceEvent event){
		    if (event.type() == DEVICE_ADDED){
                // output event info log
			    log.info("[Log] Device Added!");
                Map<String, String> values = eventExtract(event.toString());
                log.info("[Log] DeviceId: {}", values.get("Id"));
                log.info("[Log] eventType: {}", values.get("eventType"));
                log.info("[Log] timeStamp: {}", values.get("timeStamp"));

                // Encode event info into json 
                String dataJson = "";
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode data = mapper.createObjectNode();
                    data.put("deviceId", values.get("Id"));
                    data.put("eventType", values.get("eventType"));
                    data.put("timestamp", values.get("timeStamp"));
                    dataJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
                } catch (Exception ex){
                    log.info("Build JsonObject error");
                }
                //log.info(dataJson);

                // Send json to remote odlux agent
                // TODO: get agent ip from os.env
                try {
                    URL url = new URL("http://127.0.0.1:8000/v1/netconflogs");
                    HttpURLConnection con = (HttpURLConnection)url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "application/json");

                    con.setDoOutput(true);

                    OutputStream os = con.getOutputStream();
                    byte[] input = dataJson.getBytes("utf-8");
                    os.write(input, 0, input.length);
                    log.info(Integer.toString(con.getResponseCode()));
                } catch (MalformedURLException e) {
                    log.info("Failed to build URL link");
                } catch (IOException e) {
                    log.info("Failed to connect to odlux agent");
                } catch (Exception e) {
                    log.info("Loss the connection with odlux agent");
                }
                // DeviceEvent{time=2022-04-04T07:17:21.038Z, type=DEVICE_ADDED, subject=DefaultDevice{id=netconf:172.19.0.3:830, type=VIRTUAL, manufacturer=Of-Config, hwVersion=VirtualBox, swVersion=1.0, serialNumber=1, driver=ovs-netconf}}
			    //log.info("[Log] Current time: {}", event.time());
		    } else if(event.type() == DEVICE_REMOVED){
                log.info("[Log] Device Remove!");
                Map<String, String> values = eventExtract(event.toString());
                log.info("[Log] DeviceId: {}", values.get("Id"));
                log.info("[Log] eventType: {}", values.get("eventType"));
                log.info("[Log] timeStamp: {}", values.get("timeStamp"));

                // Encode event info into json 
                String dataJson = "";
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode data = mapper.createObjectNode();
                    data.put("deviceId", values.get("Id"));
                    data.put("eventType", values.get("eventType"));
                    data.put("timestamp", values.get("timeStamp"));
                    dataJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
                } catch (Exception ex){
                    log.info("Build JsonObject error");
                }

                // Send json to remote odlux agent
                // TODO: get agent ip from os.env
                try {
                    URL url = new URL("http://127.0.0.1:8000/v1/netconflogs");
                    HttpURLConnection con = (HttpURLConnection)url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "application/json");

                    con.setDoOutput(true);

                    OutputStream os = con.getOutputStream();
                    byte[] input = dataJson.getBytes("utf-8");
                    os.write(input, 0, input.length);
                    log.info(Integer.toString(con.getResponseCode()));
                } catch (MalformedURLException e) {
                    log.info("Failed to build URL link");
                } catch (IOException e) {
                    log.info("Failed to connect to odlux agent");
                } catch (Exception e) {
                    log.info("Loss the connection with odlux agent");
                }

		    } else if(event.type() == DEVICE_AVAILABILITY_CHANGED){
                log.info("[Log] Device status change!");
			    Map<String, String> values = eventExtract(event.toString());
                log.info("[Log] DeviceId: {}", values.get("Id"));
                log.info("[Log] eventType: {}", values.get("eventType"));
                log.info("[Log] timeStamp: {}", values.get("timeStamp"));

                // Encode event info into json 
                String dataJson = "";
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode data = mapper.createObjectNode();
                    data.put("deviceId", values.get("Id"));
                    data.put("eventType", values.get("eventType"));
                    data.put("timestamp", values.get("timeStamp"));
                    dataJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
                } catch (Exception ex){
                    log.info("Build JsonObject error");
                }

                // Send json to remote odlux agent
                // TODO: get agent ip from os.env
                try {
                    URL url = new URL("http://127.0.0.1:8000/v1/netconflogs");
                    HttpURLConnection con = (HttpURLConnection)url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "application/json");

                    con.setDoOutput(true);

                    OutputStream os = con.getOutputStream();
                    byte[] input = dataJson.getBytes("utf-8");
                    os.write(input, 0, input.length);
                    log.info(Integer.toString(con.getResponseCode()));
                } catch (MalformedURLException e) {
                    log.info("Failed to build URL link");
                } catch (IOException e) {
                    log.info("Failed to connect to odlux agent");
                } catch (Exception e) {
                    log.info("Loss the connection with odlux agent");
                }
		    } else {
			    log.info("[Log] New device event detected! ");
			    log.info("[Log] Event type: {}", event.type());
			    log.info("[Log] {}", event.subject());
			    log.info("[Log] Current time: {}", event.time());
		    }
	    }
    }
}
