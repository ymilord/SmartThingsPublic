
 
definition(
    name: "Dusk-to-Dawn Dimming Motion Light with Optional Ambient Light Trigger",
    namespace: "My Apps",
    author: "ymilord",
    description: "Turn lights on at dusk at a dimmed level, brighten when motion is sensed temporarily. Turn off at dawn.  Adjustable dim level, bright level, bright time, down offest, dusk offset. Dusk and dawn times based on location (either zip code or hub location)",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",  
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    //todo:  replace icon with something appropriate.  Maybe a lightbulb with the moon and stars


preferences {
	section("Select Motion Sensor(s) you want to Use") {
        input "motions", "capability.motionSensor", title: "Motion Detectors (leave blank for just dusk-to-dawn function)", required: false, multiple: true
	}
    section("Select Dimmers you want to Use") {
        input "switches", "capability.switchLevel", title: "Dimmer Switches", required: false, multiple: true
	}
    section ("Set Bright and Dim Levels and Bright Time") {
		input "DimLevelStr", "enum", title: "Dimmed or Dusk-to-Dawn Level %", required: true, 
        	options: ["0","5","10","15","20","30","50","75","100"], defaultValue: "20"
        
        input "BrightLevelStr", "enum", title: "Motion-Sensed Level %", required: true, 
        	options: ["100","75","50"], defaultValue: "100"
        
        input "DelayMinStr", "enum", title: "Bright Delay After Motion Stops, minutes", required: true, 
        	options: ["1","3","5","10","15","30","60"], defaultValue: "5"
        	}
    section ("Turn on lights to dimmed state based on measured light also (optional)") {
    	input "LightMeter", "capability.illuminanceMeasurement", title: "Light Meters", required: false, multiple: false
		input "LuxSetPointStr", "enum", title: "Lux level set point", required: false, 
        	options: ["50","100","200","400"], defaultValue: "50"
           	}
    section ("Zip code (optional, defaults to location coordinates)...") {
		input "zipCode", "text", title: "Enter 5-digit ZIP code", required: false
	}
    section ("Sunrise offset (optional)...") {
		input "sunriseOffsetStr", "enum", title: "Offset in minutes (use negative for before)", required: false,
        	options: ["-120", "-60", "-30", "-15", "-5", "5", "15", "30", "60", "120"]
		
	}
	section ("Sunset offset (optional)...") {
		input "sunsetOffsetStr", "enum", title: "Offset in minutes (use negative for before)", required: false,
        	options: ["-120", "-60", "-30", "-15", "-5", "5", "15", "30", "60", "120"]
	}
}
def installed() {
	log.debug "Installed with settings: ${settings} DelayMin: $DelayMin"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}  DelayMin: $DelayMin"

	unsubscribe()
	initialize()
}

def initialize() {
	log.debug "initialize()"
    
	state.DimLevel = DimLevelStr as Integer
    if (state.DimLevel == 100) {
    	state.DimLevel = 99
    }
    state.BrightLevel = BrightLevelStr as Integer
   	if (state.BrightLevel == 100) {
    	state.BrightLevel = 99
    }
    state.DelayMin = DelayMinStr as Integer
    state.LuxSetPoint = LuxSetPointStr as Integer
    
    if(sunriseOffsetStr){
    	state.sunriseOffset = sunriseOffsetStr as Integer
        log.debug "value of sunriseOffset is $state.sunriseOffset minutes, set by user"
        }
    else{
    	state.sunriseOffset = 1
        log.debug "value of sunriseOffset is $state.sunriseOffset minute, arbitrary offset to avoid peak"
    }
    
    if(sunsetOffsetStr){
    	state.sunsetOffset = sunsetOffsetStr as Integer
        log.debug "value of sunsetOffset is $state.sunsetOffset minutes, set by user"
        }
    else{
    	state.sunsetOffset = -1
        log.debug "value of sunsetOffset is $state.sunsetOffset minute, arbitrary offset to avoid peak"
    }
    
    subscribe(motions, "motion.active", handleMotionEvent)
    subscribe(motions, "motion.inactive", handleEndMotionEvent)
    subscribe(LightMeter, "illuminance", handleLuxChange)
	
    initialSunPosition()  
	
    
}

def initialSunPosition() {  
	//Determine if sun is down at time of initializtion and run sunsetHandler() if so
    //Light meter is not evaluated initially, light level first evaluated at first luminance event
    log.debug "initialSunPosition()"
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: state.sunriseOffset, sunsetOffset: state.sunsetOffset)
	def now = new Date()
    def riseTime = s.sunrise
	def setTime = s.sunset
	state.ambient = "light"  //initialize to "light"
    state.sunPosition = "up" //initialize to "up"
	
    if(setTime.before(now) || riseTime.after(now)) {   //before midnight/after sunset or after midnight/before sunset
	  	log.info "Sun is already down, run sunsetHandler"
        sunsetHandler()
    }
    
	scheduleSunEvents()													//setting initial rise/set 
    
    unschedule(scheduleSunEvents)
    schedule(timeToday("12:13",location.timeZone),scheduleSunEvents) 	//subsequent evaluation of rise/set times
    																	//at 12:13PM every day
}

