/**
 *  D-Link Camera Event Manager
 *  Build 2017082501
 *
 *  Adapted from Ben Lebson's (GitHub: blebson) Smart Security Camera SmartApp that is designed to work with his D-Link
 *  series of device handlers.
 *
 *  Copyright 2017 Jordan Markwell
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
 *      Earlier:
 *          Dwell time following a motion event is now a preference
 *          Added ability to return to home position after having moved to a preset position
 *          Added logic to keep the app from sending duplicitous commands
 *          Added movement locking mechanism to keep the camera from spazzing out when there is a lot of activity
 *          Added ability to limit photos taken while in Home mode
 *          Added a 4 second delay before taking a photo after movement has occurred to ensure that the camera has
 *              arrived at the requested location before the photo is taken. This may work more efficiently if a change
 *              in the switch6 attribute can trigger the photo burst...
 *      
 *      2017081801:
 *          Converted moveLock to a device attribute with corresponding functions so that the lock works in conjunction
 *              with other instances of D-Link Camera Event Manager
 *      
 *      2017082101:
 *          If movement is enabled and the camera is not in the requested position for a photo, snap() will now be
 *              triggered by movement events from the camera
 *          The lock, photoLock is instance specific and sometimes hangs on a mode change. Adding a mode change
 *              subscription that will reset the lock
 *      
 *      2017082301:
 *          Mode based reset didn't fix the problem with photoLock. Mode restrictions appear to exist in a higher level
 *              process. Converted photoLock from a delay based system to a time based system
 *      
 *      2017082401:
 *          Functionized setting of moveDelay and de-functionized photoLockOff() by setting conditions on snap()
 *          Made the menus more fancy
 *      
 *      2017082501:
 *          Added debug logging setting
 *      
 */
definition(
    name: "D-Link Camera Event Manager",
    namespace: "jmarkwell",
    author: "Jordan Markwell",
    description: "For D-Link cameras using BLebson's device handlers. Move to preset positions, take photos, record video clips and send notifications.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when@2x.png"
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
    subscribe(contact, "contact.open", sendMessage)
    subscribe(acceleration, "acceleration.active", sendMessage)
    subscribe(motion, "motion.active", sendMessage)
    subscribe(motion, "motion.inactive", sendMessage)
    subscribe(switchOn, "switch.on", sendMessage)
    subscribe(presenceArrival, "presence.present", sendMessage)
    subscribe(presenceDeparture, "presence.not present", sendMessage)
    subscribe(camera, "switch6", switch6Handler)
    subscribe(camera, "moveLock", moveLockHandler)
}

def sendMessage(evt) {
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
            if ( (!state.moveLock) && (state.positionState != presetNum) ) {
                log.debug "Moving to preset $presetNum. ($evt.name: $evt.value)"
                camera.presetCommand(presetNum)
                
                if (debug) { log.debug "Setting $motionDuration second movement request lockout. ($evt.name: $evt.value)" }
                moveLockOn()
                runIn(motionDuration, moveLockOff)
                
                moveDelayOn()
            } else if (state.positionState == presetNum) {
                if (state.moveLock) {
                    if (debug) { log.debug "Setting $motionDuration second movement request lockout. (rescheduled) ($evt.name: $evt.value)" }
                    unschedule(moveLockOff)
                } else {
                    if (debug) { log.debug "Setting $motionDuration second movement request lockout. ($evt.name: $evt.value)" }
                }
                moveLockOn()
                runIn(motionDuration, moveLockOff)
                
                snap()
            }
        } else { // if (!moveEnabled)
            snap()
        }
    } else if ( (evt.name == "motion") && (evt.value == "inactive") ) {
        if (recordVideo) {
            if (debug) { log.debug "Turning video recording off in $motionDuration seconds. ($evt.name: $evt.value)" }
            runIn(motionDuration, videoOff)
        }
        
        if ( (moveEnabled) && (returnHome) && (state.positionState != 1) ) {
            if (debug) { log.debug "Going home in $motionDuration seconds. ($evt.name: $evt.value)" }
            runIn(motionDuration, goHome)
            // do not set moveLock here if you want new events to update the timeframe
        }
    } else if (evt.name != "motion") {
        if (recordVideo) {
            log.debug "Turning video recording on. ($evt.name: $evt.value)"
            camera.vrOn()
            
            if (debug) { log.debug "Turning video recording off in $motionDuration seconds. ($evt.name: $evt.value)" }
            runIn(nonMotionDuration, videoOff)
        }
        
        if (moveEnabled) {
            if ( (!state.moveLock) && (state.positionState != presetNum) ) {
                log.debug "Moving to preset $presetNum. ($evt.name: $evt.value)"
                camera.presetCommand(presetNum)
                
                if (debug) { log.debug "Setting $nonMotionDuration second movement request lockout. ($evt.name: $evt.value)" }
                moveLockOn()
                runIn(nonMotionDuration, moveLockOff)
                
                moveDelayOn()
                
                if ( (returnHome) && (presetNum != 1) ) { // if camera is not moving to home position
                    if (debug) { log.debug "Going home in $nonMotionDuration seconds. ($evt.name: $evt.value)" }
                    runIn(nonMotionDuration, goHome)
                }
            } else if (state.positionState == presetNum) { // if camera is in preset position
                if (state.moveLock) {
                    if (debug) { log.debug "Setting $motionDuration second movement request lockout. (rescheduled) ($evt.name: $evt.value)" }
                    unschedule(moveLockOff)
                } else {
                    if (debug) { log.debug "Setting $motionDuration second movement request lockout. ($evt.name: $evt.value)" }
                }
                moveLockOn()
                runIn(motionDuration, moveLockOff)
                
                snap()
                
                if ( (returnHome) && (state.positionState != 1) ) { // if camera is not in home position
                    if (debug) { log.debug "Going home in $nonMotionDuration seconds. (rescheduled) ($evt.name: $evt.value)" }
                    // goHome() unscheduled at start of function
                    runIn(nonMotionDuration, goHome)
                }
            }
        } else { // if (!moveEnabled)
            snap()
        }
    }
    
    if ( !( (evt.name == "motion") && (evt.value == "inactive") ) ) {
        sendNotification()
    }
}

def switch6Handler(evt) {
    // log.debug "switch6Handler: [$evt.name: $evt.value]"
    
    // this particular evt.value is also known as camera.currentValue("switch6")
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

def moveLockHandler(evt) {
    // log.debug "moveLockHandler: [$evt.name: $evt.value]"
    
    // this particular evt.value is also known as camera.currentValue("moveLock")
    switch (evt.value) {
        case "on":
            state.moveLock = true
            break
        default:
            state.moveLock = false
            break
    }
    
    if (debug) { log.debug "moveLock: ${state.moveLock}" }
}

def sendNotification() {
    if (messageText) {
        if (location.contactBookEnabled) {
            sendNotificationToContacts(messageText, recipients)
        } else {
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
        
        log.debug "Taking ${burst} photo(s) with a 7 second delay."
        camera.take()
        (burst - 1).times {
            camera.take([delay: 7000])
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
    if (!state.moveLock) {
        camera.moveLockOn()
    }
}

def moveLockOff() {
    if (state.moveLock) {
        camera.moveLockOff()
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
