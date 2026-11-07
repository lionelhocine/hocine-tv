package com.tv.streamapp.data.parser

import com.tv.streamapp.data.model.CategoryGroup
import com.tv.streamapp.data.model.ChannelRaw
import com.tv.streamapp.data.model.PlaylistResponse

object M3uParser {

    fun isM3u(content: String): Boolean {
        val t = content.trimStart()
        return t.startsWith("#EXTM3U") || t.contains("#EXTINF")
    }

    fun parse(content: String): PlaylistResponse {
        val groups = LinkedHashMap<String, MutableList<ChannelRaw>>()
        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null
        var counter = 0

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF")) {
                pendingName = line.substringAfterLast(",", "").trim()
                pendingLogo = attribute(line, "tvg-logo")
                pendingGroup = attribute(line, "group-title")
            } else if (line.startsWith("#")) {
                continue
            } else {
                counter++
                val name = pendingName?.takeIf { it.isNotBlank() } ?: "Channel $counter"
                val group = pendingGroup?.takeIf { it.isNotBlank() } ?: "Other"
                groups.getOrPut(group) { mutableListOf() }
                    .add(ChannelRaw(name = name, url = line, logo = pendingLogo?.takeIf { it.isNotBlank() }))
                pendingName = null
                pendingLogo = null
                pendingGroup = null
            }
        }

        return PlaylistResponse(
            categories = groups.map { (name, list) -> CategoryGroup(name = name, channels = list) }
        )
    }

    private fun attribute(line: String, key: String): String? {
        val regex = Regex(key + "=\"([^\"]*)\"", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.getOrNull(1)
    }
}
