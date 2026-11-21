using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Localization;
using Microsoft.AspNetCore.Mvc;
namespace SmartFarm.Web.Controllers;
[AllowAnonymous]
public sealed class LanguageController : Controller
{
    [HttpPost, ValidateAntiForgeryToken]
    public IActionResult Select(string language, string? returnUrl)
    {
        if (language is not ("en" or "fil")) return BadRequest();
        Response.Cookies.Append(CookieRequestCultureProvider.DefaultCookieName,
            CookieRequestCultureProvider.MakeCookieValue(new RequestCulture(language)),
            new CookieOptions { Expires = DateTimeOffset.UtcNow.AddYears(1), HttpOnly = true,
                Secure = Request.IsHttps, SameSite = SameSiteMode.Lax, IsEssential = true });
        return LocalRedirect(Url.IsLocalUrl(returnUrl) ? returnUrl! : "/");
    }
}
