package com.medina.juanantonio.watcher.openai.sources

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.data.models.video.VideoGroup
import com.medina.juanantonio.watcher.sources.content.IContentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAIUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentRepository: IContentRepository,
    private val openAIRepository: IOpenAIRepository
) {

    suspend fun getSimilarContent(total: Int, title: String): VideoGroup? {
        val openAIResult = openAIRepository.getCompletion(
            context.getString(R.string.open_ai_similar_films_format, total, title)
        )

        return if (openAIResult != null && openAIResult.choices.isNotEmpty()) {
            val listVideo = arrayListOf<Video>()
            val firstChoiceCompletion = openAIResult.choices.first().text.trim()
            val completionSplitPerResult = firstChoiceCompletion.split("^")

            completionSplitPerResult.forEach { result ->
                try {
                    val splitResult = result.split("~") // MovieTitle~MovieYear
                    val searchResult = contentRepository.searchByKeywordSpecific(
                        splitResult[0],
                        splitResult[1]
                    )

                    searchResult?.let { listVideo.add(it) }
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }

            VideoGroup(
                category = context.getString(R.string.since_you_liked_videos),
                videoList = listVideo,
                contentType = VideoGroup.ContentType.VIDEOS
            )
        } else null
    }
}