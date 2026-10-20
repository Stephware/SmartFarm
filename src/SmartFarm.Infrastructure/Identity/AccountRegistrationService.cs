using Microsoft.AspNetCore.Identity;

namespace SmartFarm.Infrastructure.Identity;

// Single registration path shared by MVC and Android API: never grant farm permissions on signup.
public sealed class AccountRegistrationService(UserManager<SmartFarmUser> users)
{
    public async Task<IdentityResult> RegisterAsync(string name, string email, string password)
    {
        var cleanName = name.Trim();
        var cleanEmail = email.Trim();
        if (cleanName.Length is < 2 or > 120 || cleanEmail.Length is < 3 or > 256)
            return IdentityResult.Failed(new IdentityError { Description = "Invalid name or email length." });

        var user = new SmartFarmUser
        {
            FullName = cleanName,
            UserName = cleanEmail,
            Email = cleanEmail,
            LockoutEnabled = true
        };
        return await users.CreateAsync(user, password);
    }
}

