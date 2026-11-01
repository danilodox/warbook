package io.github.danilodox.warbook.data.remote


import io.github.danilodox.warbook.data.remote.model.ClanDetailsDto
import io.github.danilodox.warbook.data.remote.model.ClanDto
import io.github.danilodox.warbook.data.remote.model.PlayerDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("clans/{clanTag}")
    suspend fun getClanDetails(@Path("clanTag") clanTag: String): ClanDetailsDto

    @GET("players/{playerTag}")
    suspend fun getPlayerDetails(
        @Path("playerTag") playerTag: String,
    ): PlayerDto
}