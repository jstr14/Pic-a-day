package com.jstr14.picaday.domain.usecase

import com.jstr14.picaday.data.repository.ImageRepository
import com.jstr14.picaday.domain.model.Album
import com.jstr14.picaday.domain.model.DayEntry
import com.jstr14.picaday.domain.repository.AlbumRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

/**
 * Merges personal entries with entries from all albums the user belongs to.
 * The resulting flow reacts live to changes in both personal data and album membership.
 */
class GetMergedEntriesUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
    private val albumRepository: AlbumRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun forMonth(month: YearMonth): Flow<List<DayEntry>> =
        albumRepository.getAlbumsForUser().flatMapLatest { albums ->
            val personalFlow = imageRepository.getEntriesForMonth(month)
            if (albums.isEmpty()) return@flatMapLatest personalFlow

            val albumFlows = albums.map { album ->
                albumRepository.getEntriesForMonth(album.id, month)
                    .map { entries -> tagWithAlbum(entries, album) }
            }
            buildMergedFlow(listOf(personalFlow) + albumFlows)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun forYear(year: Int): Flow<List<DayEntry>> =
        albumRepository.getAlbumsForUser().flatMapLatest { albums ->
            val personalFlow = imageRepository.getEntriesForYear(year)
            if (albums.isEmpty()) return@flatMapLatest personalFlow

            val albumFlows = albums.map { album ->
                albumRepository.getEntriesForYear(album.id, year)
                    .map { entries -> tagWithAlbum(entries, album) }
            }
            buildMergedFlow(listOf(personalFlow) + albumFlows)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildMergedFlow(flows: List<Flow<List<DayEntry>>>): Flow<List<DayEntry>> {
        if (flows.size == 1) return flows[0]
        @Suppress("UNCHECKED_CAST")
        val array = flows.toTypedArray() as Array<Flow<List<DayEntry>>>
        return combine(*array) { arrays: Array<List<DayEntry>> ->
            mergeByDate(arrays.flatMap { it })
        }
    }

    private fun tagWithAlbum(entries: List<DayEntry>, album: Album): List<DayEntry> =
        entries.map { entry ->
            entry.copy(photos = entry.photos.map { photo ->
                photo.copy(albumId = album.id, albumNames = listOf(album.name))
            })
        }

    private fun mergeByDate(entries: List<DayEntry>): List<DayEntry> =
        entries.groupBy { it.date }.map { (date, grouped) ->
            DayEntry(
                date = date,
                photos = grouped.flatMap { it.photos }
                    .groupBy { it.url }
                    .map { (_, dupes) ->
                        dupes.first().copy(
                            albumNames = dupes.flatMap { it.albumNames }.distinct()
                        )
                    }
                    .sortedBy { it.time },
                description = grouped.firstOrNull { it.description != null }?.description,
            )
        }
}
