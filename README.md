# Java-Archive
> Note Rhythm game


## Teaser

https://github.com/user-attachments/assets/1cdcdbaa-0cbf-4b0b-9954-a52e161265ab




## Updates

- Can choose difficulties
- Easy, Normal, Hard and Extreme modes
- 4K and 6K key modes
- Gradle build
- Windows executable distribution


## Packages

- app
- asset
- audio
- core
- editor
- stage
- state
- util

## How to play

1. Open [Releases](https://github.com/peofksk/Java-Archive/releases)
2. Download `Java-Archive-1.0.0-windows.zip`
3. Unzip the downloaded file
4. Run `Java-Archive.exe`

- Java installation is not required
- Do not move `Java-Archive.exe` out of the unzipped folder

## How to run from source

### Requirements

- JDK 17 or later
- Git

### Run

1. Clone this repository

```bash
git clone https://github.com/peofksk/Java-Archive.git
cd Java-Archive
```

2. Switch to the Gradle branch

```bash
git switch gradle
```

3. Run with Gradle Wrapper

```bash
./gradlew run
```

Windows Command Prompt:

```bat
gradlew.bat run
```

- Gradle installation is not required

## How to build Windows distribution

### Requirements

- Windows
- JDK 17 or later
- `jpackage`

1. Build the application

```bash
./gradlew clean installDist
```

2. Create the Windows application

```bash
jpackage \
  --type app-image \
  --dest build/package \
  --name Java-Archive \
  --app-version 1.0.0 \
  --input build/install/Java-Archive/lib \
  --main-jar Java-Archive-1.0.0.jar \
  --main-class app.Main \
  --add-modules java.desktop \
  --vendor peofksk \
  --description "Java Swing rhythm game"
```

3. Run the executable

```bash
./build/package/Java-Archive/Java-Archive.exe
```

4. Create the distribution ZIP

```bash
mkdir -p build/distributions

powershell.exe -NoProfile -Command \
  "Compress-Archive -Force -Path '$(cygpath -w "build/package/Java-Archive")' -DestinationPath '$(cygpath -w "build/distributions/Java-Archive-1.0.0-windows.zip")'"
```

5. Upload this file to GitHub Releases

```text
build/distributions/Java-Archive-1.0.0-windows.zip
```

- Select the `gradle` branch when creating the release tag
- `Source code (zip)` is not the executable distribution
