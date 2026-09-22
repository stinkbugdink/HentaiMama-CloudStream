version = 1

android {
    namespace = "com.stinkbugNSFW.hentaimama"
}

cloudstream {
    description = "HentaiMama - Free Hentai Streaming with English Subtitles"
    authors = listOf("stinkbugdink")
    status = 1
    tvTypes = listOf("Anime", "AnimeMovie", "OVA")
}

dependencies {
    val implementation by configurations

    implementation(kotlin("stdlib"))
    implementation("com.github.Blatzar:NiceHttp:0.4.11")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("io.karn:khttp-android:0.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.mozilla:rhino:1.7.14")
}