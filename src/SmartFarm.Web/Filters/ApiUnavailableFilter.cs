using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.AspNetCore.Mvc.ModelBinding;
using Microsoft.AspNetCore.Mvc.ViewFeatures;
using Microsoft.AspNetCore.Mvc.Routing;
using System.Data.Common;
using Microsoft.EntityFrameworkCore;

namespace SmartFarm.Web.Filters;

// Shared services and HTTP integrations must offer safe, context-aware recovery.
public sealed class ApiUnavailableFilter(ILogger<ApiUnavailableFilter> logger) : IExceptionFilter
{
    public void OnException(ExceptionContext context)
    {
        if (context.Exception is not (HttpRequestException or DbException or DbUpdateException { InnerException: DbException })) return;

        logger.LogError(context.Exception, "SmartFarm data request failed for {Method} {Path}",
            context.HttpContext.Request.Method, context.HttpContext.Request.Path);
        var request = context.HttpContext.Request;
        var path = request.PathBase + request.Path + request.QueryString;
        var url = context.HttpContext.RequestServices.GetRequiredService<IUrlHelperFactory>().GetUrlHelper(context);
        var isRead = HttpMethods.IsGet(request.Method);
        var viewData = new ViewDataDictionary(new EmptyModelMetadataProvider(), context.ModelState)
        {
            ["Title"] = "Farm data unavailable",
            ["RetryUrl"] = isRead && url.IsLocalUrl(path) ? path : null,
            ["WriteUnconfirmed"] = !isRead
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
