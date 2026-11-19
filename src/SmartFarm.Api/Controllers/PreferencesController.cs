using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

public sealed class LanguageInput
{
    [Required, RegularExpression("^(en|fil)$")] public string Language { get; set; } = "en";
}

[ApiController, Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer), Route("api/auth/preferences")]
public sealed class PreferencesController(UserManager<SmartFarmUser> users, UserPreferencesService preferences) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> Get(CancellationToken ct) => Result(async () => Ok(new { language = await preferences.LanguageAsync(ActorId, ct) }));
    [HttpPut]
    public Task<IActionResult> Put(LanguageInput input, CancellationToken ct) => Result(async () =>
    {
        await preferences.SetLanguageAsync(ActorId, input.Language, ct);
        return Ok(new { language = input.Language });
    });
}
