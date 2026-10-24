using SmartFarm.Core.Contracts.Weather;

namespace SmartFarm.Core.Interfaces.Weather;

// Stored farm weather overview shared by MVC and API, not a live external API call.
public interface IFarmWeatherService
{
    Task<WeatherOverview> GetAsync(Guid farmId, Guid actor, CancellationToken ct = default);
}
