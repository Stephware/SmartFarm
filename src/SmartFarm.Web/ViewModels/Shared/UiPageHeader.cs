namespace SmartFarm.Web.ViewModels.Shared;

public sealed record UiPageHeader(string Title, string Description,
    string? ActionText = null, string? ActionUrl = null,
    string? Id = null, string? BackText = null, string? BackUrl = null);
