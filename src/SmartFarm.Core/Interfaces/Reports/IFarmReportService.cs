using SmartFarm.Core.Contracts.Reports;

namespace SmartFarm.Core.Interfaces.Reports;

public interface IFarmReportService
{
    Task<FarmDashboard> DashboardAsync(Guid farmId, Guid actor, CancellationToken ct = default);
    Task<HarvestReport> HarvestsAsync(Guid farmId, Guid actor,
        DateOnly? from = null, DateOnly? to = null,
        Guid? plotId = null, Guid? cropTypeId = null, CancellationToken ct = default);
}
