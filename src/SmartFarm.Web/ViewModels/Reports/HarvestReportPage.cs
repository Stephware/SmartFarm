using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Reports;

namespace SmartFarm.Web.ViewModels.Reports;

public sealed record HarvestReportPage(
    HarvestReport Report, IReadOnlyList<PlotDetails> Plots,
    IReadOnlyList<CropTypeDetails> CropTypes);
