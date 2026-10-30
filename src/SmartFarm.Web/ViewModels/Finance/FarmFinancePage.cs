using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Finance;

namespace SmartFarm.Web.ViewModels.Finance;

public sealed record FarmFinancePage(FarmDetails Farm, FarmFinanceOverview Finance,
    IReadOnlyList<CropCycleDetails> Crops);
