/**
 *  Path Lighting Color Picker
 *
 *  Author: smarttthings@mlabs.us
 *  Date: 2014-03-26
 */
 // for the UI
preferences {
    input("SparkID", "text", title: "Device ID")
    input("AccessToken", "text", title: "Access Token")
}
    
metadata {
	definition (name: "Path Lighting Color Control", author: "smarttthings@mlabs.us") {
		capability "Switch"
		capability "Color Control"
}    
    tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on",  label: '${name}', action: "switch.off", nextState: "off", icon: "st.switches.switch.on",  backgroundColor: "#79b821"
			state "off", label: '${name}', action: "switch.on",  nextState: "on",  icon: "st.switches.switch.off", backgroundColor: "#ffffff"
         }
        controlTile("rgbSelector", "device.color", "color", height: 3, width: 3, inactiveLabel: false) {                                                                            
            state "color", action:"color control.setColor"                                                                                                                                           
         } 
     main (["switch"])
     details (["rgbSelector", "switch"])
  }
}

def on() {
	control 'on'
}

def off() {
	control 'off'
}

def setHue() {
	log.debug "Executing 'setHue'"
	// TODO: handle 'setHue' command
}

def setSaturation() {
	log.debug "Executing 'setSaturation'"
	// TODO: handle 'setSaturation' command
}

def setColor(value) {
	log.debug "Executing 'setColor'"
	log.debug "Hex Value: ${value.hex}"                                                                                                                                        
    setLEDrgbhex '${value.hex}'
} 

def parse(String description) {
	log.error "This device does not support incoming events"
	return null
}

private setLEDrgbhex(state) {
	httpPost(
		uri: "https://api.spark.io/v1/devices/${SparkID}/hex",
        body: [access_token: AccessToken, command: state],  
	) {response -> log.debug (response.data)}
}

private control(state) {
	httpPost(
		uri: "https://api.spark.io/v1/devices/${SparkID}/control",
        body: [access_token: AccessToken, command: state],  
	) {response -> log.debug (response.data)}
}