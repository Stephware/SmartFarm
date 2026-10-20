package com.smartfarm.mobile.data.repository

import com.google.gson.reflect.TypeToken
import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.FarmApi
import com.smartfarm.mobile.data.remote.dto.*
import java.lang.reflect.Type

class FarmRepository(
    private val authentication: AuthRepository,
    private val cache: OfflineReadCache? = null,
    private val api: () -> FarmApi
) {
    private val farmListType: Type = TypeToken.getParameterized(List::class.java, FarmDto::class.java).type
    private val plotListType: Type = TypeToken.getParameterized(List::class.java, PlotDto::class.java).type
    private val memberListType: Type = TypeToken.getParameterized(List::class.java, FarmMemberDto::class.java).type

    private suspend fun <T> cached(key: String, type: Type, fetch: suspend () -> T): CachedRead<T> =
        cache?.read(key, type, fetch) ?: CachedRead(fetch(), false, System.currentTimeMillis())

    suspend fun listSnapshot(): CachedRead<List<FarmDto>> =
        cached("farms", farmListType) { authentication.authorized { api().farms(it) } }
    suspend fun list(): List<FarmDto> = listSnapshot().value

    suspend fun farmSnapshot(farmId: String): CachedRead<FarmDto> =
        cached("farm/$farmId", FarmDto::class.java) {
            authentication.authorized { api().farm(it, farmId) }
        }
    suspend fun farm(farmId: String): FarmDto = farmSnapshot(farmId).value

    suspend fun plotsSnapshot(farmId: String): CachedRead<List<PlotDto>> =
        cached("farm/$farmId/plots", plotListType) {
            authentication.authorized { api().plots(it, farmId) }
        }
    suspend fun plots(farmId: String): List<PlotDto> = plotsSnapshot(farmId).value

    suspend fun membersSnapshot(farmId: String): CachedRead<List<FarmMemberDto>> =
        cached("farm/$farmId/members", memberListType) {
            authentication.authorized { api().members(it, farmId) }
        }
    suspend fun members(farmId: String): List<FarmMemberDto> = membersSnapshot(farmId).value

    suspend fun create(input: FarmInputDto): FarmDto {
        val result = authentication.authorized { api().createFarm(it, input) }
        cache?.invalidate("farms")
        return result
    }

    suspend fun updateFarm(farmId: String, input: FarmInputDto): FarmDto {
        val result = authentication.authorized { api().updateFarm(it, farmId, input) }
        cache?.invalidate("farms")
        cache?.invalidate("farm/$farmId")
        return result
    }
    suspend fun archiveFarm(farmId: String, input: RowVersionDto) {
        authentication.authorized { api().archiveFarm(it, farmId, input) }
        cache?.invalidate("farms")
        cache?.invalidatePrefix("farm/$farmId")
    }

    suspend fun createPlot(farmId: String, input: PlotInputDto): PlotDto {
        val result = authentication.authorized { api().createPlot(it, farmId, input) }
        cache?.invalidate("farm/$farmId/plots")
        return result
    }
    suspend fun updatePlot(farmId: String, plotId: String, input: PlotInputDto): PlotDto {
        val result = authentication.authorized { api().updatePlot(it, farmId, plotId, input) }
        cache?.invalidate("farm/$farmId/plots")
        return result
    }
    suspend fun archivePlot(farmId: String, plotId: String, input: RowVersionDto) {
        authentication.authorized { api().archivePlot(it, farmId, plotId, input) }
        cache?.invalidate("farm/$farmId/plots")
    }

    suspend fun invite(farmId: String, input: InviteMemberDto): InvitationDto =
        authentication.authorized { api().invite(it, farmId, input) }
    suspend fun accept(token: String): InvitationAcceptedDto {
        val result = authentication.authorized { api().accept(it, AcceptInvitationDto(token)) }
        cache?.invalidate("farms")
        return result
    }
    suspend fun changeRole(farmId: String, memberId: String, role: Int) {
        authentication.authorized { api().changeRole(it, farmId, memberId, UpdateMemberDto(role)) }
        cache?.invalidate("farm/$farmId/members")
    }
    suspend fun removeMember(farmId: String, memberId: String) {
        authentication.authorized { api().removeMember(it, farmId, memberId) }
        cache?.invalidate("farm/$farmId/members")
    }
}
