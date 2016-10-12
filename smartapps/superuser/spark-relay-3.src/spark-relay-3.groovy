/**
 *  Spark Module 2 - Relay 3
 *
 *  Author: smarttthings@mlabs.us
 *  Date: 2014-01-11
 */

preferences {
	section("Use this switch for Relay 3..") {
		input "Relay3", "capability.switch", title: "Relay 3?"
	}
    
    section("Spark Core ID..") {
		input "SparkID", "parameters", title: "Spark Core ID?"
	}
    
    section("Access Token..") {
		input "AccessToken", "parameters", title: "Access Token?"
	}

}

def installed() {
	log.trace "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
	subscribe(Relay3, "switch", Relay3Handler)
}

def deckHandler(evt) {
	log.trace evt.value
    if (evt.value == "on") {
		turnRelay3On('Relay3')
	} else if (evt.value == "off") {
		turnRelayOffDeck('Relay3')
	}
}


def turnRelay3On(group) {
    if (group == "") {
    	group = "all"
    }
	def successClosure = { response ->       
                log.debug "Request was successful, $response"                                                                          
               }                                            
               def postBody = "access_token=${AccessToken}&params=r3%2CLOW"
               httpPost("https://api.spark.io/v1/devices/${SparkID}/relay", postBody, successClosure)
}

def turnRelay3Off(group) {
	log.trace "turnRelayOff: ${group}"
    
    if (group == "") {
    	group = "all"
}        
    def successClosure = { response ->       
                log.debug "Request was successful, $response"                                                                          
               }                                            
               def postBody = "access_token=${AccessToken}&params=r3%2CHIGH"
               httpPost("https://api.spark.io/v1/devices/${SparkID}/relay", postBody, successClosure)
}
