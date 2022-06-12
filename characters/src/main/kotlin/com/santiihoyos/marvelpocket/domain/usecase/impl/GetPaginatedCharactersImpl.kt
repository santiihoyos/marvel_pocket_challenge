package com.santiihoyos.marvelpocket.domain.usecase.impl

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.santiihoyos.marvel.data.repository.CharacterRepository
import com.santiihoyos.marvelpocket.domain.entity.Character
import com.santiihoyos.marvelpocket.domain.extension.toCharacter
import com.santiihoyos.marvelpocket.domain.usecase.GetPaginatedCharacters
import com.santiihoyos.marvelpocket.ui.feature.characters.BuildConfig

class GetPaginatedCharactersImpl(
    override var itemsPerPage: Int = 20,
    private val characterRepository: CharacterRepository
) : GetPaginatedCharacters {

    override suspend fun getCharactersByPage(page: Int): Result<List<Character>> {
        val result = characterRepository.getCharactersByPage(
            limit = itemsPerPage,
            offset = page * itemsPerPage,
            orderBy = "name"
        )
        return if (result.isSuccess && result.getOrNull() != null) {
            Result.success(
                result.getOrNull()!!.map {
                    it.toCharacter()
                }.toList()
            )
        } else {
            Result.failure(Exception("Error on ${this::class.java.name}"))
        }
    }

    override fun getCharactersPagingSource(): PagingSource<Int, Character> {
        return CharacterPagingSource(this::getCharactersByPage)
    }
}

/**
 * Paging Source.
 *
 * DISCLAIMER: Google says that we should put it into data layer but our data layer is android
 * libraries agnostic so we keep it here to allow reuse without forcing api module
 * accomplishment to android specific implementations.
 */
private class CharacterPagingSource(
    private val onNextPage: suspend (page: Int) -> Result<List<Character>>
) : PagingSource<Int, Character>() {

    override fun getRefreshKey(state: PagingState<Int, Character>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Character> {
        val nextPage = params.key ?: 1
        val characters = onNextPage(nextPage)
        return if (characters.isFailure) {
            LoadResult.Error(
                characters.exceptionOrNull() ?: UnknownError()
            )
        } else {
            val newCharacters = characters.getOrNull()
            LoadResult.Page(
                data = newCharacters ?: emptyList(),
                prevKey = if (nextPage == 1) null else nextPage - 1,
                nextKey = if (newCharacters.isNullOrEmpty()) null else nextPage + 1
            )
        }
    }
}