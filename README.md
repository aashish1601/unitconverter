UnitConverterPro
🚀 A modern Unit Converter Android App built with Jetpack Compose and C++ (NDK) for fast native unit calculations.

📱 Features
Convert Length, Temperature, and Weight units

Supports Dark & Light Mode

Speech services for voice input

Jetpack Compose UI

Native C++ via JNI for high-performance logic

Lightweight & minimal

🛠️ How to Build and Run
bash
Copy
Edit
git clone https://github.com/your-username/UnitConverterPro.git
cd UnitConverterPro
Open the project in Android Studio (Koala or later)

Ensure NDK & CMake are installed (via SDK Manager)

Build with:

Build > Clean Project

Build > Rebuild Project

Click ▶️ Run

🧠 How NDK is Used
Conversions are processed in unitconverterpro.cpp

Kotlin calls C++ through JNI

JNI functions implemented using extern "C" for:

convertLength()

convertTemperature()

convertWeight()

🖼️ Screenshots
<div align="center"> <img src="https://github.com/user-attachments/assets/43d882d7-1f35-4159-a446-fabf65aa0366" alt="Home Screen" width="30%"> <img src="https://github.com/user-attachments/assets/894454f8-5dd0-46d9-b5c7-e09eefcd8268" alt="Dropdown" width="30%"> <img src="https://github.com/user-attachments/assets/b50b0b02-16a9-4751-b430-9bd05cb4ea2f" alt="Result" width="30%"> <br/><br/> <img src="https://github.com/user-attachments/assets/061cc9ed-9b75-4480-b984-7ab9a76a94dd" alt="Dark Mode" width="30%"> </div>
📦 Tech Stack
Kotlin + Jetpack Compose

Android NDK (C++)

JNI Bridge

Android Studio (Koala)
