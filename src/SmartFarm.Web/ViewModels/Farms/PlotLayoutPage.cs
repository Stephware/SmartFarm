using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Enums;

namespace SmartFarm.Web.ViewModels.Farms;

public sealed record PlotLayoutTile(Guid PlotId, string Name, decimal AreaSqMeters,
    string? CropName, CropCycleStatus? CropStatus);

public sealed record PlotLayoutPage(FarmDetails Farm, IReadOnlyList<PlotDetails> Plots,
    IReadOnlyList<CropCycleDetails> Cycles)
{
    public IReadOnlyList<PlotLayoutTile> Tiles => Plots.Select(p =>
    {
        var current = Cycles.Where(c => c.PlotId == p.Id &&
                c.Status is CropCycleStatus.Planned or CropCycleStatus.Active)
            .OrderByDescending(c => c.Status == CropCycleStatus.Active)
            .ThenByDescending(c => c.PlantedDate ?? c.PlannedPlantingDate)
            .FirstOrDefault();
        return new PlotLayoutTile(p.Id, p.Name, p.AreaSqMeters,
            current?.CropTypeName, current?.Status);
    }).ToList();
}
