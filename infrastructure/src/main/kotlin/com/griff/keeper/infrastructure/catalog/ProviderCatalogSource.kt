package com.griff.keeper.infrastructure.catalog

import com.griff.keeper.domain.model.ManagementUrl
import com.griff.keeper.domain.model.Provider
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.ProviderId

/**
 * Offline catalog of popular services.
 *
 * Kept as data (not UI code) so the list can later be replaced by a remote or database backed
 * source. Display names are brand names and therefore not translated; the "Other" entry is
 * localized by the presentation layer.
 */
internal object ProviderCatalogSource {

    val providers: List<Provider> = buildList {
        // Video
        add(provider("netflix", "Netflix", ProviderCategory.VIDEO, "https://www.netflix.com/account"))
        add(provider("hbo_max", "HBO Max", ProviderCategory.VIDEO, "https://www.hbomax.com/settings/subscription"))
        add(provider("prime_video", "Prime Video", ProviderCategory.VIDEO, "https://www.primevideo.com/settings"))
        add(provider("disney_plus", "Disney+", ProviderCategory.VIDEO, "https://www.disneyplus.com/account/subscription"))
        add(provider("apple_tv_plus", "Apple TV+", ProviderCategory.VIDEO, "https://apps.apple.com/account/subscriptions"))
        add(provider("canal_plus_online", "CANAL+ online", ProviderCategory.VIDEO, "https://pl.canalplus.com/moje-konto"))
        add(provider("player", "Player", ProviderCategory.VIDEO, "https://player.pl/moje-konto"))
        add(provider("polsat_box_go", "Polsat Box Go", ProviderCategory.VIDEO, "https://polsatboxgo.pl/moje-konto"))
        add(provider("skyshowtime", "SkyShowtime", ProviderCategory.VIDEO, "https://www.skyshowtime.com/account"))
        add(provider("cda_premium", "CDA Premium", ProviderCategory.VIDEO, "https://www.cda.pl/konto"))
        add(provider("sweet_tv", "Sweet.tv", ProviderCategory.VIDEO, "https://sweet.tv/account"))

        // Music and audio
        add(provider("spotify", "Spotify", ProviderCategory.MUSIC, "https://www.spotify.com/account/subscription"))
        add(provider("youtube_premium", "YouTube Premium", ProviderCategory.MUSIC, "https://www.youtube.com/paid_memberships"))
        add(provider("youtube_music", "YouTube Music", ProviderCategory.MUSIC, "https://www.youtube.com/paid_memberships"))
        add(provider("apple_music", "Apple Music", ProviderCategory.MUSIC, "https://apps.apple.com/account/subscriptions"))
        add(provider("tidal", "Tidal", ProviderCategory.MUSIC, "https://account.tidal.com/subscription"))

        // Books and audiobooks
        add(provider("storytel", "Storytel", ProviderCategory.BOOKS, "https://www.storytel.com/pl/my-account"))
        add(provider("bookbeat", "BookBeat", ProviderCategory.BOOKS, "https://www.bookbeat.com/pl/account"))
        add(provider("audioteka_klub", "Audioteka Klub", ProviderCategory.BOOKS, "https://audioteka.com/pl/account"))
        add(provider("legimi", "Legimi", ProviderCategory.BOOKS, "https://www.legimi.pl/panel/"))
        add(provider("empik_go", "Empik Go", ProviderCategory.BOOKS, "https://www.empik.com/konto"))

        // AI
        add(provider("chatgpt", "ChatGPT", ProviderCategory.AI, "https://chatgpt.com/#settings/Subscription"))
        add(provider("claude", "Claude", ProviderCategory.AI, "https://claude.ai/settings/billing"))
        add(provider("google_gemini", "Google AI / Gemini", ProviderCategory.AI, "https://one.google.com/plans"))
        add(provider("perplexity_pro", "Perplexity Pro", ProviderCategory.AI, "https://www.perplexity.ai/settings/account"))
        add(provider("github_copilot", "GitHub Copilot", ProviderCategory.AI, "https://github.com/settings/copilot"))

        // Cloud and productivity
        add(provider("google_workspace", "Google Workspace", ProviderCategory.CLOUD, "https://admin.google.com/ac/billing"))
        add(provider("google_one", "Google One", ProviderCategory.CLOUD, "https://one.google.com/settings"))
        add(provider("microsoft_365", "Microsoft 365", ProviderCategory.CLOUD, "https://account.microsoft.com/services"))
        add(provider("icloud_plus", "iCloud+", ProviderCategory.CLOUD, "https://www.icloud.com/settings/"))
        add(provider("dropbox", "Dropbox", ProviderCategory.CLOUD, "https://www.dropbox.com/account/plan"))

        // Software
        add(provider("adobe_creative_cloud", "Adobe Creative Cloud", ProviderCategory.SOFTWARE, "https://account.adobe.com/plans"))
        add(provider("canva_pro", "Canva Pro", ProviderCategory.SOFTWARE, "https://www.canva.com/settings/billing-and-plans"))
        add(provider("jetbrains", "JetBrains", ProviderCategory.SOFTWARE, "https://account.jetbrains.com/licenses"))

        // Hosting and domains
        add(provider("seohost", "SeoHost.pl", ProviderCategory.HOSTING, "https://panel.seohost.pl"))
        add(provider("home_pl", "home.pl", ProviderCategory.HOSTING, "https://panel.home.pl"))
        add(provider("cyber_folks", "cyber_Folks", ProviderCategory.HOSTING, "https://panel.cyberfolks.pl"))
        add(provider("nazwa_pl", "nazwa.pl", ProviderCategory.HOSTING, "https://panel.nazwa.pl"))
        add(provider("ovhcloud", "OVHcloud", ProviderCategory.HOSTING, "https://www.ovh.com/manager/"))
        add(provider("lh_pl", "LH.pl", ProviderCategory.HOSTING, "https://panel.lh.pl"))
        add(provider("dhosting", "dhosting.pl", ProviderCategory.HOSTING, "https://panel.dhosting.pl"))
        add(provider("hostido", "Hostido", ProviderCategory.HOSTING, "https://panel.hostido.pl"))
        add(provider("zenbox", "Zenbox", ProviderCategory.HOSTING, "https://panel.zenbox.pl"))
        add(provider("aftermarket", "AfterMarket.pl", ProviderCategory.HOSTING, "https://www.aftermarket.pl/panel/"))

        // Shopping and delivery
        add(provider("amazon_prime", "Amazon Prime", ProviderCategory.SHOPPING, "https://www.amazon.pl/gp/primecentral"))
        add(provider("allegro_smart", "Allegro Smart!", ProviderCategory.SHOPPING, "https://allegro.pl/moje-allegro/smart"))
        add(provider("wolt_plus", "Wolt+", ProviderCategory.SHOPPING, "https://wolt.com/pl/me/subscription"))
        add(provider("glovo_prime", "Glovo Prime", ProviderCategory.SHOPPING, "https://glovoapp.com/pl/"))

        // Gaming
        add(provider("xbox_game_pass", "Xbox Game Pass", ProviderCategory.GAMING, "https://account.microsoft.com/services"))
        add(provider("playstation_plus", "PlayStation Plus", ProviderCategory.GAMING, "https://www.playstation.com/acct/subscription"))
        add(provider("nintendo_switch_online", "Nintendo Switch Online", ProviderCategory.GAMING, "https://accounts.nintendo.com/"))
        add(provider("ea_play", "EA Play", ProviderCategory.GAMING, "https://www.ea.com/ea-play"))

        // Always last: catch-all entry for services outside of the catalog.
        add(
            Provider(
                id = ProviderId.OTHER,
                displayName = "Other",
                category = ProviderCategory.OTHER,
                logoKey = ProviderId.OTHER.value,
                defaultManagementUrl = null,
            ),
        )
    }

    private fun provider(
        id: String,
        displayName: String,
        category: ProviderCategory,
        managementUrl: String?,
    ) = Provider(
        id = ProviderId(id),
        displayName = displayName,
        category = category,
        logoKey = id,
        defaultManagementUrl = managementUrl?.let(ManagementUrl::ofOrNull),
    )
}
