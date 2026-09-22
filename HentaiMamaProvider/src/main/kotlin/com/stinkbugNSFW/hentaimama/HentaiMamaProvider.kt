package com.stinkbugNSFW.hentaimama

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.nicehttp.NiceResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class HentaiMamaProvider : MainAPI() {
    override var mainUrl = "https://hentaimama.io"
    override var name = "HentaiMama"
    override var lang = "en"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)

    companion object {
        fun getTypeFromUrl(url: String, title: String, tags: List<String>): TvType {
            return when {
                url.contains("/movie-") || title.contains("Movie", true) || tags.any { it.contains("Movie", true) } -> TvType.AnimeMovie
                title.contains("OVA", true) || tags.any { it.contains("OVA", true) } -> TvType.OVA
                else -> TvType.Anime
            }
        }

        fun getStatus(status: String): ShowStatus {
            return when (status.lowercase()) {
                "ongoing", "airing", "releasing" -> ShowStatus.Ongoing
                "completed", "finished", "ended" -> ShowStatus.Completed
                else -> ShowStatus.Completed
            }
        }
    }

    private suspend fun request(url: String, ref: String? = null): NiceResponse {
        return app.get(
            url,
            headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Accept-Language" to "en-US,en;q=0.9"
            ),
            cookies = mapOf("_as_ipin_ct" to "ID"),
            referer = ref
        )
    }

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Latest Episodes",
        "$mainUrl/genre/hentai/page/" to "Hentai Series",
        "$mainUrl/genre/3d/page/" to "3D Content",
        "$mainUrl/genre/uncensored/page/" to "Uncensored",
        "$mainUrl/genre/english-subbed/page/" to "English Subbed",
        "$mainUrl/movie-terbaru/page/" to "Movies",
        "$mainUrl/genre/ova/page/" to "OVA Specials"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else request.data + page
        val document = this.request(url).document

        val items = document.select("article.post, div.post-item, div.result-item, .anime-item").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, items, hasNext = items.isNotEmpty())
    }

    private fun Element.toSearchResult(): AnimeSearchResponse? {
        val linkEl = this.selectFirst("a[href]") ?: return null
        val href = fixUrlNull(linkEl.attr("href")) ?: return null

        val titleEl = this.selectFirst(".title a, h2 a, h3 a, .post-title a, .entry-title a, a[href*='/anime/'], a[href*='/movie-']")
        val title = titleEl?.text()?.trim() ?: linkEl.attr("title") ?: linkEl.text() ?: return null

        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("src") ?:
            this.selectFirst("img")?.attr("data-src") ?:
            this.selectFirst("img")?.attr("data-lazy-src")
        )

        val type = getTypeFromUrl(href, title, listOf())
        val epNum = Regex("Episode\\s?(\\d+)").find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()

        return newAnimeSearchResponse(title, href, type) {
            this.posterUrl = posterUrl
            addSub(epNum)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val encodedQuery = query.replace(" ", "+")
        val document = request("$mainUrl/?s=$encodedQuery").document

        return document.select("article.post, div.post-item, div.result-item, .anime-item, .search-result").mapNotNull {
            it.toSearchResult()
        }
    }

    private fun getProperAnimeLink(url: String): String {
        return if (url.contains("/anime/") || url.contains("/movie-")) {
            url
        } else {
            var title = url.substringAfter("$mainUrl/")
            title = when {
                title.contains("-episode-") && !title.contains("-movie-") -> title.substringBefore("-episode-")
                title.contains("-movie-") -> title.substringBefore("-movie-")
                else -> title
            }
            "$mainUrl/anime/$title"
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = request(url).document

        val title = document.selectFirst("h1.entry-title, h1.post-title, .post-title h1, .entry-title")
            ?.text()
            ?.replace("Subtitle Indonesia", "")
            ?.replace("English Sub", "")
            ?.replace("Sub Indo", "")
            ?.trim()
            ?: throw ErrorLoadingException("No title found for $url")

        val poster = fixUrlNull(
            document.selectFirst("div.thumbnail img, .poster img, .anime-poster img, .entry-content img[src*='cover'], .entry-content img[src*='poster']")?.attr("src") ?:
            document.selectFirst("img.wp-post-image")?.attr("src")
        )

        val year = document.select("span.year, .year, .release-year, meta[property='og:release_date']")
            .firstOrNull()?.attr("content")?.substringBefore("-")?.toIntOrNull()
            ?: document.select("span.year, .year, .release-year").firstOrNull()?.text()?.toIntOrNull()

        val tags = document.select(".genres a, .tags a, .info a[href*='/genre/'], .categories a, .genre a")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val type = getTypeFromUrl(url, title, tags)

        val episodes = if (type == TvType.AnimeMovie) {
            listOf(
                newEpisode(url) {
                    this.name = title
                    this.episode = 1
                    this.season = 1
                }
            )
        } else {
            document.select("ul.episodes li, .episode-list li, .daftar li, .episodes-list li, #episode-list li").mapNotNull { ep ->
                val epLinkEl = ep.selectFirst("a[href]") ?: return@mapNotNull null
                val epLink = fixUrlNull(epLinkEl.attr("href")) ?: return@mapNotNull null
                val epTitle = epLinkEl.text().trim()
                val epNum = Regex("Episode\\s?(\\d+)").find(epTitle)?.groupValues?.getOrNull(1)?.toIntOrNull()

                newEpisode(epLink) {
                    this.name = epTitle.removePrefix("Episode $epNum: ").removePrefix("Episode $epNum ").trim()
                    this.episode = epNum
                    this.season = 1
                }
            }.reversed()
        }

        val plot = document.selectFirst(".synopsis, .description, .entry-content p, .anime-description, .plot-summary")?.text()

        val hasEnglishSub = document.text().contains("English Sub", true) ||
            document.text().contains("Subtitle English", true) ||
            document.select("track[srclang='en'], track[label*='English']").isNotEmpty()

        val status = document.select("span.status, .status, .anime-status, td:contains(Status) + td, .info:contains(Status) span")
            .firstOrNull()?.text()?.let { getStatus(it) }

        val malId = document.select("a[href*='myanimelist.net/anime/']").firstOrNull()?.attr("href")
            ?.substringAfter("/anime/")?.substringBefore("/")?.toIntOrNull()
        val aniListId = document.select("a[href*='anilist.co/anime/']").firstOrNull()?.attr("href")
            ?.substringAfter("/anime/")?.substringBefore("/")?.toIntOrNull()

        return if (type == TvType.Anime || type == TvType.OVA) {
            newAnimeLoadResponse(title, url, type) {
                this.posterUrl = poster
                this.year = year
                this.plot = plot
                this.tags = (this.tags ?: emptyList()) + tags
                this.showStatus = status
                addEpisodes(DubStatus.Subbed, episodes)
                if (hasEnglishSub) this.tags = (this.tags ?: emptyList()) + "English Sub"
                if (malId != null) this.syncData = (this.syncData ?: mutableMapOf()).apply { put("malId", malId.toString()) }
                if (aniListId != null) this.syncData = (this.syncData ?: mutableMapOf()).apply { put("aniListId", aniListId.toString()) }
            }
        } else {
            newMovieLoadResponse(title, url, type, episodes.first().data) {
                this.posterUrl = poster
                this.year = year
                this.plot = plot
                this.tags = (this.tags ?: emptyList()) + tags
                if (hasEnglishSub) this.tags = (this.tags ?: emptyList()) + "English Sub"
                if (malId != null) this.syncData = (this.syncData ?: mutableMapOf()).apply { put("malId", malId.toString()) }
                if (aniListId != null) this.syncData = (this.syncData ?: mutableMapOf()).apply { put("aniListId", aniListId.toString()) }
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = request(data).document

val tracks = document.select("track[kind=subtitles][srclang]")
        for (i in 0 until tracks.size) {
            val track = tracks[i]
            val src = track.attr("src") ?: continue
            val lang = track.attr("srclang") ?: "en"
            subtitleCallback.invoke(newSubtitleFile(lang.uppercase(), fixUrl(src)))
        }

        val videoSources = mutableListOf<String>()

        val videoSourcesList = document.select("video source[src]")
        for (i in 0 until videoSourcesList.size) {
            val source = videoSourcesList[i]
            val src = source.attr("src")
            if (src.isNotEmpty()) videoSources.add(src)
        }

        val iframes = document.select("iframe[src], iframe[data-src]")
        for (i in 0 until iframes.size) {
            val iframe = iframes[i]
            val src = iframe.attr("src").ifEmpty { iframe.attr("data-src") }
            if (src.isNotEmpty() && !src.contains("ads", true) && !src.contains("adsense", true)) {
                videoSources.add(src)
            }
        }

        val players = document.select(".player-iframe, .video-player, .embed-responsive iframe")
        for (i in 0 until players.size) {
            val player = players[i]
            val src = player.attr("src") ?: player.attr("data-src") ?: ""
            if (src.isNotEmpty() && !src.contains("ads", true)) {
                videoSources.add(src)
            }
        }

        val seen = mutableSetOf<String>()
        val filteredSources = videoSources.filter { it !in seen && seen.add(it) }
        for (sourceUrl in filteredSources) {
            safeApiCall {
                loadExtractor(fixUrl(sourceUrl), mainUrl, subtitleCallback) { link ->
                    val quality = getQualityFromName(link.name) ?: Qualities.Unknown.value
                    callback.invoke(ExtractorLink(
                        source = name,
                        name = link.name,
                        url = link.url,
                        referer = mainUrl,
                        quality = quality,
                        type = link.type,
                        headers = link.headers,
                        extractorData = link.extractorData
                    ))
                }
            }
        }

        return true
    }

    private fun getQualityFromName(name: String): Int? {
        return Regex("(\\d{3,4})[pP]").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: when {
                name.contains("4K", true) || name.contains("2160") -> 2160
                name.contains("1080") -> 1080
                name.contains("720") -> 720
                name.contains("480") -> 480
                name.contains("360") -> 360
                else -> null
            }
    }
}