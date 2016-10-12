/**
 *  Hallway Dimmer
 *
 *  Author: smarttthings@mlabs.us
 *  Date: 2013-12-10
 */
preferences {
	section("Title") {
		// TODO: put inputs here
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers