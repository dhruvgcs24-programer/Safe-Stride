# 🧭 SafeStride — AI Navigation App for the Visually Impaired

SafeStride is an Android-based assistive application that helps visually impaired users navigate safely by detecting obstacles in real time using **computer vision and voice feedback**.

---

## 🎯 Problem Statement

Indoor navigation is challenging for visually impaired individuals due to:

* Lack of GPS support indoors
* Difficulty identifying obstacles in real time
* Limited assistive tools for dynamic environments

SafeStride addresses this by providing **real-time obstacle detection and directional guidance using a smartphone camera**.

---

## 🚀 Key Features

### 📷 Real-Time Camera Processing

* Uses **CameraX API** for live video feed
* Processes frames continuously for detection

### 🤖 AI-Based Object Detection

* Integrated **YOLOv8 model** for detecting obstacles
* Identifies objects like:

  * Walls
  * People
  * Furniture

### 🧭 Directional Awareness (Core Logic)

* Screen divided into:

  * Left
  * Center
  * Right
* Detects where obstacle is located and guides user accordingly

Example:

* “Obstacle on the right, move left”

---

### 🔊 Voice Feedback System

* Integrated **Text-to-Speech (TTS)**
* Provides real-time spoken navigation instructions
* Hands-free usability

---

### 🎨 Clean UI/UX

* Simple and accessible interface
* Focus on usability for visually impaired users
* Minimal distractions

---

## 🛠 Tech Stack

| Layer    | Technology     |
| -------- | -------------- |
| Platform | Android (Java) |
| Camera   | CameraX        |
| AI Model | YOLOv8         |
| Vision   | OpenCV         |
| Audio    | Android TTS    |
| IDE      | Android Studio |

---

## 📁 Project Structure

```bash
SafeStride/
│
├── ui/                 # Activities (CameraActivity, MainActivity)
├── vision/             # YOLOv8 detection logic
├── navigation/         # Direction (left/right/center) logic
├── speech/             # Text-to-Speech module
├── utils/              # Helper functions
```

---

## ⚙️ How It Works

1. Camera captures live video using CameraX
2. YOLOv8 model detects objects in each frame
3. Frame is divided into regions (left/center/right)
4. System determines obstacle position
5. TTS announces navigation instruction

---

## ▶️ How to Run

### 🔹 1. Clone the repository

```bash
git clone https://github.com/dhruvgcs24-programer/Safe-Stride
```

---

### 🔹 2. Open in Android Studio

* Open project folder
* Let Gradle sync complete

---

### 🔹 3. Run the App

* Connect Android device OR use emulator
* Click **Run ▶️**

---

## 🧪 Example Output

* “Obstacle detected on the left”
* “Move slightly right”
* “Path is clear ahead”

---

## ⚠️ Challenges Faced

* Real-time processing latency on mobile devices
* Accurate left/right classification of objects
* Handling low-light detection issues
* Optimizing model performance for Android

---

## 📈 Future Improvements

* Distance estimation (how far obstacle is)
* Voice command interaction
* Integration with wearable devices
* Improved model accuracy and speed
* Navigation path prediction

---

## 👨‍💻 Author

**Dhruv G Nayak**
CSE Engineering Student

---

## ⭐ Contribution

Suggestions and improvements are welcome!
