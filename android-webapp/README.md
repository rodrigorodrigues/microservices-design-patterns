# Android Webapp

Android native application for the Spring Boot microservices platform, based on the `react-webapp` functionality.

## Features

- **Authentication**: JWT-based login/logout
- **Session Management**: Persistent user sessions with SharedPreferences
- **API Integration**: Retrofit-based REST API client
- **Material Design**: Modern UI with Material Components
- **Services Support**:
  - Companies management
  - Persons management
  - Products management
  - Users management

## Architecture

- **Language**: Java
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Architecture Pattern**: MVVM-ready structure
- **Networking**: Retrofit 2 + OkHttp
- **JSON Parsing**: Gson
- **UI**: Material Components for Android

## Project Structure

```
android-webapp/
├── app/
│   ├── src/main/
│   │   ├── java/com/springboot/android/
│   │   │   ├── api/              # Retrofit service interfaces
│   │   │   ├── model/            # Data models (POJOs)
│   │   │   ├── ui/               # Activities and UI components
│   │   │   ├── util/             # Utility classes (SessionManager)
│   │   │   └── MainApplication.java
│   │   ├── res/
│   │   │   ├── layout/           # XML layouts
│   │   │   ├── menu/             # Menu resources
│   │   │   ├── values/           # Strings, colors, themes
│   │   │   └── mipmap-*/         # App icons
│   │   └── AndroidManifest.xml
│   ├── build.gradle              # App module build config
│   └── proguard-rules.pro
├── build.gradle                  # Root build config
├── settings.gradle
└── gradle.properties
```

## API Services

### AuthService
- `POST /api/authenticate` - Login
- `GET /api/authenticate` - Get current user
- `POST /api/logout` - Logout

### CompanyService
- `GET /api/companies` - List companies (paginated)
- `GET /api/companies/{id}` - Get company details
- `POST /api/companies` - Create company
- `PUT /api/companies/{id}` - Update company
- `DELETE /api/companies/{id}` - Delete company

### PersonService
- `GET /api/persons` - List persons (paginated)
- `GET /api/persons/{id}` - Get person details
- `POST /api/persons` - Create person
- `PUT /api/persons/{id}` - Update person
- `DELETE /api/persons/{id}` - Delete person

### ProductService
- `GET /api/products` - List products (paginated)
- `GET /api/products/{id}` - Get product details
- `POST /api/products` - Create product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product

## Configuration

### Base URL

The base URL is configured in `app/build.gradle`:

- **Debug**: `http://10.0.2.2:8080/` (Android emulator localhost)
- **Release**: `http://localhost:8080/`

To change the backend URL, modify the `buildConfigField` in `app/build.gradle`:

```gradle
buildConfigField "String", "BASE_URL", "\"http://your-server:8080/\""
```

## Building and Running

### Prerequisites

- Android Studio Arctic Fox (2020.3.1) or later
- JDK 17 or later
- Android SDK 35
- Gradle 8.11.1

### Build from Android Studio

1. Open the `android-webapp` folder in Android Studio
2. Wait for Gradle sync to complete
3. Run the app on an emulator or physical device

### Build from Command Line

```bash
cd android-webapp

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

## Running with Backend

1. Start the Spring Boot backend services (see main project README)
2. Ensure the edge-server is running on port 8080
3. Launch the Android app
4. Login with valid credentials

### Using Android Emulator

The app is configured to use `10.0.2.2:8080` in debug mode, which maps to `localhost:8080` on your development machine.

### Using Physical Device

If testing on a physical device, ensure:
1. Device is on the same network as your development machine
2. Update the BASE_URL in `app/build.gradle` to use your machine's IP address

## Dependencies

Key dependencies (see `app/build.gradle` for complete list):

- AndroidX Core, AppCompat, Material Components
- Retrofit 2.11.0 + Gson converter
- OkHttp 4.12.0 logging interceptor
- Lifecycle components (LiveData, ViewModel)
- Navigation components
- JWT decoder

## Security

- JWT tokens stored in SharedPreferences
- Automatic token injection via OkHttp interceptor
- Session management with automatic logout
- Cleartext traffic allowed for development (disable in production)

## Testing

The project includes basic test setup for:
- Unit tests with JUnit
- Instrumented tests with Espresso

Run tests:
```bash
./gradlew test           # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

## Next Steps

To extend this application:

1. **Add ViewModels**: Implement MVVM architecture with LiveData
2. **Add RecyclerViews**: Implement list views for entities
3. **Add Navigation**: Implement fragment-based navigation
4. **Add Coroutines**: Convert Retrofit callbacks to coroutines
5. **Add Room Database**: Implement offline support
6. **Add WorkManager**: Implement background sync
7. **Add WebAuthn**: Implement passkey authentication

## License

Same as parent project
