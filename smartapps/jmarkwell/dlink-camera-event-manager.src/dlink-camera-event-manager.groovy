/**
 *  D-Link Camera Event Manager
 *  Build 2018061801
 *
 *  Adapted from Ben Lebson's (GitHub: blebson) Smart Security Camera SmartApp that is designed to work with his D-Link
 *  series of device handlers.
 *
 *  Copyright 2018 Jordan Markwell
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  ChangeLog:
 *      
 *      20180618
 *          01: moveLock will no longer extend motion locking for the position it is currently in.
 *
 *      20180510
 *          01: Removed the moveLockOff() and moveLockHandler() functions.
 *          02: The attribute, "switch6", will now be known as, "PTZPos".
 *          03: The function, "sendMessage()", will now be known as, "eventHandler()".
 *          04: Code cleanup.
 *
 *      20171124
 *          01: Removed conditions in the moveLockOff() function.
 *          02: Added moveLockOff() calls in installed() and updated() functions.
 *
 *      20170906
 *          01: Increased photo burst delay to 8 seconds.
 *
 *      20170825
 *          01: Added debug logging setting.
 *
 *      20170824
 *          01: Functionized setting of moveDelay and de-functionized photoLockOff() by setting conditions on snap().
 *          02: Made the menus more fancy.
 *
 *      20170823
 *          01: Mode based reset didn't fix the problem with photoLock. Mode restrictions appear to exist in a higher level
 *              process. Converted photoLock from a delay based system to a time based system.
 *
 *      20170821
 *          01: If movement is enabled and the camera is not in the requested position for a photo, snap() will now be
 *              triggered by movement events from the camera.
 *          02: The lock, photoLock is instance specific and sometimes hangs on a mode change. Adding a mode change
 *              subscription that will reset the lock.
 *
 *      20170818
 *          01: Converted moveLock to a device attribute with corresponding functions so that the lock works in conjunction
 *              with other instances of D-Link Camera Event Manager.
 *
 *      Earlier:
 *          Dwell time following a motion event is now a preference.
 *          Added ability to return to home position after having moved to a preset position.
 *          Added logic to keep the app from sending duplicitous commands.
 *          Added movement locking mechanism to keep the camera from spazzing out when there is a lot of activity.
 *          Added ability to limit photos taken while in Home mode.
 *          Added a 4 second delay before taking a photo after movement has occurred to ensure that the camera has
 *              arrived at the requested location before the photo is taken. This may work more efficiently if a change
 *              in the switch6 attribute can trigger the photo burst...
 */
 
definition(
    name:        "D-Link Camera Event Manager",
    namespace:   "jmarkwell",
    author:      "Jordan Markwell",
    description: "For D-Link cameras using my device handlers. Move to preset positions, take photos, record video clips and send notifications.",
    category:    "Safety & Security",
    iconUrl:     "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when.png",
    iconX2Url:   "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when@2x.png",
    iconX3Url:   "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when@2x.png",
    pausable:    true
)

preferences {
    page(name: "mainPage")
    page(name: "eventPage")
    page(name: "cameraPage")
    page(name: "notificationPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", install: true, uninstall: true) {
        section() {
            paragraph title: "D-Link Camera Event Manager", "This app is compatible only with D-Link cameras using device handlers by BLebson or jmarkwell"
        }
        section("Settings") {
            href "eventPage", title: "Events", required: true
            href "cameraPage", title: "Camera Settings", required: true
            href "notificationPage", title: "Notification Settings"
            input name: "debug", title: "Debug Logging", type: "bool", defaultValue: "false"
        }
        section("") {
            mode(title: "Set for specific mode(s)")
            label(title: "Assign a name", required: false)
        }
    }
}

def eventPage() {
    dynamicPage(name: "eventPage", title: "Events") {
        section() {
            input "motion", "capability.motionSensor", title: "Motion Detected", required: false, multiple: true
            input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
            input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
            input "switchOn", "capability.switch", title: "Switch Turned On", required: false, multiple: true
            input "presenceArrival", "capability.presenceSensor", title: "Someone Arrives", required: false, multiple: true
            input "presenceDeparture", "capability.presenceSensor", title: "Someone Leaves", required: false, multiple: true
        }
    }
}

