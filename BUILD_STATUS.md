# CloudStream HentaiMama Provider - Build Status

## Current State: BLOCKED - Missing CloudStream Library Artifacts

### Project Configuration (Working)
- **Gradle Plugin**: `com.lagradost.cloudstream3.gradle` from JitPack (`com.github.recloudstream:gradle:-SNAPSHOT`)
- **Android Gradle Plugin**: 8.2.2
- **Kotlin**: 2.3.0 (required for CloudStream 4.8.0)
- **compileSdk**: 34, **minSdk**: 21, **targetSdk**: 34
- **Java**: 8 compatibility via `compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)`
- **Dependencies** (in root build.gradle.kts subprojects):
  - kotlin-stdlib
  - NiceHttp 0.4.11
  - Jsoup 1.17.2
  - Jackson Kotlin 2.16.1
  - khttp-android 0.1.2
  - Coroutines Android 1.8.0
  - Rhino 1.7.14

### The Problem
CloudStream library artifacts (`library-android`, `library-jvm`) are **not published on JitPack** for any version:
- `com.github.recloudstream.cloudstream:library:4.x` - only BOM jar published (empty)
- `com.github.recloudstream.cloudstream:library-android:4.x` - 404
- `com.github.recloudstream.cloudstream:library-jvm:4.x` - 404
- `com.github.lagradost:cloudstream:3.x` - requires JitPack auth (401)

### Errors Without Library
```kotlin
Unresolved reference: Plugin, CloudstreamPlugin, registerApi, MainAPI, 
TvType, ShowStatus, DubStatus, LoadResponse, newAnimeLoadResponse,
newMovieLoadResponse, addEpisodes, addSubtitleLanguage, addMalId,
addAniListId, ExtractorLink, loadExtractor, safeApiCall, Quality, etc.
```

### Recommended Next Step: Build CloudStream Locally
```bash
# 1. Clone CloudStream
git clone https://github.com/recloudstream/cloudstream.git
cd cloudstream

# 2. Build and install to local Maven
./gradlew :library:publishToMavenLocal

# 3. Add mavenLocal() to repositories in build.gradle.kts
repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

# 4. Use local artifact
implementation("com.github.recloudstream.cloudstream:library-android:4.8.0")
```

### Files in This Repo
- `build.gradle.kts` - Root config with plugin, Kotlin 2.3.0, compiler options
- `HentaiMamaProvider/build.gradle.kts` - Provider config (namespace, cloudstream extension)
- `HentaiMamaProvider/src/main/kotlin/com/stinkbugNSFW/hentaimama/HentaiMamaPlugin.kt` - Plugin entry
- `HentaiMamaProvider/src/main/kotlin/com/stinkbugNSFW/hentaimama/HentaiMamaProvider.kt` - Main provider logic

### Provider Features Implemented
- Main page with categories (Latest, Hentai Series, 3D, Uncensored, English Subbed, Movies, OVA)
- Search functionality
- Episode list parsing
- Video extraction with subtitle support
- MyAnimeList / AniList ID extraction
- Quality detection from stream names