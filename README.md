# D-Link Camera Event Manager
D-Link Camera Event Manager
Build 2017082501

Adapted from Ben Lebson's (GitHub: blebson) Smart Security Camera SmartApp that is designed to work with his D-Link
series of device handlers.

Copyright 2017 Jordan Markwell

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
for the specific language governing permissions and limitations under the License.

ChangeLog:
      
    Earlier:
        Dwell time following a motion event is now a preference
        Added ability to return to home position after having moved to a preset position
        Added logic to keep the app from sending duplicitous commands
        Added movement locking mechanism to keep the camera from spazzing out when there is a lot of activity
        Added ability to limit photos taken while in Home mode
        Added a 4 second delay before taking a photo after movement has occurred to ensure that the camera has
            arrived at the requested location before the photo is taken. This may work more efficiently if a change
            in the switch6 attribute can trigger the photo burst...
    
    2017081801:
        Converted moveLock to a device attribute with corresponding functions so that the lock works in conjunction
            with other instances of D-Link Camera Event Manager
    
    2017082101:
        If movement is enabled and the camera is not in the requested position for a photo, snap() will now be
            triggered by movement events from the camera
        The lock, photoLock is instance specific and sometimes hangs on a mode change. Adding a mode change
            subscription that will reset the lock
    
    2017082301:
        Mode based reset didn't fix the problem with photoLock. Mode restrictions appear to exist in a higher level
            process. Converted photoLock from a delay based system to a time based system
    
    2017082401:
        Functionized setting of moveDelay and de-functionized photoLockOff() by setting conditions on snap()
        Made the menus more fancy
    
    2017082501:
        Added debug logging setting