def cameraPage() {
    dynamicPage(name: "cameraPage") {
        section("Camera Settings") {
             input "camera", "capability.imageCapture", title: "Camera", required: true
             input name: "recordVideo", title: "Record Video", type: "bool", defaultValue: "false", required: false
             input name: "motionDuration", title: "Seconds of Inactivity after which to Stop Recording (motion event)", type: "number", defaultValue: 20, required: true
             input name: "nonMotionDuration", title: "Duration of Video Clip (non-motion event)", type: "number", defaultValue: 60, required: true
             input name: "takePhoto", title: "Take Still Photo", type: "bool", defaultValue: "true", required: false
             input name: "burst", title: "Number of Photos to Take", type: "number", defaultValue: 3, required: true
             input name: "burstLimit", title: "In Home Mode, Allow Photo Burst Once Every X Minutes (0 for no limit)", type: "number", defaultValue: 5, required: true
        }
        section("Movement Options") {
            input name: "moveEnabled", title: "Pan to Preset Position (PTZ cameras only)", type: "bool", defaultValue: "false", required: false
            input name: "presetNum", title: "Preset Position Number", type: "number", defaultValue: 1 , required: true
            input name: "returnHome", title: "Return Home After Recording Stops", type: "bool", defaultValue: "true", required: false
        }
    }
}

def notificationPage() {
    dynamicPage(name:"notificationPage", title:"Notification Settings") {
        section("Send this message in a push notification") {
            input "messageText", "text", title: "Message Text", required: false
        }
        section("Send message as text to this number") {
            input("recipients", "contact", title: "Send notifications to", required: false) {
                input "phone", "phone", title: "Phone Number", required: false
            }
        }
    }
}

def installed() {
    goHome()
    
    log.debug "Installed with settings: ${settings}"
    
    subscribeToEvents()
}

def updated() {
    // clear the cruft
    state.clear()
    
    goHome()
    
    log.debug "Updated with settings: ${settings}"
    
    unsubscribe()
    subscribeToEvents()
}

def subscribeToEvents() {
    subscribe(contact, "contact.open", eventHandler)
    subscribe(acceleration, "acceleration.active", eventHandler)
    subscribe(motion, "motion.active", eventHandler)
    subscribe(motion, "motion.inactive", eventHandler)
    subscribe(switchOn, "switch.on", eventHandler)
    subscribe(presenceArrival, "presence.present", eventHandler)
    subscribe(presenceDeparture, "presence.not present", eventHandler)
    subscribe(camera, "PTZPos", positionHandler)
}

def eventHandler(evt) {
    def moveLockTime = camera.currentValue("moveLockTime") ?: 0
    
    if (debug) { log.debug "$evt.name: $evt.value" }
    
    // new events will modify the timeframe
    unschedule(goHome)
    unschedule(videoOff)
    
    if ( (evt.name == "motion") && (evt.value == "active") ) {
        if (recordVideo) {
            log.debug "Turning video recording on. ($evt.name: $evt.value)"
            camera.vrOn()
        }
        
        if (moveEnabled) {
            if ( (now() > moveLockTime) && (state.positionState != presetNum) ) {
                log.debug "Moving to preset $presetNum. ($evt.name: $evt.value)"
                camera.presetCommand(presetNum)
                
                if (debug) { log.debug "Setting $motionDuration second movement request lockout. ($evt.name: $evt.value)" }
                moveLockOn()
                
                moveDelayOn()
            }
            else if (state.positionState == presetNum) {
                // Enabling the following block will extend moveLock time periods when events (like a motion detection) take place.
                // if (now() <= moveLockTime) {
                    // if (debug) { log.debug "Setting $motionDuration second movement request lockout. (rescheduled) ($evt.name: $evt.value)" }
                // }
                // else {
                    // if (debug) { log.debug "Setting $motionDuration second movement request lockout. ($evt.name: $evt.value)" }
                // }
                // moveLockOn()
                
                snap()
            }
        }
        else { // if (!moveEnabled)
            snap()
        }
    }
    else if ( (evt.name == "motion") && (evt.value == "inactive") ) {
        if (recordVideo) {
            if (debug) { log.debug "Turning video recording off in $motionDuration seconds. ($evt.name: $evt.value)" }
            runIn(motionDuration, videoOff)
        }
        
        if ( (moveEnabled) && (returnHome) && (state.positionState != 1) ) {
            if (debug) { log.debug "Going home in $motionDuration seconds. ($evt.name: $evt.value)" }
            runIn(motionDuration, goHome)
        }
    }
    else if (evt.name != "motion") {
        if (recordVideo) {
            log.debug "Turning video recording on. ($evt.name: $evt.value)"
            camera.vrOn()
            
            if (debug) { log.debug "Turning video recording off in $motionDuration seconds. ($evt.name: $evt.value)" }
            runIn(nonMotionDuration, videoOff)
        }
        
        if (moveEnabled) {
            if ( (now() > moveLockTime) && (state.positionState != presetNum) ) {
                log.debug "Moving to preset $presetNum. ($evt.name: $evt.value)"
                camera.presetCommand(presetNum)
                
                if (debug) { log.debug "Setting $nonMotionDuration second movement request lockout. ($evt.name: $evt.value)" }
                moveLockOn()
                
                moveDelayOn()
                
                if ( (returnHome) && (presetNum != 1) ) { // if camera is not moving to home position
                    if (debug) { log.debug "Going home in $nonMotionDuration seconds. ($evt.name: $evt.value)" }
                    runIn(nonMotionDuration, goHome)
                }
            }
            else if (state.positionState == presetNum) { // if camera is in preset position
                // Enabling the following block will extend moveLock time periods when events (like a motion detection) take place.
                // if (now() <= moveLockTime) {
                    // if (debug) { log.debug "Setting $motionDuration second movement request lockout. (rescheduled) ($evt.name: $evt.value)" }
                // }
                // else {
                    // if (debug) { log.debug "Setting $motionDuration second movement request lockout. ($evt.name: $evt.value)" }
                // }
                // moveLockOn()
                
                snap()
                
                if ( (returnHome) && (state.positionState != 1) ) { // if camera is not in home position
                    if (debug) { log.debug "Going home in $nonMotionDuration seconds. (rescheduled) ($evt.name: $evt.value)" }
                    // goHome() unscheduled at start of function
                    runIn(nonMotionDuration, goHome)
                }
            }
        }
        else { // if (!moveEnabled)
            snap()
        }
    }
    
    if ( !( (evt.name == "motion") && (evt.value == "inactive") ) ) {
        sendNotification()
    }
}

