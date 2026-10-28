using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Farms;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class FarmsController : Controller
{
    private readonly UserManager<SmartFarmUser> _users;
    private readonly IFarmManagementService _farms;
    private readonly IFarmEnrollmentService _enrollment;

    public FarmsController(
        UserManager<SmartFarmUser> users,
        IFarmManagementService farms, IFarmEnrollmentService enrollment)
    {
        _users = users;
        _farms = farms;
        _enrollment = enrollment;
    }

    private Guid Actor =>
        Guid.TryParse(_users.GetUserId(User), out var id) ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(CancellationToken ct) =>
        View(await IndexModel(new(), ct));

    private async Task<FarmIndexViewModel> IndexModel(FarmJoinRequestInput input, CancellationToken ct)
    {
        var farms = await _farms.ListAsync(Actor, ct);
        try { return new(farms, await _enrollment.MyRequestsAsync(Actor, ct), input); }
        catch (Exception error) when (error is System.Data.Common.DbException or HttpRequestException)
        {
            HttpContext.RequestServices.GetRequiredService<ILogger<FarmsController>>().LogError(error, "Could not load join requests");
            return new(farms, [], input, "Join request status is unavailable. Refresh this page to try again.");
        }
    }

    [HttpPost, ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.Enrollment)]
    public async Task<IActionResult> RequestJoin(FarmJoinRequestInput input, CancellationToken ct)
    {
        if (ModelState.IsValid)
        {
            try
            {
                await _enrollment.RequestAsync(Actor, input.FarmCode, ct);
                TempData["Message"] = "Request submitted. Farm access begins after the owner approves.";
                return RedirectToAction(nameof(Index));
            }
            catch (FarmOperationException error) { ModelState.AddModelError(nameof(input.FarmCode), error.Message); }
        }
        return View(nameof(Index), await IndexModel(input, ct));
    }

    [HttpPost, ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.Enrollment)]
    public async Task<IActionResult> CancelJoin(Guid farmId, Guid requestId, CancellationToken ct)
    {
        try
        {
            await _enrollment.CancelAsync(farmId, requestId, Actor, ct);
            TempData["Message"] = "Join request canceled. You can apply again with a valid farm code.";
        }
        catch (FarmOperationException error)
        {
            if (error.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = error.Message;
        }
        return RedirectToAction(nameof(Index));
    }

    [HttpPost, ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.Enrollment)]
    public async Task<IActionResult> RegenerateCode(Guid id, FarmCodeRegenerationInput input,
        bool dashboard, CancellationToken ct)
    {
        if (!ModelState.IsValid) return BadRequest();
        try
        {
            await _enrollment.RegenerateCodeAsync(id, Actor, input.CurrentCode, ct);
            TempData["Message"] = "Farm code regenerated. Share the new code with farmers requesting access.";
        }
        catch (FarmOperationException error)
        {
            if (error.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = error.Message;
            if (error.Error == FarmError.Conflict)
            {
                var result = await Details(id, ct);
                if (result is ViewResult view) view.ViewName = nameof(Details);
                Response.StatusCode = StatusCodes.Status409Conflict;
                return result;
            }
        }
        return dashboard ? RedirectToAction("Index", "Dashboard", new { farmId = id })
            : RedirectToAction(nameof(Details), new { id });
    }

    [HttpPost, ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.Enrollment)]
    public async Task<IActionResult> DecideJoin(Guid id, Guid requestId, bool? approve,
        bool dashboard, CancellationToken ct)
    {
        if (!ModelState.IsValid || approve is null) return BadRequest();
        try
        {
            await _enrollment.DecideAsync(id, requestId, Actor, approve.Value, ct);
            TempData["Message"] = approve.Value ? "Join request approved." : "Join request rejected.";
        }
        catch (FarmOperationException error)
        {
            if (error.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = error.Message;
        }
        return dashboard ? RedirectToAction("Index", "Dashboard", new { farmId = id })
            : RedirectToAction(nameof(Details), new { id });
    }

    [HttpGet]
    public IActionResult Create() => View(new FarmInput());

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(FarmInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid) return View(input);
        try
        {
            var farm = await _farms.CreateAsync(Actor, input, ct);
            return RedirectToAction(nameof(Details), new { id = farm.Id });
        }
        catch (FarmOperationException e)
        {
            ModelState.AddModelError("", e.Message);
            return View(input);
        }
    }

    [HttpGet]
    public async Task<IActionResult> Details(Guid id, CancellationToken ct)
    {
        var farm = await _farms.GetAsync(id, Actor, ct);
        if (farm is null) return NotFound();
        FarmEnrollmentPanel? enrollment = null;
        if (farm.Role == "Owner")
        {
            try { enrollment = new(id, await _enrollment.OwnerOverviewAsync(id, Actor, ct)); }
            catch (Exception error) when (error is System.Data.Common.DbException or HttpRequestException or FarmOperationException)
            {
                HttpContext.RequestServices.GetRequiredService<ILogger<FarmsController>>().LogError(error, "Could not load owner join requests");
                enrollment = new(id, null, Error: "Join requests are unavailable. Refresh this page to try again.");
            }
        }
        return View(new FarmWorkspaceViewModel(farm,
            await _farms.ListPlotsAsync(id, Actor, ct), await _farms.MembersAsync(id, Actor, ct), enrollment));
    }

    [HttpGet]
    public async Task<IActionResult> Edit(Guid id, CancellationToken ct)
    {
        var farm = await _farms.GetAsync(id, Actor, ct);
        if (farm is null) return NotFound();
        if (farm.Role is not ("Owner" or "Manager")) return NotFound();
        return View(new FarmEditViewModel(id, new FarmInput
        {
            Name = farm.Name,
            LocationText = farm.LocationText,
            TimeZoneId = farm.TimeZoneId,
            Latitude = farm.Latitude,
            Longitude = farm.Longitude,
            RowVersion = farm.RowVersion
        }));
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(Guid id, [Bind(Prefix = "Input")] FarmInput input,
        CancellationToken ct)
    {
        if (!ModelState.IsValid) return View(new FarmEditViewModel(id, input));
        try
        {
            await _farms.UpdateAsync(id, Actor, input, ct);
            return RedirectToAction(nameof(Details), new { id });
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            Response.StatusCode = e.Error == FarmError.Conflict ? 409 : 400;
            return View(new FarmEditViewModel(id, input));
        }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Archive(Guid id, string rowVersion, CancellationToken ct)
    {
        try
        {
            await _farms.ArchiveAsync(id, Actor, rowVersion, ct);
            return RedirectToAction(nameof(Index));
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
            return RedirectToAction(nameof(Details), new { id });
        }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> CreatePlot(Guid id, PlotInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Use a valid plot name and positive area.";
            return RedirectToAction(nameof(Details), new { id });
        }
        try { await _farms.CreatePlotAsync(id, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpGet]
    public async Task<IActionResult> EditPlot(Guid id, Guid plotId, CancellationToken ct)
    {
        var farm = await _farms.GetAsync(id, Actor, ct);
        if (farm?.Role is not ("Owner" or "Manager")) return NotFound();
        var plot = await _farms.GetPlotAsync(id, plotId, Actor, ct);
        if (plot is null) return NotFound();
        return View(new PlotEditViewModel(id, plotId, new PlotInput
        {
            Name = plot.Name, AreaSqMeters = plot.AreaSqMeters, RowVersion = plot.RowVersion
        }));
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> EditPlot(Guid id, Guid plotId, [Bind(Prefix = "Input")] PlotInput input,
        CancellationToken ct)
    {
        if (!ModelState.IsValid) return View(new PlotEditViewModel(id, plotId, input));
        try
        {
            await _farms.UpdatePlotAsync(id, plotId, Actor, input, ct);
            return RedirectToAction(nameof(Details), new { id });
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            Response.StatusCode = e.Error == FarmError.Conflict ? 409 : 400;
            return View(new PlotEditViewModel(id, plotId, input));
        }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> ArchivePlot(Guid id, Guid plotId, string rowVersion,
        CancellationToken ct)
    {
        try { await _farms.ArchivePlotAsync(id, plotId, Actor, rowVersion, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { id });
    }

    [AllowAnonymous, HttpPost, IgnoreAntiforgeryToken]
    public IActionResult Invite() => RetiredInvitation();

    [AllowAnonymous, AcceptVerbs("GET", "POST"), IgnoreAntiforgeryToken]
    public IActionResult AcceptInvitation() => RetiredInvitation();

    private ContentResult RetiredInvitation() => new()
    {
        StatusCode = StatusCodes.Status410Gone, ContentType = "text/plain",
        Content = "Invitation links have been retired. Register or sign in, enter the owner's farm code, and wait for approval."
    };

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> ChangeRole(Guid id, Guid memberId, UpdateMemberInput input, CancellationToken ct)
    {
        try { await _farms.ChangeMemberRoleAsync(id, memberId, Actor, input.Role, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> RemoveMember(Guid id, Guid memberId, CancellationToken ct)
    {
        try { await _farms.RemoveMemberAsync(id, memberId, Actor, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { id });
    }
}

