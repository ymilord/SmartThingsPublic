/**
 *  LIFX Auto Discovery
 *
 *  Author: Andrew Filipowski with parts borrowed from Smartthings Sonos Smartapp
 *  Date: 2014-03-28
 *
 *
 */

preferences {
    page(name:"lifxDiscovery", title:"LIFX Device Setup", content:"lifxDiscovery", refreshTimeout:5)
}

//PAGES
def lifxDiscovery()
{
    int lifxRefreshCount = !state.lifxRefreshCount ? 0 : state.lifxRefreshCount as int
    state.lifxRefreshCount = lifxRefreshCount + 1
    def refreshInterval = 3

    def options = lifxsDiscovered() ?: []

    def numFound = options.size() ?: 0

    if(!state.subscribe) 
    {
        log.debug "subscribe to location"
        subscribe(location, null, locationHandler, [filterEvents:false])
        state.subscribe = true
    }

    //LIFX discovery request every 5 //25 seconds
    if((lifxRefreshCount % 8) == 0) 
    {
        discoverLifxBulbs()
    }

    //setup.xml request every 3 seconds except on discoveries
    if(((lifxRefreshCount % 1) == 0) && ((lifxRefreshCount % 8) != 0)) 
    {
        verifyLifxBulb()
    }

    return dynamicPage(name:"lifxDiscovery", title:"Discovery Started!", nextPage:"", refreshInterval:refreshInterval, install:true, uninstall: true) 
    {
        section("Please wait while we discover your Lifx Bulbs. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.")
        {
            input "selectedLifxBulbs", "enum", required:false, title:"Select LIFX Bulbs (${numFound} found)", multiple:true, options:options
        }
    }

}

private discoverLifxBulbs()
{
    def ip = "10.0.1.161:56780"
    def deviceNetworkId = "0A0001A1:0xDDCC" // "10.0.1.161:56780"
    def uri = "/lights.json"
  
    sendHubCommand(new physicalgraph.device.HubAction("""GET $uri HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
}

def getLifxBulbs()
{
    state.lifxBulbs = state.lifxBulbs ?: [:]
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
    subscribe(location, null, locationHandler, [filterEvents:false])
    subscribe(app, changeMode)
}

def changeMode(evt) {
    
  
    log.debug location.hubs*.firmwareVersionString.findAll { it }
    
    
    
}


def locationHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId

    def parsedEvent = parseEventMessage(description)
    parsedEvent << ["hub":hub]

    if (parsedEvent.headers && parsedEvent.body)
    { // lifx RESPONSES
        def headerString = new String(parsedEvent.headers.decodeBase64())
        def bodyString = new String(parsedEvent.body.decodeBase64())

        def type = (headerString =~ /Content-Type:.*/) ? (headerString =~ /Content-Type:.*/)[0] : null
        def body
        log.debug "LIFX REPONSE TYPE: $type"
        if(type?.contains("json"))
        { //(application/json)
            body = new groovy.json.JsonSlurper().parseText(bodyString)
            log.debug "GOT JSON $body"
            log.debug "LIFX Bulbs found"
            def lifxBulbs = getLifxBulbs()
        }

    }
    else {
        log.debug "cp desc: " + description
    }
}

private def parseEventMessage(Map event) {
    //handles lifx attribute events
    return event
}

private def parseEventMessage(String description) {
    def event = [:]
    def parts = description.split(',')
    parts.each { part ->
        part = part.trim()
        if (part.startsWith('id:')) {
            def valueString = part.split(":")[1].trim()
            event.deviceId = valueString
        }
        else if (part.startsWith('label:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.deviceLabel = valueString
            }
        }
        else if (part.startsWith('site_id:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.deviceSiteId = valueString
            }
        }
        else if (part.startsWith('tags:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.deviceTags = valueString
            }
        }
        else if (part.startsWith('on:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.deviceState = valueString
            }
        }
        else if (part.startsWith('color:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.deviceColor = valueString
            }
        }
        else if (part.startsWith('last_seen:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.deviceLastSeen = valueString
            }
        }
        else if (part.startsWith('seconds_since_seen:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.deviceSecondsSinceSeen = valueString
            }
        }
        else if (part.startsWith('headers')) {
            part -= "headers:"
            def valueString = part.trim()
            if (valueString) {
                event.headers = valueString
            }
        }
        else if (part.startsWith('body')) {
            part -= "body:"
            def valueString = part.trim()
            if (valueString) {
                event.body = valueString
            }
        }
    }

    event
}