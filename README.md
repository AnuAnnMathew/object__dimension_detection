Object Size Estimation Android Application

Project Description
This Android application estimates the dimensions (height and width) of a target object using the device's 
camera. It integrates TensorFlow Lite for object detection with the ssd_mobilenet_v1 model and uses CameraX 
for capturing live camera feed. The app detects both a reference object (e.g., a coin or pencil)and the 
target object and calculates the target object's dimensions relative to the reference.

Setup and Build Instructions
1.Prerequisites:
    1. Android Studio Arctic Fox or newer.
    2. Minimum Android SDK: API level 21.
    3. TensorFlow Lite dependencies.
2.Setup Instructions:
    Clone the repository - git clone <repo_url>
    Open the project in Android Studio.
    Sync the project to resolve dependencies.
    Place the ssd_mobilenet_v1.tflite model file in the assets folder.
3.Build Instructions:
In Android Studio, click on Build > Rebuild Project to ensure the project compiles successfully.
Connect an Android device or use an emulator (API level 21+).
Run the app by clicking Run > Run 'app'.

Usage Guide
1.Launch the application.
2.Allow camera permissions when prompted.
3.Place the target object on a flat surface, making sure its within the camera frame.
4.The app will use the ssd_mobilenet_v1 model to detect the objects and draw bounding boxes around them.
5.The app will display the estimated size of the target object in a clear, user-friendly manner on the screen

Assumptions and Limitations
Bounding Box Precision: The ssd_mobilenet_v1 model provides bounding boxes, and the calculated dimensions 
are an approximation based on these boxes.
Model Limitations: The ssd_mobilenet_v1 model may not perfectly detect all objects under varying lighting or
angles.

Potential Enhancements
Improved Object Detection: Integrate a more advanced model for higher accuracy and better detection of 
objects under different lighting and angles.
3D Object Detection: Incorporate depth-sensing to handle scenarios where objects aren't perfectly aligned
on the same plane.
Calibration Mode: Introduce a calibration feature to improve the accuracy of size estimation based on the
user's device and environment.

