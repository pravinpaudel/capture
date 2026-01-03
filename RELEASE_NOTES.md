# Capture - Release Notes

## Version 1.0-alpha
**Release Date**: December 26, 2025

### 🎉 What's New

**Capture** is an intelligent Android app that transforms how you add events to your calendar. Simply take a photo of any event flyer, poster, or ticket, and let AI extract the details automatically.

### ✨ Key Features

#### 📸 Smart Image Capture
- **Camera Integration** - Take photos directly within the app
- **Gallery Import** - Select existing images from your device
- **Professional UI** - Fullscreen immersive camera experience
- **Pinch to Zoom** - Precise focus on event details
- **Tap to Focus** - Quick autofocus for sharp text recognition

#### ✂️ Professional Crop Tool
- **Lens-Style Interface** - Modern, clean cropping experience
- **Precise Controls** - White border handles with touch-responsive guidelines

#### 🤖 AI-Powered Text Recognition
- **ML Kit Integration** - Google's industry-leading OCR technology
- **High Accuracy** - Reliable text extraction from images
- **Smart Pattern Detection** - Recognizes dates, times, and locations automatically

#### Intelligent Event Parsing
- **Date Recognition** - Multiple formats supported:
  - `December 25, 2025`, `Dec 25, 2025`
  - `25/12/2025`, `12/25/2025`
  - `25-12-2025`, `2025-12-25`
- **Time Detection** - 12-hour and 24-hour formats:
  - `10:30 AM`, `2:30 PM`
  - `14:30`, `10:30`
- **Location Extraction** - Automatic venue detection
- **Title & Description** - Comprehensive event information

#### 📝 Smart Event Form
- **Calendar Integration** - Direct save to device calendar

#### 🎨 Modern Design
- **Material Design 3** - Contemporary Android UI standards
- **Dark Theme** - Fullscreen black interface for camera operations
- **White Accent** - Clean, professional button styling
- **Immersive Experience** - No distracting UI elements during capture

### 🏗️ Technical Architecture

#### MVVM + Clean Architecture
- **Maintainable** - 50% code reduction in MainActivity through refactoring
- **Lifecycle-Aware** - Survives configuration changes (screen rotation)

#### Key Components
- **CameraViewModel** - State management and business logic coordination
- **CameraManager** - CameraX lifecycle and operations
- **PermissionHandler** - Runtime permission management
- **OcrProcessor** - ML Kit text recognition wrapper
- **EventParser** - Regex-based text extraction
- **DateTimeNormalizer** - Format conversion and validation
- **CalendarFormatter** - Calendar integration

### 📱 System Requirements

- **Android Version**: 7.0 (API 24) or higher
- **Permissions Required**:
  - Camera - For photo capture
  - Storage - For gallery access (Android 12 and below)
  - Read Media Images - For gallery access (Android 13+)
- **Hardware**: Camera (recommended, not required)
- **Storage**: ~15-20 MB

### 🔒 Privacy & Permissions

- **Camera Access** - Only used when you actively take photos
- **Gallery Access** - Only when you select images
- **No Internet Required** - All processing happens locally on your device
- **No Data Collection** - Your photos and events stay private
- **Local Processing** - OCR runs entirely on-device using ML Kit

### 🚀 Performance

- **Fast OCR** - Powered by Google ML Kit
- **Efficient Memory** - ViewModel prevents memory leaks
- **Optimized Images** - High-quality capture with minimal storage
- **Smooth UI** - 60fps camera preview
- **Quick Launch** - Instant camera startup

### 🎯 Use Cases

Perfect for capturing events from:
- 🎪 Concert tickets and flyers
- 🎓 School event posters
- 🏢 Conference schedules
- 🎉 Party invitations
- 🏃 Race registration details
- 🎭 Theater show information
- 📅 Meeting announcements
- 🎨 Art exhibition details

### 🐛 Known Limitations

- **Single Event Detection** - Currently processes one event per image
- **Text Clarity** - Best results with clear, high-contrast text
- **Language Support** - Optimized for English text
- **Manual Review** - Always review extracted data before saving

### 🔮 Future Enhancements

Planned for upcoming releases:
- Multi-event detection from single image
- Recurring event support
- Event sharing capabilities
- Custom reminder settings
- Offline event storage
- More date/time format support
- Additional language support


### 🙏 Acknowledgments

Built with:
- Google ML Kit for OCR
- CameraX for camera functionality
- Android Image Cropper for professional crop experience
- Material Design Components for modern UI

---

**Thank you for using Capture!** I hope this app makes managing your events easier and more convenient. Your feedback is valuable for future improvements.