def scheduleSunEvents() {
	//query sunset and sunrise times with offsets applied, schedule handlers for sun events
    //this method based loosely on Sunrise/Sunset, astroCheck()
    log.debug "scheduleSunEvents()"
    
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: state.sunriseOffset, sunsetOffset: state.sunsetOffset)
	def now = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
	
    log.debug "Today's sunrise (with offset applied): $riseTime"
	log.debug "Today's sunset (with offset applied): $setTime"
    log.debug "Now: $now"
		
	unschedule(sunriseHandler)
	
   	schedule(riseTime, sunriseHandler)
	log.info "scheduling sunrise handler for $riseTime.timeString  UTC"
   
	unschedule(sunsetHandler)

    schedule(setTime,sunsetHandler)
	log.info "scheduling sunset handler for $setTime.timeString  UTC"
}

def sunriseHandler() {
	log.info "sunriseHandler()"
    
    state.sunPosition = "up"
    if (LightMeter) {
    	if (state.luxMeasurement < state.LuxSetPoint) {
        log.info "Sunrise occurred but its still dark so leave the lights on"
        }
        else {
        	gotLight()
       		}
        }
    else {
    	gotLight()
        log.info "Light measurement option not selected, sun came up so it is light"
    }
    
}

def sunsetHandler() {
	//Light meter not evaluated here.  Sunset has priority over light measurement in determining state.ambient
	log.info "sunsetHandler()"
    
    state.sunPosition = "down"    
    if (LightMeter) {
    	if (state.ambient == "light"){
        	gotDark()
        }
        else{
        	log.info "Sun went down but it is already dark so do nothing"
        }
 	}
    else{
    	gotDark()
        log.info "Light measurement option not selected, sun went down so it is dark"
    }
}

def gotDark() {
	//Actions to do when it is determined it has transitioned from light to dark
	log.info "gotDark() It got dark according to sunset time (and offset) or light measurement"
	
    switches?.setLevel(30)  //starts at 30 (arbitrary) to make sure lights start (my LEDs won't start at below 20% but run fine)
    state.ambient = "dark"
    
    unschedule(modeDim)
    def fireTime = new Date(new Date().time + 3000) //creates date object 3 seconds in the future
    runOnce(fireTime,modeDim)  //after initial light-off, runs handler to set to selected dim level after 3 seconds
}

def gotLight() {
	//Actions to do when it is determined it has transitioned from dark to light
	log.info "gotLight() It got light according to sunrise time (and offset) and light measurement (if used)"
	
    switches?.setLevel(0)  
    state.ambient = "light"
    unschedule(modeDim) //Cancels any motion-activated bright time 
}

def handleMotionEvent(evt) {
	log.debug "handleMotionEvent() Motion detected . . . ."
    
    if (state.ambient == "dark") {
    	switches?.setLevel(state.BrightLevel)
        state.Level = state.BrightLevel
        unschedule(modeDim)   //in case motion is sensed during delay before scheduled modeDim() call
    	log.debug ". . . set the dimmers to level $state.BrightLevel"
    }
    else 
    {
    	log.debug ". . . but its light, so do nothing"
    }
}

def handleEndMotionEvent(evt) {
	log.debug "handleEndMotionEvent() Motion stopped . . . ."

    if (state.ambient == "dark") {
    	switches?.setLevel(state.BrightLevel)  //does nothing unless sun went down during active motion
        state.Level = state.BrightLevel
    	log.debug ". . . set the dimmers to level $state.BrightLevel if not already there"
        
        runIn((state.DelayMin*60), modeDim)  //delay is number of minutes entered in preferences x 60 to get seconds
                                             //will be unscheduled if it gets light in the meantime
    }
    else 
    {
    	log.debug ". . . but it light, so do nothing"
    }
}

def handleLuxChange(evt) {
	//evalute light meter reading

	log.debug "handleLuxChange()"
    
    state.luxMeasurement = evt.integerValue
    log.debug "Current lux is $state.luxMeasurement"
    
    if (state.luxMeasurement < state.LuxSetPoint) {
    	if (state.ambient == "light"){
        	gotDark()
    	}
    }
    else{
    	if (state.ambient == "dark" && state.sunPosition == "up")  //criteria for delaring 'light' is light measurement above setpoint AND sun is up
        	gotLight()
    	}
}

def modeDim() {   
	//Sets light to dim stat
    
	log.debug "modeDim()  Set lights to dimmed state" 
    
    switches?.setLevel(state.DimLevel)
    state.Level = state.DimLevel
}

def modeBright() {   
	//Sets light to bright stat
    
	log.debug "modeBright()  Set lights to bright state" 
    
    switches?.setLevel(state.BrightLevel)
    state.Level = state.BrightLevel
}
private getLabel() {
	app.label ?: "SmartThings"
}