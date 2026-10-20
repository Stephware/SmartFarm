using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Core.Interfaces.Reports;

namespace SmartFarm.Web.Clients;

public sealed class FarmReportApiService(SmartFarmApiClient api) : IFarmReportService
{
    public Task<FarmDashboard> DashboardAsync(Guid farmId, Guid actor,
        CancellationToken ct = default) =>
        api.ReadAsync<FarmDashboard>($"api/farms/{farmId}/dashboard", actor, ct);

    public Task<HarvestReport> HarvestsAsync(Guid farmId, Guid actor,
        DateOnly? from = null, DateOnly? to = null, Guid? plotId = null,
        Guid? cropTypeId = null, CancellationToken ct = default)
    {
        var query = new List<string>();
        if (from is not null) query.Add($"from={from:yyyy-MM-dd}");
        if (to is not null) query.Add($"to={to:yyyy-MM-dd}");
        if (plotId is not null) query.Add($"plotId={plotId}");
        if (cropTypeId is not null) query.Add($"cropTypeId={cropTypeId}");
        var path = $"api/farms/{farmId}/reports/harvests";
        if (query.Count > 0) path += "?" + string.Join("&", query);
        return api.ReadAsync<HarvestReport>(path, actor, ct);
    }
}