def positionHandler(evt) {
    // log.debug "positionHandler: [$evt.name: $evt.value]"
    
    // this particular evt.value is also known as camera.currentValue("PTZPos")
    switch (evt.value) {
        case "home":
            state.positionState = 1
            break
        case "presetOne":
            state.positionState = 1
            break
        case "presetTwo":
            state.positionState = 2
            break
        case "presetThree":
            state.positionState = 3
            break
        default:
            state.positionState = null
            break
    }
    
    if (debug) { log.debug "The camera has moved to preset ${state.positionState}." }
    
    if (state.moveDelay) {
        state.moveDelay = false
        snap()
    }
}

def sendNotification() {
    if (messageText) {
        if (location.contactBookEnabled) {
            sendNotificationToContacts(messageText, recipients)
        }
        else {
            sendPush(messageText)
            if (phone) {
                sendSms(phone, messageText)
            }
        }
    }
}

def snap() {
    if ( (takePhoto) && ( (location.mode != "Home") || (!burstLimit) || ( (location.mode == "Home") && (burstLimit) && (now() > state.photoLockTime) ) ) && (state.positionState == presetNum) ) {
        photoLockOn()
        
        log.debug "Taking ${burst} photo(s) with a 8 second delay."
        camera.take()
        (burst - 1).times {
            camera.take([delay: 8000])
        }
    }
}

def videoOff() {
    log.debug "Turning video recording off."
    camera.vrOff()
}

def goHome() {
    if (state.positionState != 1) {
        log.debug "Moving to home position."
        camera.home()
    }
}

def moveDelayOn() {
    if (takePhoto) {
        if (debug) { log.debug "Photo will be taken when the camera reaches its destination preset." }
        state.moveDelay = true
    }
}

def moveLockOn() {
    def moveLockTime = camera.currentValue("moveLockTime") ?: 0
    if (now() > moveLockTime) {
        camera.moveLockOn(motionDuration)
    }
}

// photoLock is by design a lock local to each instance of D-Link Camera Event Manager
// this allows events from other instances to trigger a photo if allowed by their own independent locks
def photoLockOn() {
    if ( (location.mode == "Home") && (burstLimit) ) {
        if (debug) { log.debug "Setting $burstLimit minute photo request lockout. (location.mode: $location.mode)" }
        state.photoLockTime = now() + (60000 * burstLimit)
        if (debug) { log.debug "now(): ${now()} < photoLockTime: ${state.photoLockTime}" }
    }
}
