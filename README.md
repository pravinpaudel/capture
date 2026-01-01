# Snapture - Smart Event Calendar App

An intelligent Android app that captures event details from images using OCR (Optical Character Recognition) and automatically creates calendar events.

## Features

- 📸 **Camera Capture** - Take photos of event flyers, posters, or tickets
- 🖼️ **Gallery Import** - Select existing images from your device
- ✂️ **Image Cropping** - Crop images to focus on relevant event details
- 🤖 **OCR Processing** - Extract text using ML Kit Text Recognition
- 📅 **Smart Parsing** - Automatically detect dates, times, locations, and titles
- 📆 **Calendar Integration** - Add events directly to your device calendar

## Architecture

### MVVM + Clean Architecture

```
┌─────────────────────────────────────────────────┐
│                  VIEW LAYER                     │
│  • MainActivity                                  │
│  • EventFormActivity                             │
│  • CropActivity                                  │
└──────────────────┬──────────────────────────────┘
                   │ observes LiveData
                   ↓
┌─────────────────────────────────────────────────┐
│               VIEWMODEL LAYER                   │
│  • CameraViewModel                               │
│    - State management                            │
│    - Business logic coordination                 │
└──────────────────┬──────────────────────────────┘
                   │ uses
                   ↓
┌─────────────────────────────────────────────────┐
│                DATA/DOMAIN LAYER                │
│  • OcrProcessor - ML Kit integration            │
│  • EventParser - Text pattern matching          │
│  • DateTimeNormalizer - Format conversion       │
│  • CalendarFormatter - Calendar integration     │
└─────────────────────────────────────────────────┘
```

### Key Components

#### View Layer
- **MainActivity**  - Camera capture, gallery selection, UI logic
- **EventFormActivity** - Event details form and calendar integration
- **CropActivity** - Image cropping interface

#### ViewModel Layer
- **CameraViewModel** - Manages UI state, coordinates OCR processing
- **CameraViewModelFactory** - Dependency injection for ViewModel

#### Helper Classes
- **CameraManager** - CameraX lifecycle, image capture, pinch-to-zoom
- **PermissionHandler** - Runtime permissions (camera, storage)

#### Data Processing
- **OcrProcessor** - ML Kit Text Recognition API wrapper
- **EventParser** - Regex-based text extraction
- **DateTimeNormalizer** - Date/time format normalization
- **CalendarFormatter** - Calendar intent creation

#### Data Models
- **RawEventData** - Unparsed event strings
- **StructuredDateTime** - Normalized timestamp data

## Data Flow

### 1. Capture → Crop → OCR Flow

```
User captures/selects image
        ↓
Image cropping (CropActivity)
        ↓
ViewModel.processImageWithOCR(uri)
        ↓
OcrProcessor (ML Kit Text Recognition)
        ↓
EventParser (Regex pattern matching)
        ↓
RawEventData (title, date, time, location)
        ↓
MainActivity observes result
        ↓
Navigate to EventFormActivity
```

### 2. Event Form → Calendar Flow

```
EventFormActivity receives RawEventData
        ↓
DateTimeNormalizer.normalize(data)
        ↓
StructuredDateTime (timestamps)
        ↓
User edits/confirms details
        ↓
CalendarFormatter.createCalendarIntent()
        ↓
System calendar app opens
        ↓
Event saved to calendar
```

## Technology Stack

- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Architecture**: MVVM + Clean Architecture

### Libraries

- **ML Kit Text Recognition** - OCR processing
- **CameraX** (1.3.1) - Camera functionality
- **Android Image Cropper** (4.7.0) - Image cropping
- **Lifecycle ViewModel KTX** (2.8.7) - State management
- **Coroutines** - Asynchronous operations

## Design Patterns

1. **MVVM** (Model-View-ViewModel)
2. **Observer Pattern** (LiveData)
3. **Factory Pattern** (ViewModelFactory)
4. **Single Responsibility Principle** (Each class has one job)
5. **Dependency Injection** (Constructor injection)
6. **Repository Pattern** (OcrProcessor as data source)

## Benefits of Architecture

### Separation of Concerns
- MainActivity: Only UI operations
- ViewModel: State management and coordination
- Data layer: Processing and business logic

### Testability
- ViewModel can be unit tested without Android framework
- CameraManager, PermissionHandler are independently testable
- EventParser, DateTimeNormalizer have pure functions

### Maintainability
- 50% code reduction in MainActivity (481→237 lines)
- Each component has single responsibility
- Easy to locate and fix bugs

### Lifecycle Awareness
- ViewModel survives configuration changes (screen rotation)
- Automatic coroutine cancellation prevents memory leaks
- LiveData only updates active observers

## Project Structure

```
app/src/main/java/com/example/capture/
├── MainActivity.kt                 # Main camera interface
├── EventFormActivity.kt            # Event details form
├── CropActivity.kt                 # Image cropping
├── CameraViewModel.kt              # State management
├── CameraViewModelFactory.kt       # ViewModel factory
├── CameraManager.kt                # Camera operations
├── PermissionHandler.kt            # Runtime permissions
├── OcrProcessor.kt                 # ML Kit integration
├── EventParser.kt                  # Text pattern matching
├── DateTimeNormalizer.kt           # Date/time conversion
├── CalendarFormatter.kt            # Calendar integration
├── RawEventData.kt                 # Data model
└── StructuredDateTime.kt           # Data model
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android device/emulator with API 24+
- Camera permission
- Calendar app installed


### Permissions Required

- `CAMERA` - Capture photos
- `READ_MEDIA_IMAGES` (Android 13+) / `READ_EXTERNAL_STORAGE` - Gallery access

## Usage

1. **Launch App** - Camera preview opens
2. **Capture/Select** - Take photo or choose from gallery
3. **Crop Image** - Adjust crop area to focus on event details
4. **OCR Processing** - App extracts text automatically
5. **Review Details** - Edit extracted event information
6. **Add to Calendar** - Save to your device calendar


## Future Enhancements

- [ ] Multi-event detection
- [ ] Recurring events support
- [ ] Share event details
- [ ] Event reminders configuration
- [ ] Dark mode support
- [ ] Offline event storage

