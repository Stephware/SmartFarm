using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Harvests;

namespace SmartFarm.Web.ViewModels.Harvests;

public sealed record HarvestPage(CropCycleDetails CropCycle, HarvestOverview Overview);

