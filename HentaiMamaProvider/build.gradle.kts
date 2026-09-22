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
    val compileOnly by configurations

    // Stubs for all CloudStream classes - provided by the app at runtime
    compileOnly("com.github.recloudstream.cloudstream:library:-SNAPSHOT")

    // these dependencies can include any of those which are added by the app,
    // but you dont need to include any of them if you dont need them
    implementation(kotlin("stdlib"))
    implementation("com.github.Blatzar:NiceHttp:0.4.11")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("io.karn:khttp-android:0.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.mozilla:rhino:1.7.14")
}