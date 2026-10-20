using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
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

    public FarmsController(
        UserManager<SmartFarmUser> users,
        IFarmManagementService farms)
    {
        _users = users;
        _farms = farms;
    }

    private Guid Actor =>
        Guid.TryParse(_users.GetUserId(User), out var id) ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(CancellationToken ct) =>
        View(await _farms.ListAsync(Actor, ct));

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
        return View(new FarmWorkspaceViewModel(farm,
            await _farms.ListPlotsAsync(id, Actor, ct), await _farms.MembersAsync(id, Actor, ct)));
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

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Invite(Guid id, InviteMemberInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Enter an existing user's email and choose a valid role.";
            return RedirectToAction(nameof(Details), new { id });
        }
        try
        {
            var invitation = await _farms.CreateInvitationAsync(id, Actor, input, ct);
            TempData["InvitationUrl"] = Url.Action(nameof(AcceptInvitation),
                "Farms", new { token = invitation.Token }, Request.Scheme);
            TempData["InvitationEmail"] = invitation.Email;
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpGet]
    public IActionResult AcceptInvitation(string token)
    {
        if (string.IsNullOrWhiteSpace(token)) return BadRequest();
        return View(new InvitationAcceptance { Token = token });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> AcceptInvitation(InvitationAcceptance input, CancellationToken ct)
    {
        if (!ModelState.IsValid) return View(input);
        try
        {
            var farmId = await _farms.AcceptInvitationAsync(Actor, input.Token, ct);
            return RedirectToAction(nameof(Details), new { id = farmId });
        }
        catch (FarmOperationException e)
        {
            ModelState.AddModelError("", e.Message);
            return View(input);
        }
    }

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

