# UnDetect-AI-Dectection-Remover-With-Grammer-Check-
Undetect is a local web application built in Java to sanitize AI-generated documents. Automatically removes hidden Unicode fingerprints, zero-width spaces, em-dashes, and telltale structural patterns.
Here is a complete, polished `README.md` rewritten specifically for **Undetect** based on your project details:


# Undetect

A lightweight, privacy-conscious text sanitizer and formatting cleaner available as a static web application, a local Java server, and an Android app. 

**Undetect** focuses purely on stripping hidden technical watermarks, invisible Unicode characters, and structural artifacts left behind by LLMs and automated tools. It operates entirely locally without rewriting your style, altering your core text, or sending your content to third-party AI detectors.

---

## What It Does

- **Invisible Watermark Removal:** Strips hidden zero-width and non-breaking Unicode characters (`U+200B–U+200D`, `U+FEFF`, `U+202F`, `U+00A0`, `U+00AD`) and replaces them with standard spaces.
- **Agent Trace Cleanup:** Automatically removes execution logs and task-phase headers (e.g., lines starting with `Phase [number]:` or `Execution Log:`).
- **Markdown Lead-in Normalization:** Strips bold formatting markers from colon-terminated lead-ins (e.g., converts `**Heading**:` to `Heading:`).
- **Whitespace Sanitization:** Normalizes repeated spaces and collapses excessive blank lines caused by pattern stripping.
- **Side-by-Side Comparison:** Displays original and sanitized text side-by-side with instant copy and download options.
- **Optional Grammar Assistance:** Integrates an opt-in check using the public LanguageTool API for grammar suggestions. *Note: Suggestions are never applied automatically, and enabling this feature sends your cleaned text to a third-party server.*

---

## Project Structure

```text
Undetect/
├── android/            # Android WebView container & build scripts
├── releases/           # Pre-compiled Android APK debug releases
├── src/                # Local Java HttpServer source code (`Main.java`)
└── website/            # Standalone static web application
    ├── docs/           # Help documentation and privacy guidelines
    └── index.html      # Primary web interface

```

---

## Web Application

Open [`website/index.html`](https://www.google.com/search?q=website/index.html) in any modern browser to run the standalone web interface. For detailed instructions and privacy notes, consult [`website/docs/index.html`](https://www.google.com/search?q=website/docs/index.html).

The static site performs all text cleaning locally in your browser. An active internet connection is only required if you choose to enable the optional grammar check (capped at 20,000 characters).

---

## Local Java Server

If you prefer running a local backend server in VS Code or via terminal, the native Java server is located in [`src/Main.java`](https://www.google.com/search?q=src/Main.java).

### Prerequisites

* Java 17 or higher

### Compilation & Execution

```sh
# Compile the source code
javac src/Main.java

# Run the local server
java -cp src Main

```

Once running, navigate to `http://localhost:8080` in your web browser.

---

## Android App

An installable debug APK is available at [`releases/Call-Text-Cleaner-Android-debug.apk`](https://www.google.com/search?q=releases/Call-Text-Cleaner-Android-debug.apk). It packages the web application into an offline-first Android WebView. Local text sanitization works entirely offline.

### Installation

1. Download the `.apk` file directly to your Android device.
2. Open the file to begin installation.
3. If prompted, allow your file manager or browser permission to install apps from external sources.

### Building from Source

**Requirements:**

* Java 17+
* Android SDK Platform 36 & Build Tools 36.0.0
* Environment variable `ANDROID_HOME` or `ANDROID_SDK_ROOT` configured correctly.

Run the build script from the root directory:

```sh
# Build the APK
sh android/build-apk.sh

# Build, install, and launch on a connected Android device
sh android/build-apk.sh --install

```

*Note: The build process limits Java heap usage to 4 GB. Build intermediates are generated inside `android/build/`.*

---

## Privacy Policy

**Undetect** is built around local execution. All regular text cleaning happens entirely on your machine or device. No text is saved, transmitted, or analyzed unless you explicitly opt in to the external grammar check feature.
