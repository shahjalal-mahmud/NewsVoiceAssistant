package com.appriyo.newsvoiceassistant.data.repository

class NewsRepository {

    private val headlinesQueue = mutableListOf<String>()
    private var currentIndex = 0

    init {
        // Sample headlines - can be replaced with API calls
        headlinesQueue.addAll(
            listOf(
                "Breaking News: Scientists discover new renewable energy source",
                "Sports Update: National team qualifies for world cup finals",
                "Technology: New smartphone breaks sales records worldwide",
                "Weather Alert: Heavy rainfall expected in coastal regions",
                "Business: Stock markets reach all-time high",
                "Health: Breakthrough in cancer treatment research",
                "Entertainment: Award-winning movie releases globally",
                "Politics: International summit addresses climate change"
            )
        )
    }

    fun getCurrentHeadline(): String? {
        return if (headlinesQueue.isNotEmpty() && currentIndex < headlinesQueue.size) {
            headlinesQueue[currentIndex]
        } else {
            null
        }
    }

    fun nextHeadline(): String? {
        if (headlinesQueue.isEmpty()) return null

        currentIndex = (currentIndex + 1) % headlinesQueue.size
        return getCurrentHeadline()
    }

    fun previousHeadline(): String? {
        if (headlinesQueue.isEmpty()) return null

        currentIndex = if (currentIndex == 0) headlinesQueue.size - 1 else currentIndex - 1
        return getCurrentHeadline()
    }

    fun addHeadlines(newHeadlines: List<String>) {
        headlinesQueue.addAll(newHeadlines)
    }

    fun clearQueue() {
        headlinesQueue.clear()
        currentIndex = 0
    }

    fun getQueueSize(): Int = headlinesQueue.size

    fun getCurrentIndex(): Int = currentIndex
}