Welcome to the android-multitouch-controller project.

This project currently comprises three Android sub-projects:
 1. MTController, the MultiTouch Controller class for Android (see below);
 2. MTVisualizer, the source code for the app "MultiTouch Visualizer 2" on Google Play;
 3. MTPhotoSortr, a demo app showing how to use the MultiTouch Controller class.

This MultiTouch Controller class makes it much easier to write multitouch applications for Android:
- It filters out "event noise" on Synaptics touch screens (G1, MyTouch, Nexus One) -- for example, when you have two touch points down and lift just one finger, each of the ordinates X and Y can be lifted in separate touch events, meaning you get a spurious motion event (or several events) consisting of a sudden fast snap of the touch point to the other axis before the correct single touch event is generated.
- It simplifies the somewhat messy and inconsistent MotionEvent touch point API -- this API has grown from handling single touch points, potentially with packaged event history (Android 1.6 and earlier) to multiple indistinguised touch points (Android 2.0) to the potential for handling multiple touch points that are kept distinct even if lower-indexed touchpoints are raised (and thus each point has an index and generates its own indexed touch-up/touch-down event). All this means there are a lot of API quirks you have to be aware of. This MultiTouch Controller class simplifies getting access to these events for applications that just want event positions and up/down status.
- The controller also supports pinch-zoom, including tracking the transformation between screen coordinates and object coordinates. It correctly centers the pinch operation about the center of the pinch (not about the center of the screen, as with most of the "Google Experience" apps that added their own pinch-zoom capability in Android-2.x). This also means that you can do a combined pinch-drag operation that will simultaneously translate and scale an object. This is the only natural way to implement pinch-zoom, and subconsciously feels much more natural than scaling about the center of the screen. Compare pinch-zoom in Google Maps to Fractoid (in Market) to see what I mean -- Fractoid uses this multitouch controller code.
- The controller was recently updated to support pinch-rotate, allowing you to physically twist objects using two touch points on the screen. In fact all of rotate, scale and translate can be simultaneously adjusted based on relative movements of the first two touch points. NOTE: rotation is quirky on older touchscreen devices that use a Synaptics or Synaptics-like "2x1D" sensor (G1, MyTouch, Droid, Nexus One) and not a true 2D sensor like the HTC Incredible or HTC EVO 4G. The quirky behavior results from "axis snapping" when the two points get close together in X or Y, and "ordinate confusion" where (x1,y1) and (x2,y2) get confused for (x1,y2) and (x2,y1). There is no way around this other than to keep the two fingers in the same two relative quadrants (i.e. keep them on a leading or a trailing diagonal), or to disallow rotation on these devices. (In spite of misinformation on the Web, there is also no firmware or software update that can fix this problem, it is a hardware limitation. Hopefully all newer phones will have a true 2D touch sensor.)
- I also added anisotropic scaling as an alternative to using the rotation and scale information, so that if you are scaling something like a graph which has a different X and Y scale, you can dynamically change both scales by simultaneously stretching in horizontal and vertical directions.
- The controller makes it very easy to work with a canvas of separate objects (e.g. a stack of photos), each of which can be separately dragged with a single touch point or scaled with a pinch operation.

An example of how to use the API is included in the "MTPhotoSortr" demo app in the source repository linked above. (The source is not very polished but it shows you the basics of how to use the controller.) A second example is the app in the Android Market called "MultiTouch Visualizer 2". The source for this app is available in this source code repository too.

# Known bugs
For pinch-zoom, currently the center of the scaling operation is the center of the pinched object, not the midpoint between the two touch points on the screen, due to an error in converting between screen and object coordinates and vice versa. This needs to be fixed, and would be a nice (and relatively simple) contribution if anyone is willing to submit a patch :-) It requires some understanding of composition of transformations. The current code does not use matrix transformations, but it might be worth converting the code to do matrix math. (There are also a few other issues listed on the Issues page.)

# License
Licensed under the MIT license.

# In use by
Please send me a note if you use this code, I'm interested to see where it is used. So far I know of (in approximate reverse chronological order of receiving notification):
- Photo Designs : Photo Editor by ANH2 team
- Instachaka by Rodrigo Arjona
- Photo Editor : Photo Collage by Vinicorp
- Avare app for aviators, available on Play and github
- OSMDroid
- Wifi Compass by Thomas Konrad and Paul Wölfel, for triangulating Wifi access point locations indoors by using the accelerometer to trace how far you have walked.
- KiwiViewer 3D isosurface visualizer by Pat Marion and Kitware
- Compass VO by Alin Berce
- mCatalog by AHG (Alex Heiphetz)
- File Manager and File Manager HD by Rhythm Software
- Collage by David Erosa
- RoidRage by Paul Bourke
- The Fundroid development platform by Ignacio Bautiste Cuervo
- Androzic by Andrey Novikov
- Go Scoring Camera by Jim Babcock
- Fractoid by Dave Byrne
- Face Frenzy by Mickael Despesse
- Yuan Chin's fork of ADW Launcher that supports multitouch
- mmin's handyCalc calculator
- My own app "MultiTouch Visualizer 2"
- Formerly: The browser in cyanogenmod (and before that, JesusFreke), and other firmwares like dwang5. This usage has been replaced with official pinch/zoom in Maps, Browser and Gallery3D as of API level 5.
- Other mentions: Paul Bourke's blog post on using android-multitouch-controller

# Author:
Luke Hutchison -- luke.dot.hutch.at.gmail.dot.com (@LH on Twitter)
