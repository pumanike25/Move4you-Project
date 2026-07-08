[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Java-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Backend](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![Encryption](https://img.shields.io/badge/Security-E2EE%20RSA-0052CC?logo=lock&logoColor=white)](#-security-implementation-e2ee)
[![Testing](https://img.shields.io/badge/Tests-JUnit%20%26%20Espresso-4CAF50?logo=github-actions&logoColor=white)](#-testing-suite)

**Move4you** is a native Android application developed as an **Undergraduate Graduation Thesis Project (Lucență)**. It serves as an eco-friendly navigation system and a highly secure social platform, blending mapping functionality, real-time meetup organization, gamified sustainability tracking, and strict zero-knowledge privacy through End-to-End Encryption (E2EE).


## Key Features

### 1. Sustainable Navigation & Location Management
* **Route Planning:** Uses the Google Maps API for real-time route plotting, navigation, and tracking.
* **Favorite Locations:** Allows saving custom points of interest with personalized nicknames/aliases or falling back to default addresses for ultra-fast navigation triggers.
* **History Log:** Tracks previous paths and destinations, allowing effortless route re-visitation.

### 2. End-to-End Encrypted (E2EE) Chat
* **Zero-Knowledge Privacy:** Secure, real-time direct messaging infrastructure where neither Firebase nor any third party can intercept conversations.
* **Asymmetric Cryptography:** Utilizes the RSA algorithm combined with the hardware-backed **Android KeyStore** to ensure text messages remain fully encrypted on transit and at rest.

### 3. Interactive Meetup Planner
* **Direct Integration:** Seamlessly propose meetups inside direct chats by selecting a spot on the map along with a date and time.
* **Dynamic UI States:** Custom real-time meetup cards handle conditional UI rendering based on invitation states (`PENDING`, `ACCEPTED`, or `DECLINED`). Accepted events automatically integrate into the user's saved location registry.

### 4. Eco-Impact Analytics Dashboard
Integrates custom responsive web widgets built with JavaScript libraries (Chart.js and Google Charts) hosted inside native `WebView` containers to visualize walking accomplishments over time:
* **The Virtual Forest Widget:** Converts step metrics to annual CO₂ reduction equivalents, rendering a virtual forest that visualizes the effective workload of equivalent trees.
* **The Fuel Savings Tracker:** Tailors financial gains by evaluating distance against customizable car/engine configurations, showcasing gas liters saved and monetary fuel cost reductions.
* **Activity Heatmaps:** Renders calendar rings based on daily step targets (e.g., Apple Fitness style), filling daily goals with conical gradients.
* **Transport Preference Analytics:** Graphs transport choice breakdowns (Walking vs. Vehicular trips) dynamically.

### 5. Gamification & Profile Customization
* **Achievement Engine:** Automatically unlocks milestone trophies based on cumulative stats (e.g., *First Journey*, *Explorer*, *Frequent Traveler*, *10K Club*, *Marathoner*).
* **Privacy Toggles:** Switch profile settings between **Public** and **Private**. Private profiles hide achievements from other platform members entirely, leaving only the friend request feature.
* **Showcase Registry:** Users can actively configure and highlight a maximum of 3 unlocked trophies to exhibit on their public-facing boards.


## Technical Architecture

* **Frontend:** Android XML Layouts, Material Design 3 Components, Custom HTML5/JavaScript Canvas Widgets inside Native WebViews.
* **Languages & Core APIs:** Java (JDK 17), Google Maps SDK for Android, Google Places API.
* **Backend Cloud Infrastructure:** Firebase Authentication, Cloud Firestore (Real-time streams), Google Services core router.
* **Security Subsystem:** Java Cryptography Architecture (JCA), `java.security` providers, Android Hardware KeyStore Provider.


## Security Implementation (E2EE RSA)

The application handles communication privacy using a multi-step asymmetric encryption schema:
1. **Key Pair Generation:** Upon initial signup, the device calls `EncryptionHelper` to generate a secure 2048-bit **RSA Key Pair**. The **Private Key** is injected directly into the phone's hardware-backed safe (`AndroidKeyStore`) and never leaves the device.
2. **Public Key Registry:** The corresponding **Public Key** is exported to Base64 and synced to the user's metadata block in Cloud Firestore.
3. **Double-Encryption Payload:** When sending a message inside `ChatActivity`, the app pulls the counterparty's public key. The text payload is encrypted twice:
   * `textForReceiver`: Encrypted with the recipient's public key (readable only by the recipient).
   * `textForSender`: Encrypted with the sender's own public key (so the sender can read their own history).
   * The raw text parameter is entirely skipped (`""`), and `isEncrypted` is flagged to `true`.
4. **Local Decryption:** Inside `ChatAdapter`, the local private key is pulled from the hardware seif to safely decode the corresponding encrypted stream in real-time.


## Testing Suite

Maps4u includes a robust testing package consisting of local unit tests (validating the application's core logic and mathematical formulas) and instrumented UI tests.

### Local Unit Tests (`app/src/test/java/`)
* **`EnvironmentalMathTest`:** Confirms the conversion mechanics of tracking step vectors into exact kilometer distances and metabolic caloric expense computations without drifting anomalies.
* **`SustainabilityLogicTest`:** Validates eco-computations, ensuring precise outputs for fuel volume conservation, currency savings metrics, and carbon sequestration tree ratios.
* **`ChatMessageModelTest`:** Verifies the cryptographic flags, structure configurations, and state transitions of meetup objects (e.g., state shifts from `PENDING` to `ACCEPTED`).
* **`BiometricDataTest`:** Validates object construction, safe fallbacks for missing parameters, and dynamic biometric updates.

### Instrumented UI Tests (`app/src/androidTest/java/`)
* **`UIValidationTest`:** Leverages **Espresso** on a running emulator/device to validate interface layout visibility conditions, active tab switches, and biometric edit constraints.


## Local Setup & Configuration

To compile and run this graduation thesis project locally, follow these configuration guidelines to ensure security files remain un-tracked by Git tracking branches.

### 1. Prerequisites
* Android Studio (Ladybug / Hedgehog or newer)
* Android SDK 34+
* Gradle Build System configured with your native Java environment

### 2. Secrets and Environment Files
Create a file named `secrets.xml` inside your local resource layout folder:  
`app/src/main/res/values/secrets.xml`
