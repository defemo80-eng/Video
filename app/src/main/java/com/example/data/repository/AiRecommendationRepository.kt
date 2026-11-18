package com.example.data.repository

import com.example.data.local.VideoEntity
import com.example.data.local.WatchHistoryEntity

data class AiRecommendationResult(
    val headline: String,
    val summary: String,
    val recommendedTags: List<String>,
    val rankedVideoIds: List<String>,
    val notice: String,
    val isPlaceholder: Boolean
)

interface AiRecommendationRepository {
    suspend fun generateRecommendations(
        history: List<WatchHistoryEntity>,
        availableVideos: List<VideoEntity>
    ): AiRecommendationResult
}

/**
 * Placeholder implementation used while Firebase AI Logic is not enabled.
 * Clearly labels sample AI recommendations and reminds the user that Gemini requires Firebase.
 */
class PlaceholderAiRecommendationRepository : AiRecommendationRepository {
    override suspend fun generateRecommendations(
        history: List<WatchHistoryEntity>,
        availableVideos: List<VideoEntity>
    ): AiRecommendationResult {
        val recentTitles = history.take(3).map { it.title }
        val preferredCategories = availableVideos
            .filter { v -> history.any { it.videoId == v.id } }
            .map { it.category }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

        val topTags = (preferredCategories + listOf("Tendencias", "Creadores", "Música", "Tecnología"))
            .distinct()
            .take(5)

        val rankedIds = availableVideos
            .sortedByDescending { video ->
                val categoryBoost = if (preferredCategories.contains(video.category)) 10_000L else 0L
                categoryBoost + video.viewsCount + (video.likesCount * 3L)
            }
            .map { it.id }

        val headline = if (recentTitles.isEmpty()) {
            "Sugerencias de muestra basadas en tendencias disponibles"
        } else {
            "Sugerencias de muestra basadas en tu historial reciente"
        }

        val summary = if (recentTitles.isEmpty()) {
            "Ejemplo de recomendación: explora categorías globales como ${topTags.joinToString(", ")}."
        } else {
            "Ejemplo de recomendación a partir de «${recentTitles.first()}»: priorizamos contenido afín a ${topTags.take(3).joinToString(", ")}."
        }

        return AiRecommendationResult(
            headline = headline,
            summary = summary,
            recommendedTags = topTags,
            rankedVideoIds = rankedIds,
            notice = "Aviso: Las recomendaciones generativas con Gemini requieren activar Firebase AI Logic en AI Studio. Mostrando sugerencias de ejemplo locales.",
            isPlaceholder = true
        )
    }
}
