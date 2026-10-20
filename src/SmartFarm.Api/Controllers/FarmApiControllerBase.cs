using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

public abstract class FarmApiControllerBase(UserManager<SmartFarmUser> users) : ControllerBase
{
    protected Guid ActorId =>
        Guid.TryParse(users.GetUserId(User), out var id) ? id : Guid.Empty;

    protected async Task<IActionResult> Result(Func<Task<IActionResult>> work)
    {
        try { return await work(); }
        catch (FarmOperationException problem)
        {
            return problem.Error switch
            {
                FarmError.NotFound => NotFound(new { message = "Resource not found." }),
                FarmError.Conflict => Conflict(new { message = problem.Message }),
                _ => BadRequest(new { message = problem.Message })
            };
        }
    }
}
