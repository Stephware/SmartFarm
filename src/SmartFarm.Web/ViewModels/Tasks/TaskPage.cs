using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Tasks;

namespace SmartFarm.Web.ViewModels.Tasks;

public sealed record TaskPage(
    FarmDetails Farm,
    IReadOnlyList<FarmTaskDetails> Tasks,
    IReadOnlyList<FarmMemberDetails> Members,
    IReadOnlyList<CropCycleDetails> Cycles);
