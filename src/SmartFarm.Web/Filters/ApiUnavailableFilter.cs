using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.AspNetCore.Mvc.ModelBinding;
using Microsoft.AspNetCore.Mvc.ViewFeatures;

namespace SmartFarm.Web.Filters;

// Keep an API outage from turning every farm page into a developer exception screen.
public sealed class ApiUnavailableFilter(
    ILogger<ApiUnavailableFilter> logger, IWebHostEnvironment environment) : IExceptionFilter
{
    public void OnException(ExceptionContext context)
    {
        if (context.Exception is not HttpRequestException error) return;

        logger.LogError(error, "SmartFarm API request failed (HTTP {Status})",
            error.StatusCode is { } status ? (int)status : null);
        var viewData = new ViewDataDictionary(new EmptyModelMetadataProvider(), context.ModelState)
        {
            ["Title"] = "Farm data unavailable",
            ["ApiStatus"] = error.StatusCode is { } status ? ((int)status).ToString() : null,
            ["Development"] = environment.IsDevelopment()
        };
        context.Result = new ViewResult
        {
            ViewName = "~/Views/Shared/ApiUnavailable.cshtml",
            ViewData = viewData,
            StatusCode = StatusCodes.Status503ServiceUnavailable
        };
        context.ExceptionHandled = true;
    }
}
