package com.medina.juanantonio.watcher.sources.content

import android.app.SearchManager.*
import android.database.Cursor
import android.database.MatrixCursor
import android.provider.BaseColumns
import com.medina.juanantonio.watcher.sources.media.IMediaRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchProviderUseCase @Inject constructor(
    private var contentRepository: IContentRepository,
    private var mediaRepository: IMediaRepository
) {

    suspend fun getSearchResults(query: String, limit: Int): Cursor {
        val searchResults = contentRepository.searchByKeyword(query)
        val resultsFromKeyword = searchResults?.firstOrNull()?.videoList ?: emptyList()
        val movieResults = resultsFromKeyword.filter { it.isMovie }.take(limit)
        val matrixCursor = MatrixCursor(
            arrayOf(
                BaseColumns._ID,
                SUGGEST_COLUMN_TEXT_1,
                SUGGEST_COLUMN_PRODUCTION_YEAR,
                SUGGEST_COLUMN_DURATION,
                SUGGEST_COLUMN_RESULT_CARD_IMAGE,
                SUGGEST_COLUMN_INTENT_DATA_ID
            )
        )

        movieResults.forEach { video ->
            val videoMedia = mediaRepository.getVideo(
                id = video.contentId,
                category = video.category ?: -1,
                episodeNumber = video.episodeNumber
            ) ?: return@forEach

            matrixCursor.newRow()
                .add(BaseColumns._ID, video.contentId)
                .add(SUGGEST_COLUMN_TEXT_1, video.title)
                .add(SUGGEST_COLUMN_PRODUCTION_YEAR, video.year)
                .add(
                    SUGGEST_COLUMN_DURATION,
                    TimeUnit.MINUTES.toMillis(videoMedia.totalDuration.toLong())
                )
                .add(SUGGEST_COLUMN_RESULT_CARD_IMAGE, videoMedia.coverHorizontalUrl)
                .add(SUGGEST_COLUMN_INTENT_DATA_ID, video.contentId)
        }

        return matrixCursor
    }
}