# ScareVirusAPK - Prank Scareware

Chỉ dọa, **không** đánh cắp dữ liệu, **không** xóa file, **không** phá máy thật.

- Màn hình nhiễu + ảnh kinh dị + âm thanh loop
- Text giả "data stolen / ransomware"
- Nút chữa bằng chữ Ả Rập + Trung Quốc
- Tự hết sau khi bấm chữa

## Cách dùng

1. Clone repo
2. Tải ảnh kinh dị → đổi tên thành `scary_face.png` → bỏ vào `app/src/main/res/drawable/`
3. Tải âm thanh horror/glitch → đổi tên thành `scare_sound.mp3` → bỏ vào `app/src/main/res/raw/`
4. Mở bằng Android Studio → Sync → Run

Hoặc dùng GitHub Actions (Actions tab) để build APK debug (đã ký bằng debug keystore).

### Ảnh gợi ý (free)
- https://clipart-library.com/images/pT5dRngkc.jpg
- https://www.publicdomainpictures.net/pictures/370000/velka/image-1599417362z5n.jpg

### Âm thanh gợi ý (free)
- https://dailysounds.org/sounds/640
- https://orangefreesounds.com/wp-content/uploads/2024/07/Horror-scream-sound-effect.mp3

## Build local
```bash
./gradlew assembleDebug
```
APK nằm ở `app/build/outputs/apk/debug/app-debug.apk`
