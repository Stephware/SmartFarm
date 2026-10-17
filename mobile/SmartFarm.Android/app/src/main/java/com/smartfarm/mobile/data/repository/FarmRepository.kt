package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.FarmApi
import com.smartfarm.mobile.data.remote.dto.*

class FarmRepository(
    private val authentication: AuthRepository,
    private val api: () -> FarmApi
) {
    suspend fun list(): List<FarmDto> = authentication.authorized { api().farms(it) }
    suspend fun create(input: FarmInputDto): FarmDto =
        authentication.authorized { api().createFarm(it, input) }

    suspend fun farm(farmId: String): FarmDto =
        authentication.authorized { api().farm(it, farmId) }
    suspend fun updateFarm(farmId: String, input: FarmInputDto): FarmDto =
        authentication.authorized { api().updateFarm(it, farmId, input) }
    suspend fun archiveFarm(farmId: String, input: FarmInputDto) =
        authentication.authorized { api().archiveFarm(it, farmId, input) }

    suspend fun plots(farmId: String): List<PlotDto> =
        authentication.authorized { api().plots(it, farmId) }
    suspend fun createPlot(farmId: String, input: PlotInputDto): PlotDto =
        authentication.authorized { api().createPlot(it, farmId, input) }
    suspend fun updatePlot(farmId: String, plotId: String, input: PlotInputDto): PlotDto =
        authentication.authorized { api().updatePlot(it, farmId, plotId, input) }
    suspend fun archivePlot(farmId: String, plotId: String, input: PlotInputDto) =
        authentication.authorized { api().archivePlot(it, farmId, plotId, input) }

    suspend fun members(farmId: String): List<FarmMemberDto> =
        authentication.authorized { api().members(it, farmId) }
    suspend fun invite(farmId: String, input: InviteMemberDto): InvitationDto =
        authentication.authorized { api().invite(it, farmId, input) }
    suspend fun accept(token: String): InvitationAcceptedDto =
        authentication.authorized { api().accept(it, AcceptInvitationDto(token)) }
    suspend fun changeRole(farmId: String, memberId: String, role: Int) =
        authentication.authorized { api().changeRole(it, farmId, memberId, UpdateMemberDto(role)) }
    suspend fun removeMember(farmId: String, memberId: String) =
        authentication.authorized { api().removeMember(it, farmId, memberId) }
}

