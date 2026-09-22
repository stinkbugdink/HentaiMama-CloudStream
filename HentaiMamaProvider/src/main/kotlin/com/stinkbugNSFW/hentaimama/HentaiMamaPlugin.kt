package com.stinkbugNSFW.hentaimama

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin
import android.content.Context

@CloudstreamPlugin
class HentaiMamaPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(HentaiMamaProvider())
    }
}