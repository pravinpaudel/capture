# Plan: OCR Integration for Event Poster Parsing
Integrate OCR (Optical Character Recognition) to automatically extract event details (title, date, time, location, description) from captured pamphlet photos and intelligently populate the event form fields using ML Kit Text Recognition.
Steps
- Add ML Kit Text Recognition dependency to app/build.gradle.kts - Google's ML Kit is free, on-device, works offline, and optimized for Android with no API keys needed
- Create OcrProcessor.kt class to handle text recognition - extract raw text from image URI, return structured data with confidence scores, handle errors gracefully
- Create EventParser.kt class with smart parsing logic - use regex patterns to identify dates (MM/DD/YYYY, Month DD), times (12h/24h format), location keywords (at, venue, @), extract title from largest/bold text at top, and description from remaining content
- Integrate OCR into MainActivity.kt - after photo capture, process image with OcrProcessor, pass extracted event data to EventFormActivity via Intent extras alongside photo URI
- Update EventFormActivity.kt - receive parsed event data from Intent, pre-populate form fields with extracted information, allow manual editing of all fields since OCR may have errors
- Add loading indicator to activity_main.xml - show Processing image... progress dialog during OCR to inform user the app is working


## Further Considerations
OCR accuracy challenges - Posters have varying fonts, colors, backgrounds; should we add a "Re-scan" button if extraction fails or add manual text correction UI?
Alternative OCR options - ML Kit (recommended for beginners), Tesseract (more powerful but complex setup), or Google Cloud Vision API (requires internet + billing)?
Performance optimization - Should we compress images before OCR processing, run OCR on background thread with coroutines, or cache OCR results to avoid re-processing?


Steps
Add ML Kit Text Recognition v2 dependency to app/build.gradle.kts - use play-services-mlkit-text-recognition (bundled with app, ~10MB) instead of unbundled version for offline reliability
Create OcrProcessor.kt class with coroutines support - use TextRecognition.getClient() to process image URI, return Result<String> wrapper for success/failure handling, run recognition on IO dispatcher to avoid blocking UI thread
Create EventParser.kt utility class with smart heuristics - use regex for dates (Jan|Feb|..., MM/DD/YYYY, DD-MM-YYYY), times (10:00 AM, 18:00, 10 AM - 2 PM), location keywords ("at ", "venue:", "@", addresses with numbers), title extraction (first 1-3 lines with largest font/all-caps pattern), remaining text as description
Update MainActivity.kt camera callback - after successful photo capture, show progress dialog, process image with OcrProcessor using lifecycleScope.launch, pass both photo URI and extracted text to EventFormActivity via Intent extras
Update EventFormActivity.kt onCreate() - receive "extracted_title", "extracted_date", "extracted_time", "extracted_location", "extracted_description" from Intent extras, pre-populate corresponding form fields with setText(), keep all fields editable since OCR may have errors
Add loading UI to activity_main.xml - include ProgressBar (initially visibility="gone"), show/hide programmatically during OCR processing with "Extracting event details..." message


**Further Considerations**
Image preprocessing recommended - compress images to max 1024x1024 pixels before OCR using BitmapFactory.decodeStream() with inSampleSize, this reduces processing time from 3-5s to 1-2s without losing text accuracy
Error handling strategy - if OCR fails or returns empty text, should we pre-populate form with blank fields and show Toast "Could not extract text, please enter manually", or retry OCR once automatically?
Caching strategy unnecessary - since users typically scan once per poster and storage costs outweigh benefits, skip caching and always process fresh images